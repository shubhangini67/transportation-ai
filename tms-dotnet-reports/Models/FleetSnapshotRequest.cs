namespace TmsReports.Models;

public class FleetSnapshotRequest
{
    public int TotalVehicles { get; set; }
    public int AvailableVehicles { get; set; }
    public int BusyVehicles { get; set; }
    public int MaintenanceVehicles { get; set; }
    public int TotalDrivers { get; set; }
    public int ActiveDrivers { get; set; }
    public int InactiveDrivers { get; set; }
    public int TotalTrips { get; set; }
    public int PlannedTrips { get; set; }
    public int InProgressTrips { get; set; }
    public int CompletedTrips { get; set; }
    public decimal TotalExpenses { get; set; }
    public int TotalBookings { get; set; }
}
