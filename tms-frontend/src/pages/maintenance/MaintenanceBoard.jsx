import React, { useEffect, useState } from 'react';
import { FiTool } from 'react-icons/fi';
import { toast } from 'react-toastify';
import { maintenanceService } from '../../services/opsServices';
import StatCard from '../../components/stats/StatCard';
import '../../styles/dashboard.css';

function MaintenanceBoard() {
  const [alerts, setAlerts] = useState([]);
  const [filter, setFilter] = useState('');
  useEffect(() => {
    maintenanceService.getAlerts()
      .then((res) => setAlerts(res.data || []))
      .catch(() => toast.error('Failed to load maintenance alerts'));
  }, []);

  const visible = filter ? alerts.filter((a) => a.severity === filter) : alerts;

  return (
    <div>
      <div className="page-header"><h2 className="page-title"><FiTool /> Workshop</h2></div>
      <p className="insight-engine">Service due from odometer vs next-service-km, plus 180-day calendar policy.</p>
      <div className="stats-grid" style={{ marginBottom: 20 }}>
        <StatCard label="Overdue" value={alerts.filter((a) => a.severity === 'OVERDUE').length} color="#c62828" onClick={() => setFilter('OVERDUE')} />
        <StatCard label="Due soon" value={alerts.filter((a) => a.severity === 'DUE_SOON').length} color="#ef6c00" onClick={() => setFilter('DUE_SOON')} />
        <StatCard label="All trucks" value="Open" to="/vehicles" />
      </div>
      <div className="table-container">
        <table className="data-table">
          <thead><tr><th>Severity</th><th>Vehicle</th><th>Odometer</th><th>Due at</th><th>Remaining</th><th>Last service</th><th>Note</th></tr></thead>
          <tbody>
            {visible.map((a) => (
              <tr key={a.vehicleId} className="clickable-row">
                <td><span className={`risk-badge risk-${a.severity === 'OVERDUE' ? 'high' : 'medium'}`}>{a.severity.replace('_', ' ')}</span></td>
                <td>{a.vehicleNumber} <span className="muted">{a.vehicleType}</span></td>
                <td>{a.odometerKm?.toLocaleString()} km</td>
                <td>{a.nextServiceDueKm?.toLocaleString()} km</td>
                <td>{a.kmRemaining != null ? `${a.kmRemaining.toLocaleString()} km` : '—'}</td>
                <td>{a.lastServiceDate || '—'}</td>
                <td>{a.message}</td>
              </tr>
            ))}
            {visible.length === 0 && <tr><td colSpan="7" className="empty-row">No vehicles in the service window</td></tr>}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export default MaintenanceBoard;
