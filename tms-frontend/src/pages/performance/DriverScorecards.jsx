import React, { useEffect, useState } from 'react';
import { FiAward } from 'react-icons/fi';
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { toast } from 'react-toastify';
import { performanceService } from '../../services/opsServices';
import '../../styles/dashboard.css';

function DriverScorecards() {
  const [rows, setRows] = useState([]);
  useEffect(() => {
    performanceService.getDrivers()
      .then((res) => setRows(res.data || []))
      .catch(() => toast.error('Failed to load scorecards'));
  }, []);

  const chart = rows.map((r) => ({ name: r.driverName.split(' ')[0], score: r.score, onTime: r.onTimePercent }));

  return (
    <div>
      <div className="page-header"><h2 className="page-title"><FiAward /> Crew scores</h2></div>
      <p className="insight-engine">On-time % vs route ETA, delay count, and fuel booked on the driver’s trips.</p>
      <div className="chart-card" style={{ marginBottom: 20 }}>
        <h3>Score vs on-time</h3>
        <ResponsiveContainer width="100%" height={260}>
          <BarChart data={chart}><CartesianGrid strokeDasharray="3 3" /><XAxis dataKey="name" /><YAxis /><Tooltip /><Bar dataKey="score" fill="#c45c26" /><Bar dataKey="onTime" fill="#3d7a4a" /></BarChart>
        </ResponsiveContainer>
      </div>
      <div className="table-container">
        <table className="data-table">
          <thead><tr><th>Driver</th><th>Band</th><th>Score</th><th>On-time</th><th>Completed</th><th>Delayed</th><th>Fuel spend</th></tr></thead>
          <tbody>
            {rows.map((r) => (
              <tr key={r.driverId}>
                <td>{r.driverName}</td>
                <td><span className={`risk-badge ${r.band === 'A' ? 'risk-low' : r.band === 'D' ? 'risk-high' : 'risk-medium'}`}>{r.band}</span></td>
                <td>{r.score}</td>
                <td>{r.onTimePercent}%</td>
                <td>{r.completedTrips}</td>
                <td>{r.delayedTrips}</td>
                <td>₹{Number(r.fuelSpend || 0).toLocaleString()}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export default DriverScorecards;
