import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { FiAlertTriangle, FiMapPin } from 'react-icons/fi';
import { toast } from 'react-toastify';
import operationsService from '../../services/operationsService';
import StatCard from '../../components/stats/StatCard';
import StatusBadge from '../../components/common/StatusBadge';
import '../../styles/dashboard.css';

function OperationsBoard() {
  const navigate = useNavigate();
  const [alerts, setAlerts] = useState([]);
  const [filter, setFilter] = useState('ALL');
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    try {
      const res = await operationsService.getAlerts();
      setAlerts(res.data || []);
    } catch {
      toast.error('Failed to load operations alerts');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
    const id = setInterval(load, 30000);
    return () => clearInterval(id);
  }, [load]);

  const overdue = alerts.filter((a) => a.code === 'OVERDUE_START').length;
  const delayed = alerts.filter((a) => a.code === 'DELAYED').length;
  const high = alerts.filter((a) => a.severity === 'HIGH').length;

  const visible = alerts.filter((a) => {
    if (filter === 'OVERDUE_START') return a.code === 'OVERDUE_START';
    if (filter === 'DELAYED') return a.code === 'DELAYED';
    if (filter === 'HIGH') return a.severity === 'HIGH';
    return true;
  });

  return (
    <div>
      <div className="page-header">
        <h2 className="page-title"><FiAlertTriangle /> Exceptions</h2>
        <span className="auto-refresh-label">Auto-refreshes every 30s</span>
      </div>

      <div className="stats-grid" style={{ marginBottom: 24 }}>
        <StatCard label="Open exceptions" value={alerts.length} onClick={() => setFilter('ALL')} />
        <StatCard label="Overdue to start" value={overdue} color="#e65100" onClick={() => setFilter('OVERDUE_START')} />
        <StatCard label="Running late" value={delayed} color="#c62828" onClick={() => setFilter('DELAYED')} />
        <StatCard label="High severity" value={high} color="#b71c1c" onClick={() => setFilter('HIGH')} />
      </div>

      {visible.length === 0 ? (
        <div className="chart-card">
          <p className="empty-hint">No delayed or overdue trips. Fleet is on schedule.</p>
        </div>
      ) : (
        <div className="table-container">
          <table className="data-table">
            <thead>
              <tr>
                <th>Severity</th>
                <th>Issue</th>
                <th>Vehicle</th>
                <th>Driver</th>
                <th>Route</th>
                <th>Status</th>
                <th>Overdue</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {visible.map((alert) => (
                <tr
                  key={`${alert.tripId}-${alert.code}`}
                  className="clickable-row"
                  onClick={() => navigate(`/trips/${alert.tripId}/tracking`)}
                >
                  <td>
                    <span className={`risk-badge risk-${(alert.severity || 'low').toLowerCase()}`}>
                      {alert.severity}
                    </span>
                  </td>
                  <td>
                    <strong>{alert.code.replace(/_/g, ' ')}</strong>
                    <div className="muted">{alert.message}</div>
                  </td>
                  <td>{alert.vehicleNumber}</td>
                  <td>{alert.driverName}</td>
                  <td>{alert.routeLabel}</td>
                  <td><StatusBadge status={alert.tripStatus} /></td>
                  <td>{alert.minutesOverdue} min</td>
                  <td>
                    <button className="btn-icon btn-edit" title="Track trip" onClick={() => navigate(`/trips/${alert.tripId}/tracking`)}>
                      <FiMapPin />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

export default OperationsBoard;
