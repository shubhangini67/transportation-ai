import React, { useState, useEffect } from 'react';
import { FiDownload, FiBarChart2 } from 'react-icons/fi';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, PieChart, Pie, Cell, Legend } from 'recharts';
import { toast } from 'react-toastify';
import reportService from '../../services/reportService';
import StatCard from '../../components/stats/StatCard';
import '../../styles/dashboard.css';

const COLORS = ['#c45c26', '#243830', '#8a9a7b', '#d4a574', '#5c6d62'];

function ReportsPage() {
  const [dateRange, setDateRange] = useState({
    from: new Date(Date.now() - 30 * 86400000).toISOString().slice(0, 10),
    to: new Date().toISOString().slice(0, 10),
  });
  const [tripReport, setTripReport] = useState(null);
  const [vehicleReport, setVehicleReport] = useState(null);
  const [driverReport, setDriverReport] = useState(null);
  const [insights, setInsights] = useState(null);

  useEffect(() => {
    const load = async () => {
      const [trips, vehicles, drivers, fleetInsights] = await Promise.allSettled([
        reportService.getTripReport(dateRange.from, dateRange.to),
        reportService.getVehicleReport(),
        reportService.getDriverReport(),
        reportService.getFleetInsights(),
      ]);
      if (trips.status === 'fulfilled') setTripReport(trips.value.data);
      else toast.error('Failed to load trip report');
      if (vehicles.status === 'fulfilled') setVehicleReport(vehicles.value.data);
      if (drivers.status === 'fulfilled') setDriverReport(drivers.value.data);
      if (fleetInsights.status === 'fulfilled') setInsights(fleetInsights.value.data);
      else setInsights(null);
    };
    load();
  }, [dateRange]);

  const downloadCsv = async () => {
    try {
      const res = await reportService.getTripReportCsv(dateRange.from, dateRange.to);
      const url = window.URL.createObjectURL(new Blob([res.data]));
      const link = document.createElement('a');
      link.href = url;
      link.download = 'trip-report.csv';
      link.click();
    } catch { toast.error('Export failed'); }
  };

  const vehicleChartData = vehicleReport ? [
    { name: 'Available', value: vehicleReport.availableVehicles || 0 },
    { name: 'Busy', value: vehicleReport.busyVehicles || 0 },
    { name: 'Maintenance', value: vehicleReport.maintenanceVehicles || 0 },
  ] : [];

  const tripStatusData = tripReport?.byStatus ? Object.entries(tripReport.byStatus).map(([key, val]) => ({ name: key.replace(/_/g, ' '), value: val })) : [];

  return (
    <div>
      <div className="page-header">
        <h2 className="page-title"><FiBarChart2 /> Insights</h2>
        <button className="btn btn-primary" onClick={downloadCsv}><FiDownload /> Export CSV</button>
      </div>
      <div style={{ display: 'flex', gap: 12, marginBottom: 20 }}>
        <input className="form-input" type="date" value={dateRange.from} onChange={e => setDateRange({ ...dateRange, from: e.target.value })} />
        <input className="form-input" type="date" value={dateRange.to} onChange={e => setDateRange({ ...dateRange, to: e.target.value })} />
      </div>

      <div className="stats-grid" style={{ marginBottom: 24 }}>
        <StatCard label="Total Trips" value={tripReport?.totalTrips || 0} color="#673ab7" to="/trips" />
        <StatCard label="Total Bookings" value={tripReport?.totalBookings || 0} color="#009688" to="/bookings" />
        <StatCard label="Total Expenses" value={`₹${Number(tripReport?.totalExpenses || 0).toLocaleString()}`} color="#ff5722" to="/expenses" />
        <StatCard label="Active Drivers" value={`${driverReport?.activeDrivers || 0}/${driverReport?.totalDrivers || 0}`} color="#4caf50" to="/drivers?status=ACTIVE" />
      </div>

      {insights && (
        <div className="chart-card insight-panel" style={{ marginBottom: 24 }}>
          <div className="insight-header">
            <h3>Fleet insights</h3>
            <span className={`risk-badge risk-${(insights.riskLevel || 'LOW').toLowerCase()}`}>
              {insights.riskLevel || 'LOW'} risk
            </span>
          </div>
          <p className="insight-engine">Scored by {insights.engine || 'reporting service'}</p>
            <div className="stats-grid" style={{ marginBottom: 16 }}>
            <StatCard label="Fleet health score" value={insights.fleetHealthScore ?? '—'} to="/fleet-map" />
            <StatCard label="Vehicle utilization" value={`${insights.vehicleUtilizationPercent ?? 0}%`} color="#e65100" to="/trips?status=IN_PROGRESS" />
            <StatCard label="In maintenance" value={`${insights.maintenanceLoadPercent ?? 0}%`} color="#c62828" to="/maintenance" />
            <StatCard label="Expense per trip" value={`₹${Number(insights.expensePerTrip || 0).toLocaleString()}`} color="#ff5722" to="/expenses" />
          </div>
          {insights.alerts?.length > 0 && (
            <div className="insight-list">
              <strong>Alerts</strong>
              <ul>{insights.alerts.map((a) => <li key={a}>{a}</li>)}</ul>
            </div>
          )}
          {insights.recommendations?.length > 0 && (
            <div className="insight-list">
              <strong>Recommendations</strong>
              <ul>{insights.recommendations.map((a) => <li key={a}>{a}</li>)}</ul>
            </div>
          )}
        </div>
      )}

      <div className="charts-grid">
        <div className="chart-card">
          <h3>Trip Status Breakdown</h3>
          <ResponsiveContainer width="100%" height={250}>
            <BarChart data={tripStatusData}><CartesianGrid strokeDasharray="3 3" /><XAxis dataKey="name" /><YAxis /><Tooltip /><Bar dataKey="value" fill="#c45c26" /></BarChart>
          </ResponsiveContainer>
        </div>
        <div className="chart-card">
          <h3>Vehicle Utilization</h3>
          <ResponsiveContainer width="100%" height={250}>
            <PieChart>
              <Pie data={vehicleChartData} cx="50%" cy="50%" outerRadius={80} dataKey="value" label>
                {vehicleChartData.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
              </Pie>
              <Legend /><Tooltip />
            </PieChart>
          </ResponsiveContainer>
        </div>
      </div>
    </div>
  );
}

export default ReportsPage;

