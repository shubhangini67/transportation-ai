import React, { useState, useEffect, useCallback } from 'react';
import { useIntl } from 'react-intl';
import { toast } from 'react-toastify';
import { FiDroplet, FiTruck, FiDollarSign, FiTrendingUp } from 'react-icons/fi';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid, Legend } from 'recharts';
import fuelAnalyticsService from '../../services/fuelAnalyticsService';
import '../../styles/dashboard.css';
import '../../styles/components.css';

function FuelAnalytics() {
  const intl = useIntl();
  const [analytics, setAnalytics] = useState(null);
  const [loading, setLoading] = useState(true);
  const [from, setFrom] = useState(() => {
    const d = new Date(); d.setMonth(d.getMonth() - 6);
    return d.toISOString().split('T')[0];
  });
  const [to, setTo] = useState(() => new Date().toISOString().split('T')[0]);

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const res = await fuelAnalyticsService.getAnalytics({ from, to });
      setAnalytics(res.data);
    } catch {
      toast.error('Failed to load fuel analytics');
    } finally {
      setLoading(false);
    }
  }, [from, to]);

  useEffect(() => { fetchData(); }, [fetchData]);

  if (loading) return <div className="page-loading">{intl.formatMessage({ id: 'loading' })}</div>;
  if (!analytics) return <div className="page-loading">No data available</div>;

  const trendData = (analytics.monthlyTrend || []).map(p => ({
    month: p.period,
    spend: parseFloat(p.totalSpend) || 0,
    costPerKm: parseFloat(p.costPerKm) || 0,
    distance: p.totalDistanceKm || 0,
  }));

  const vehicleData = (analytics.vehicleBreakdowns || []).slice(0, 10).map(v => ({
    vehicle: v.vehicleNumber,
    fuelCost: parseFloat(v.totalFuelCost) || 0,
    costPerKm: parseFloat(v.costPerKm) || 0,
    distance: v.totalDistanceKm || 0,
  }));

  return (
    <div className="page-container">
      <div className="page-header">
        <h1><FiDroplet style={{ marginRight: 8 }} />{intl.formatMessage({ id: 'fuelAnalytics.title' })}</h1>
        <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
          <input type="date" className="form-input" value={from} onChange={e => setFrom(e.target.value)} style={{ width: 160 }} />
          <span>to</span>
          <input type="date" className="form-input" value={to} onChange={e => setTo(e.target.value)} style={{ width: 160 }} />
        </div>
      </div>

      <div className="stats-grid" style={{ marginBottom: 24 }}>
        <div className="stat-card">
          <div className="stat-icon" style={{ background: 'var(--primary-light)', color: 'var(--primary)' }}><FiDollarSign /></div>
          <div className="stat-info">
            <span className="stat-value">₹{parseFloat(analytics.totalFuelSpend || 0).toLocaleString()}</span>
            <span className="stat-label">{intl.formatMessage({ id: 'fuelAnalytics.totalSpend' })}</span>
          </div>
        </div>
        <div className="stat-card">
          <div className="stat-icon" style={{ background: '#e8f5e9', color: '#2e7d32' }}><FiTrendingUp /></div>
          <div className="stat-info">
            <span className="stat-value">₹{parseFloat(analytics.averageCostPerKm || 0).toFixed(2)}</span>
            <span className="stat-label">{intl.formatMessage({ id: 'fuelAnalytics.avgCostPerKm' })}</span>
          </div>
        </div>
        <div className="stat-card">
          <div className="stat-icon" style={{ background: '#e3f2fd', color: '#1565c0' }}><FiTruck /></div>
          <div className="stat-info">
            <span className="stat-value">{(analytics.totalDistanceKm || 0).toLocaleString()} km</span>
            <span className="stat-label">{intl.formatMessage({ id: 'fuelAnalytics.totalDistance' })}</span>
          </div>
        </div>
        <div className="stat-card">
          <div className="stat-icon"><FiDroplet /></div>
          <div className="stat-info">
            <span className="stat-value">{analytics.fuelTransactionCount}</span>
            <span className="stat-label">{intl.formatMessage({ id: 'fuelAnalytics.transactions' })}</span>
          </div>
        </div>
      </div>

      <div className="dashboard-charts" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 20, marginBottom: 24 }}>
        <div className="chart-card">
          <h3>{intl.formatMessage({ id: 'fuelAnalytics.monthlyTrend' })}</h3>
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={trendData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="month" />
              <YAxis />
              <Tooltip />
              <Legend />
              <Bar dataKey="spend" fill="var(--primary)" name="Fuel Spend (₹)" />
              <Bar dataKey="distance" fill="#4caf50" name="Distance (km)" />
            </BarChart>
          </ResponsiveContainer>
        </div>
        <div className="chart-card">
          <h3>{intl.formatMessage({ id: 'fuelAnalytics.costPerKmByVehicle' })}</h3>
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={vehicleData} layout="vertical">
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis type="number" />
              <YAxis dataKey="vehicle" type="category" width={120} />
              <Tooltip />
              <Legend />
              <Bar dataKey="costPerKm" fill="#ff9800" name="Cost/Km (₹)" />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>

      <div className="chart-card">
        <h3>{intl.formatMessage({ id: 'fuelAnalytics.vehicleBreakdown' })}</h3>
        <div style={{ overflowX: 'auto' }}>
          <table className="data-table">
            <thead>
              <tr>
                <th>Vehicle</th>
                <th>Fuel Cost</th>
                <th>Distance (km)</th>
                <th>Cost/Km</th>
                <th>Trips</th>
                <th>Fuel Entries</th>
              </tr>
            </thead>
            <tbody>
              {(analytics.vehicleBreakdowns || []).map((v, i) => (
                <tr key={v.vehicleId || i}>
                  <td><strong>{v.vehicleNumber}</strong></td>
                  <td>₹{parseFloat(v.totalFuelCost || 0).toLocaleString()}</td>
                  <td>{(v.totalDistanceKm || 0).toLocaleString()}</td>
                  <td>₹{parseFloat(v.costPerKm || 0).toFixed(2)}</td>
                  <td>{v.tripCount}</td>
                  <td>{v.fuelExpenseCount}</td>
                </tr>
              ))}
              {(!analytics.vehicleBreakdowns || analytics.vehicleBreakdowns.length === 0) && (
                <tr><td colSpan={6} style={{ textAlign: 'center' }}>No fuel data found for this period</td></tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

export default FuelAnalytics;

