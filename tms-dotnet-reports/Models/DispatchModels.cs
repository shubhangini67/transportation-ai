namespace TmsReports.Models;

public class DispatchSnapshotRequest
{
    public string Origin { get; set; } = "";
    public string Destination { get; set; } = "";
    public double DistanceKm { get; set; }
    public int EstimatedMinutes { get; set; }
    public int RequiredCapacity { get; set; }
    public List<DispatchVehicle> Vehicles { get; set; } = new();
    public List<DispatchDriver> Drivers { get; set; } = new();
}

public class DispatchVehicle
{
    public string Id { get; set; } = "";
    public string Number { get; set; } = "";
    public string Type { get; set; } = "";
    public int Capacity { get; set; }
    public string Status { get; set; } = "";
    public string CurrentLocation { get; set; } = "";
    public bool BusyOnTrip { get; set; }
}

public class DispatchDriver
{
    public string Id { get; set; } = "";
    public string Name { get; set; } = "";
    public string Status { get; set; } = "";
    public bool BusyOnTrip { get; set; }
}

public class DispatchPlanResponse
{
    public string Engine { get; set; } = "ASP.NET Core 8";
    public string RouteLabel { get; set; } = "";
    public List<DispatchRecommendation> Recommendations { get; set; } = new();
}

public class DispatchRecommendation
{
    public int Rank { get; set; }
    public int Score { get; set; }
    public string VehicleId { get; set; } = "";
    public string VehicleNumber { get; set; } = "";
    public string VehicleType { get; set; } = "";
    public string DriverId { get; set; } = "";
    public string DriverName { get; set; } = "";
    public string Reason { get; set; } = "";
}
