namespace TmsReports.Models;

public class FleetInsightsResponse
{
    public string Engine { get; set; } = "ASP.NET Core 8";
    public double VehicleUtilizationPercent { get; set; }
    public double MaintenanceLoadPercent { get; set; }
    public double DriverAvailabilityPercent { get; set; }
    public double ExpensePerTrip { get; set; }
    public int FleetHealthScore { get; set; }
    public string RiskLevel { get; set; } = "LOW";
    public List<string> Alerts { get; set; } = new();
    public List<string> Recommendations { get; set; } = new();
}
