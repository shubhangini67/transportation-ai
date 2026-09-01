import React, { useEffect, useState, useCallback } from 'react';
import {
  LineChart, Line, BarChart, Bar, PieChart, Pie, Cell,
  XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend
} from 'recharts';
import { Link, useNavigate } from 'react-router-dom';
import dashboardService from '../services/dashboardService';
import operationsService from '../services/operationsService';
import StatCard from '../components/stats/StatCard';
import StatusBadge from '../components/common/StatusBadge';
import EntityAvatar from '../components/common/EntityAvatar';
import { useAuth } from '../context/AuthContext';
import { FiNavigation, FiTruck, FiUsers, FiPackage, FiBookmark, FiRadio, FiCheckCircle, FiActivity } from 'react-icons/fi';
import '../styles/dashboard.css';

const REFRESH_INTERVAL = 30000;
const PIE_COLORS = ['#c45c26', '#2a3d34', '#8a9a7b', '#d4a574', '#5c6d62'];

function Dashboard() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const canOps = ['ADMIN', 'DISPATCHER'].includes(user?.role);
  const canDrivers = canOps;
  const canVehicles = ['ADMIN', 'DISPATCHER', 'DRIVER'].includes(user?.role);
  const canBookings = ['ADMIN', 'DISPATCHER', 'CLIENT'].includes(user?.role);
  const [stats, setStats] = useState(null);
  const [trends, setTrends] = useState(null);
  const [opsCount, setOpsCount] = useState(0);
  const [loading, setLoading] = useState(true);

  const loadStats = useCallback(async () => {
    try {
      const [statsRes, trendsRes] = await Promise.all([
        dashboardService.getMetrics(),
        dashboardService.getTrends(),
      ]);
      setStats(statsRes.data);
      setTrends(trendsRes.data);
      if (canOps) {
        try {
          const opsRes = await operationsService.getAlerts();
          setOpsCount((opsRes.data || []).length);
        } catch {
          setOpsCount(0);
        }
      }
    } catch (err) {
      console.error('Failed to load dashboard metrics', err);
    } finally {
      setLoading(false);
    }
  }, [canOps]);

  useEffect(() => {
    loadStats();
    const interval = setInterval(loadStats, REFRESH_INTERVAL);
    return () => clearInterval(interval);
  }, [loadStats]);

  if (loading) return <div className="page-loader">Loading...</div>;
  if (!stats) return <div className="page-loader">Failed to load data</div>;

  const formatDate = (dateStr) => {
    if (!dateStr) return '—';
    return new Date(dateStr).toLocaleString();
  };

  const tripsByStatusData = trends?.tripsByStatus
    ? Object.entries(trends.tripsByStatus).map(([name, value]) => ({ name: name.replace(/_/g, ' '), value }))
    : [];

  const vehiclesByStatusData = trends?.vehiclesByStatus
    ? Object.entries(trends.vehiclesByStatus).map(([name, value]) => ({ name, value }))
    : [];

  return (
    <div className="dashboard">
      <div className="page-header">
        <div>
          <p className="page-kicker">Operations</p>
          <h2 className="page-title">Control room</h2>
          <p className="page-subtitle">Fleet, lanes, and exceptions at a glance.</p>
        </div>
        <span className="auto-refresh-label">Live · refreshes every 30s</span>
      </div>

      {canOps && (
        <div className="ops-banner ai-banner">
          <span>Talk to <strong>Copilot</strong> — it can write crew, fleet, and lanes for you.</span>
          <Link to="/ai">Open Copilot</Link>
        </div>
      )}

      {canOps && opsCount > 0 && (
        <div className="ops-banner">
          <span>{opsCount} trip{opsCount === 1 ? '' : 's'} overdue or delayed.</span>
          <Link to="/operations">Open exceptions</Link>
        </div>
      )}

      <div className="stats-grid">
        <StatCard icon={<FiNavigation />} label="Total runs" value={stats.totalTrips} to="/trips" />
        <StatCard icon={<FiActivity />} label="Live runs" value={stats.activeTrips} to="/trips?status=IN_PROGRESS" />
        <StatCard icon={<FiCheckCircle />} label="Completed" value={stats.completedTrips} to="/trips?status=COMPLETED" />
        {canVehicles && <StatCard icon={<FiTruck />} label="Free trucks" value={stats.availableVehicles} to="/vehicles?status=AVAILABLE" />}
        {canVehicles && <StatCard icon={<FiTruck />} label="Fleet size" value={stats.totalVehicles} to="/vehicles" />}
        {canDrivers && <StatCard icon={<FiUsers />} label="Crew on duty" value={stats.activeDrivers} to="/drivers?status=ACTIVE" />}
        <StatCard icon={<FiPackage />} label="Waybills" value={stats.totalLrs} to="/lrs" />
        {canBookings && <StatCard icon={<FiBookmark />} label="Bookings" value={stats.totalBookings} to="/bookings" />}
        {canVehicles && <StatCard icon={<FiRadio />} label="Live map" value="Open" to="/fleet-map" />}
      </div>

      {trends && (
        <div className="charts-grid">
          <div className="chart-card">
            <h3>Runs (7 days)</h3>
            <ResponsiveContainer width="100%" height={250}>
              <LineChart data={trends.tripTrend}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="label" />
                <YAxis allowDecimals={false} />
                <Tooltip />
                <Line type="monotone" dataKey="count" stroke="var(--accent)" strokeWidth={2} name="Runs" />
              </LineChart>
            </ResponsiveContainer>
          </div>
          <div className="chart-card">
            <h3>Bookings (7 days)</h3>
            <ResponsiveContainer width="100%" height={250}>
              <BarChart data={trends.bookingTrend}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="label" />
                <YAxis allowDecimals={false} />
                <Tooltip />
                <Bar dataKey="count" fill="#c45c26" name="Bookings" />
              </BarChart>
            </ResponsiveContainer>
          </div>
          <div className="chart-card">
            <h3>Runs by status</h3>
            <ResponsiveContainer width="100%" height={250}>
              <PieChart>
                <Pie data={tripsByStatusData} dataKey="value" nameKey="name" cx="50%" cy="50%" outerRadius={80} label
                  onClick={(_, i) => {
                    const key = Object.keys(trends?.tripsByStatus || {})[i];
                    if (key) navigate(`/trips?status=${key}`);
                  }}
                  style={{ cursor: 'pointer' }}
                >
                  {tripsByStatusData.map((entry, i) => (
                    <Cell key={entry.name} fill={PIE_COLORS[i % PIE_COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip />
                <Legend />
              </PieChart>
            </ResponsiveContainer>
          </div>
          <div className="chart-card">
            <h3>Fleet by status</h3>
            <ResponsiveContainer width="100%" height={250}>
              <PieChart>
                <Pie data={vehiclesByStatusData} dataKey="value" nameKey="name" cx="50%" cy="50%" outerRadius={80} label
                  onClick={(_, i) => {
                    const key = Object.keys(trends?.vehiclesByStatus || {})[i];
                    if (key && canVehicles) navigate(`/vehicles?status=${key}`);
                  }}
                  style={{ cursor: 'pointer' }}
                >
                  {vehiclesByStatusData.map((entry, i) => (
                    <Cell key={entry.name} fill={PIE_COLORS[i % PIE_COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip />
                <Legend />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </div>
      )}

      <div className="recent-section">
        <h3>Recent runs <Link to="/trips" className="auth-link" style={{ fontSize: '0.85rem', fontWeight: 500, marginLeft: 8 }}>View all</Link></h3>
        <div className="table-container">
          <table className="data-table">
            <thead>
              <tr>
                <th>Truck</th>
                <th>Crew</th>
                <th>Lane</th>
                <th>Start</th>
                <th>Waybills</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {stats.recentTrips?.length > 0 ? stats.recentTrips.map((trip) => (
                <tr
                  key={trip.id}
                  className="clickable-row"
                  onClick={() => navigate(
                    trip.status === 'COMPLETED' ? '/trips?status=COMPLETED' : `/trips/${trip.id}/tracking`
                  )}
                >
                  <td>
                    <span className="entity-cell">
                      <EntityAvatar kind="vehicle" type={trip.vehicleType} size={28} />
                      {trip.vehicleNumber}
                    </span>
                  </td>
                  <td>
                    <span className="entity-cell">
                      <EntityAvatar name={trip.driverName} size={28} />
                      {trip.driverName}
                    </span>
                  </td>
                  <td>{trip.routeOrigin ? `🗺️ ${trip.routeOrigin} → ${trip.routeDestination}` : '—'}</td>
                  <td>{formatDate(trip.startTime)}</td>
                  <td>{trip.lorryReceipts?.length || 0}</td>
                  <td><StatusBadge status={trip.status} /></td>
                </tr>
              )) : (
                <tr><td colSpan="6" className="empty-row">No recent trips</td></tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

export default Dashboard;
