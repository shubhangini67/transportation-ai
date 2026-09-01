using TmsReports.Models;

namespace TmsReports.Services;

/// <summary>
/// Rule-based fleet insights. Spring Boot sends a snapshot; this service scores
/// utilization, maintenance load, and driver coverage.
/// </summary>
public class FleetInsightEngine
{
    public FleetInsightsResponse Analyze(FleetSnapshotRequest snap)
    {
        var vehicles = Math.Max(snap.TotalVehicles, 0);
        var drivers = Math.Max(snap.TotalDrivers, 0);
        var trips = Math.Max(snap.TotalTrips, 0);

        double util = Pct(snap.BusyVehicles, vehicles);
        double maint = Pct(snap.MaintenanceVehicles, vehicles);
        double driverAvail = Pct(snap.ActiveDrivers, drivers);
        double expensePerTrip = trips == 0 ? 0 : (double)snap.TotalExpenses / trips;

        int health = 100;
        health -= (int)Math.Min(40, maint * 1.5);
        health -= util > 85 ? 15 : 0;
        health -= driverAvail < 50 ? 20 : 0;
        health -= snap.InProgressTrips > snap.ActiveDrivers && snap.ActiveDrivers > 0 ? 10 : 0;
        health = Math.Clamp(health, 0, 100);

        string risk = health >= 75 ? "LOW" : health >= 50 ? "MEDIUM" : "HIGH";

        var alerts = new List<string>();
        var recs = new List<string>();

        if (maint >= 20)
        {
            alerts.Add($"{maint:0.#}% of the fleet is in maintenance — capacity is reduced.");
            recs.Add("Prioritize returning maintenance vehicles to AVAILABLE before planning new long-haul trips.");
        }
        if (util >= 80)
        {
            alerts.Add($"Vehicle utilization is {util:0.#}% — little spare capacity.");
            recs.Add("Keep at least one AVAILABLE truck as a buffer for delayed or emergency loads.");
        }
        if (driverAvail < 60 && drivers > 0)
        {
            alerts.Add($"Only {driverAvail:0.#}% of drivers are ACTIVE.");
            recs.Add("Activate standby drivers or reduce planned trips until coverage improves.");
        }
        if (snap.InProgressTrips > 0 && snap.BusyVehicles == 0)
        {
            alerts.Add("Trips are in progress but no vehicle is marked BUSY — status may be out of sync.");
            recs.Add("Check trip lifecycle: starting a trip should mark the vehicle BUSY.");
        }
        if (expensePerTrip > 8000)
        {
            recs.Add($"Average expense per trip is ₹{expensePerTrip:0} — review fuel and toll spend in Fuel Analytics.");
        }
        if (alerts.Count == 0)
        {
            recs.Add("Fleet looks healthy. Continue monitoring geofence alerts and fuel cost per km.");
        }

        return new FleetInsightsResponse
        {
            Engine = "ASP.NET Core 8",
            VehicleUtilizationPercent = Math.Round(util, 1),
            MaintenanceLoadPercent = Math.Round(maint, 1),
            DriverAvailabilityPercent = Math.Round(driverAvail, 1),
            ExpensePerTrip = Math.Round(expensePerTrip, 2),
            FleetHealthScore = health,
            RiskLevel = risk,
            Alerts = alerts,
            Recommendations = recs
        };
    }

    private static double Pct(int part, int whole) =>
        whole <= 0 ? 0 : 100.0 * part / whole;
}
