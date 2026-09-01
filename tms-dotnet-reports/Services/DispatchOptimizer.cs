using TmsReports.Models;

namespace TmsReports.Services;

/// <summary>
/// Greedy vehicle–driver pairing for a lane. Spring Boot sends live fleet
/// state; this service ranks assignments. Not a rewrite of TMS.
/// </summary>
public class DispatchOptimizer
{
    public DispatchPlanResponse Recommend(DispatchSnapshotRequest snap)
    {
        var required = Math.Max(snap.RequiredCapacity, 1);
        var origin = snap.Origin ?? "";
        var vehicles = (snap.Vehicles ?? new List<DispatchVehicle>())
            .Where(v => !string.Equals(v.Status, "MAINTENANCE", StringComparison.OrdinalIgnoreCase))
            .ToList();
        var drivers = (snap.Drivers ?? new List<DispatchDriver>())
            .Where(d => string.Equals(d.Status, "ACTIVE", StringComparison.OrdinalIgnoreCase) && !d.BusyOnTrip)
            .ToList();

        var pairs = new List<(int Score, DispatchVehicle V, DispatchDriver D, string Reason)>();
        foreach (var vehicle in vehicles)
        {
            foreach (var driver in drivers)
            {
                var (score, reason) = ScorePair(vehicle, driver, snap.DistanceKm, required, origin);
                if (score > 0)
                {
                    pairs.Add((score, vehicle, driver, reason));
                }
            }
        }

        var usedVehicles = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
        var usedDrivers = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
        var recs = new List<DispatchRecommendation>();

        foreach (var pair in pairs.OrderByDescending(p => p.Score))
        {
            if (usedVehicles.Contains(pair.V.Id) || usedDrivers.Contains(pair.D.Id))
            {
                continue;
            }

            usedVehicles.Add(pair.V.Id);
            usedDrivers.Add(pair.D.Id);
            recs.Add(new DispatchRecommendation
            {
                Rank = recs.Count + 1,
                Score = pair.Score,
                VehicleId = pair.V.Id,
                VehicleNumber = pair.V.Number,
                VehicleType = pair.V.Type,
                DriverId = pair.D.Id,
                DriverName = pair.D.Name,
                Reason = pair.Reason
            });
            if (recs.Count >= 5)
            {
                break;
            }
        }

        return new DispatchPlanResponse
        {
            Engine = "ASP.NET Core 8",
            RouteLabel = $"{snap.Origin} → {snap.Destination}",
            Recommendations = recs
        };
    }

    private static (int Score, string Reason) ScorePair(
        DispatchVehicle vehicle,
        DispatchDriver driver,
        double distanceKm,
        int requiredCapacity,
        string origin)
    {
        int score = 40;
        var why = new List<string>();

        if (vehicle.BusyOnTrip || string.Equals(vehicle.Status, "BUSY", StringComparison.OrdinalIgnoreCase))
        {
            score -= 35;
            why.Add("vehicle is already BUSY");
        }
        else
        {
            score += 25;
            why.Add("vehicle AVAILABLE");
        }

        if (vehicle.Capacity < requiredCapacity)
        {
            return (0, "capacity too low");
        }

        int spare = vehicle.Capacity - requiredCapacity;
        score += Math.Min(15, spare);
        if (spare <= 4)
        {
            why.Add("capacity is a tight fit");
        }
        else
        {
            why.Add($"capacity {vehicle.Capacity} vs need {requiredCapacity}");
        }

        var type = (vehicle.Type ?? "").ToUpperInvariant();
        if (distanceKm >= 250 && type is "TRUCK" or "BUS")
        {
            score += 20;
            why.Add("long-haul type match");
        }
        else if (distanceKm <= 80 && type is "VAN" or "MINI_BUS")
        {
            score += 15;
            why.Add("short-haul type match");
        }

        if (!string.IsNullOrWhiteSpace(origin)
            && (vehicle.CurrentLocation ?? "").Contains(origin, StringComparison.OrdinalIgnoreCase))
        {
            score += 18;
            why.Add($"already near {origin}");
        }

        score += 10; // active idle driver already filtered
        why.Add($"{driver.Name} is idle");

        return (Math.Clamp(score, 1, 100), string.Join("; ", why));
    }
}
