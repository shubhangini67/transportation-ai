import React, { useEffect, useState } from 'react';
import { FiPercent } from 'react-icons/fi';
import { toast } from 'react-toastify';
import { rateService } from '../../services/opsServices';
import routeService from '../../services/routeService';
import '../../styles/dashboard.css';
import '../../styles/forms.css';

function RateCards() {
  const [cards, setCards] = useState([]);
  const [routes, setRoutes] = useState([]);
  const [routeId, setRouteId] = useState('');
  const [vehicleType, setVehicleType] = useState('TRUCK');
  const [quote, setQuote] = useState(null);

  useEffect(() => {
    Promise.all([rateService.list(), routeService.getActive()])
      .then(([c, r]) => {
        setCards(c.data || []);
        const list = r.data || [];
        setRoutes(list);
        if (list[0]) setRouteId(String(list[0].id));
      })
      .catch(() => toast.error('Failed to load rate cards'));
  }, []);

  const runQuote = async () => {
    try {
      const res = await rateService.quote(routeId, vehicleType);
      setQuote(res.data);
    } catch (err) {
      setQuote(null);
      toast.error(err.response?.data?.message || 'No rate card for this lane / type');
    }
  };

  return (
    <div>
      <div className="page-header"><h2 className="page-title"><FiPercent /> Tariffs (GST 18%)</h2></div>
      <p className="insight-engine">Lane rate cards with per-km charge, minimum freight, and GST. Quote before you dispatch.</p>

      <div className="filter-bar" style={{ marginBottom: 20 }}>
        <div className="filter-group" style={{ flex: 2 }}>
          <label className="filter-label">Route</label>
          <select className="form-input" value={routeId} onChange={(e) => setRouteId(e.target.value)}>
            {routes.map((r) => <option key={r.id} value={r.id}>{r.origin} → {r.destination} ({r.distance} km)</option>)}
          </select>
        </div>
        <div className="filter-group">
          <label className="filter-label">Vehicle type</label>
          <select className="form-input" value={vehicleType} onChange={(e) => setVehicleType(e.target.value)}>
            {['TRUCK', 'VAN', 'BUS', 'MINI_BUS'].map((t) => <option key={t} value={t}>{t}</option>)}
          </select>
        </div>
        <button className="btn btn-primary" style={{ alignSelf: 'flex-end' }} onClick={runQuote}>Quote GST</button>
      </div>

      {quote && (
        <div className="stats-grid" style={{ marginBottom: 20 }}>
          <div className="stat-card"><div className="stat-info"><span className="stat-value">₹{Number(quote.ratePerKm).toLocaleString()}</span><span className="stat-label">Per km</span></div></div>
          <div className="stat-card"><div className="stat-info"><span className="stat-value">₹{Number(quote.quotedSubtotal).toLocaleString()}</span><span className="stat-label">Taxable freight</span></div></div>
          <div className="stat-card"><div className="stat-info"><span className="stat-value">₹{Number(quote.quotedGst).toLocaleString()}</span><span className="stat-label">GST {quote.gstPercent}%</span></div></div>
          <div className="stat-card"><div className="stat-info"><span className="stat-value">₹{Number(quote.quotedTotal).toLocaleString()}</span><span className="stat-label">Customer total</span></div></div>
        </div>
      )}

      <div className="table-container">
        <table className="data-table">
          <thead><tr><th>Lane</th><th>Type</th><th>₹ / km</th><th>Min charge</th><th>GST</th></tr></thead>
          <tbody>
            {cards.map((c) => (
              <tr key={c.id}>
                <td>{c.origin} → {c.destination}</td>
                <td>{c.vehicleType}</td>
                <td>₹{Number(c.ratePerKm).toLocaleString()}</td>
                <td>₹{Number(c.minCharge).toLocaleString()}</td>
                <td>{c.gstPercent}%</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export default RateCards;
