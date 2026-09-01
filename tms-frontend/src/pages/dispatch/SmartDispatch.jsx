import React, { useCallback, useEffect, useState } from 'react';
import { FiCpu, FiNavigation } from 'react-icons/fi';
import { toast } from 'react-toastify';
import dispatchService from '../../services/dispatchService';
import routeService from '../../services/routeService';
import tripService from '../../services/tripService';
import '../../styles/dashboard.css';
import '../../styles/forms.css';

function localDateTimePlusHours(hours) {
  const d = new Date(Date.now() + hours * 3600000);
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

function SmartDispatch() {
  const [routes, setRoutes] = useState([]);
  const [routeId, setRouteId] = useState('');
  const [requiredCapacity, setRequiredCapacity] = useState(10);
  const [plan, setPlan] = useState(null);
  const [loading, setLoading] = useState(false);
  const [assigning, setAssigning] = useState(null);

  useEffect(() => {
    const load = async () => {
      try {
        const res = await routeService.getActive();
        const list = res.data || [];
        setRoutes(list);
        if (list.length && !routeId) setRouteId(String(list[0].id));
      } catch {
        toast.error('Failed to load routes');
      }
    };
    load();
  }, []);

  const recommend = useCallback(async () => {
    if (!routeId) return;
    setLoading(true);
    try {
      const res = await dispatchService.recommend(routeId, requiredCapacity);
      setPlan(res.data);
    } catch {
      toast.error('Dispatch optimizer is unavailable');
    } finally {
      setLoading(false);
    }
  }, [routeId, requiredCapacity]);

  useEffect(() => {
    if (routeId) recommend();
  }, [routeId, recommend]);

  const assign = async (rec) => {
    setAssigning(rec.rank);
    try {
      await tripService.create({
        vehicleId: rec.vehicleId,
        driverId: rec.driverId,
        routeId: Number(routeId),
        startTime: localDateTimePlusHours(2),
        notes: `Smart dispatch #${rec.rank}: ${rec.reason}`,
      });
      toast.success(`Trip created for ${rec.vehicleNumber} + ${rec.driverName}`);
      await recommend();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Could not create trip from recommendation');
    } finally {
      setAssigning(null);
    }
  };

  const selected = routes.find((r) => String(r.id) === String(routeId));

  return (
    <div>
      <div className="page-header">
        <h2 className="page-title"><FiCpu /> Assign</h2>
      </div>
      <p className="insight-engine" style={{ marginBottom: 16 }}>
        Spring Boot sends live fleet state to an ASP.NET Core 8 optimizer. The browser never calls C# directly.
      </p>

      <div className="filter-bar" style={{ marginBottom: 20 }}>
        <div className="filter-group" style={{ flex: 2 }}>
          <label className="filter-label">Route</label>
          <select className="form-input filter-input" value={routeId} onChange={(e) => setRouteId(e.target.value)}>
            {routes.map((r) => (
              <option key={r.id} value={r.id}>
                {r.origin} → {r.destination} ({r.distance} km · {r.estimatedTimeMinutes} min)
              </option>
            ))}
          </select>
        </div>
        <div className="filter-group">
          <label className="filter-label">Required capacity</label>
          <input
            className="form-input filter-input"
            type="number"
            min="1"
            value={requiredCapacity}
            onChange={(e) => setRequiredCapacity(Number(e.target.value) || 1)}
          />
        </div>
        <button className="btn btn-primary" style={{ alignSelf: 'flex-end' }} onClick={recommend} disabled={loading || !routeId}>
          {loading ? 'Scoring…' : 'Recommend pairs'}
        </button>
      </div>

      {selected && (
        <div className="stats-grid" style={{ marginBottom: 20 }}>
          <div className="stat-card"><div className="stat-info"><span className="stat-value">{selected.distance} km</span><span className="stat-label">Distance</span></div></div>
          <div className="stat-card"><div className="stat-info"><span className="stat-value">{selected.estimatedTimeMinutes} min</span><span className="stat-label">ETA</span></div></div>
          <div className="stat-card"><div className="stat-info"><span className="stat-value">{requiredCapacity}</span><span className="stat-label">Capacity needed</span></div></div>
        </div>
      )}

      {plan && (
        <div className="chart-card insight-panel">
          <div className="insight-header">
            <h3>{plan.routeLabel || 'Recommended assignments'}</h3>
          </div>
          <p className="insight-engine">Scored by {plan.engine}</p>
          {(plan.recommendations || []).length === 0 ? (
            <p className="empty-hint">No idle vehicle–driver pairs match this capacity. Free a unit from an active trip and try again.</p>
          ) : (
            <div className="table-container">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Rank</th>
                    <th>Score</th>
                    <th>Vehicle</th>
                    <th>Driver</th>
                    <th>Why this pair</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {plan.recommendations.map((rec) => (
                    <tr key={`${rec.rank}-${rec.vehicleId}`}>
                      <td>#{rec.rank}</td>
                      <td><strong>{rec.score}</strong></td>
                      <td>{rec.vehicleNumber} <span className="muted">({rec.vehicleType})</span></td>
                      <td>{rec.driverName}</td>
                      <td className="reason-cell">{rec.reason}</td>
                      <td>
                        <button
                          className="btn btn-primary"
                          onClick={() => assign(rec)}
                          disabled={assigning != null}
                        >
                          {assigning === rec.rank ? 'Creating…' : <><FiNavigation /> Assign trip</>}
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

export default SmartDispatch;
