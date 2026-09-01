import React, { useEffect, useMemo, useState } from 'react';
import { MapContainer, Circle, CircleMarker, Popup, useMapEvents, useMap } from 'react-leaflet';
import { FiPlus, FiTarget } from 'react-icons/fi';
import { toast } from 'react-toastify';
import geofenceService from '../../services/geofenceService';
import IndiaTileLayer from '../../components/maps/IndiaTileLayer';
import { INDIA_CENTER, INDIA_HUBS, INDIA_ZOOM, INDIA_BOUNDS } from '../../constants/indiaMap';
import Modal from '../../components/common/Modal';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import { useAuth } from '../../context/AuthContext';
import 'leaflet/dist/leaflet.css';
import '../../styles/dashboard.css';
import '../../styles/components.css';

const COLORS = { DEPOT: '#2e7d32', RESTRICTED_ZONE: '#c62828', DELIVERY_ZONE: '#1565c0', CUSTOM: '#6a1b9a' };

function Recenter({ center, zoom }) {
  const map = useMap();
  useEffect(() => { map.setView(center, zoom); }, [center, zoom, map]);
  return null;
}

function ClickHandler({ onPick }) {
  useMapEvents({ click(e) { onPick(e.latlng.lat, e.latlng.lng); } });
  return null;
}

function GeofenceForm({ geofence, onSubmit, onCancel }) {
  const [form, setForm] = useState({
    name: geofence?.name || '',
    description: geofence?.description || '',
    latitude: geofence?.latitude || INDIA_CENTER[0],
    longitude: geofence?.longitude || INDIA_CENTER[1],
    radiusMeters: geofence?.radiusMeters || 800,
    type: geofence?.type || 'DEPOT',
    active: geofence?.active !== undefined ? geofence.active : true,
  });
  const set = (field, value) => setForm((f) => ({ ...f, [field]: value }));

  return (
    <div className="entity-form">
      <div className="hub-chips">
        {INDIA_HUBS.map((h) => (
          <button type="button" key={h.name} className="hub-chip" onClick={() => { set('latitude', h.lat); set('longitude', h.lng); set('name', form.name || `${h.name} zone`); }}>
            {h.name}
          </button>
        ))}
      </div>
      <div className="form-grid">
        <div className="form-field"><label className="form-label">Name *</label><input className="form-input" value={form.name} onChange={(e) => set('name', e.target.value)} /></div>
        <div className="form-field">
          <label className="form-label">Type</label>
          <select className="form-input" value={form.type} onChange={(e) => set('type', e.target.value)}>
            <option value="DEPOT">Depot / yard</option>
            <option value="DELIVERY_ZONE">Delivery zone</option>
            <option value="RESTRICTED_ZONE">Restricted</option>
            <option value="CUSTOM">Custom</option>
          </select>
        </div>
        <div className="form-field"><label className="form-label">Radius (m)</label><input className="form-input" type="number" min="50" value={form.radiusMeters} onChange={(e) => set('radiusMeters', parseFloat(e.target.value))} /></div>
        <div className="form-field"><label className="form-label"><input type="checkbox" checked={form.active} onChange={(e) => set('active', e.target.checked)} /> Active</label></div>
      </div>
      <div className="india-map-frame" style={{ height: 280, margin: '12px 0' }}>
        <MapContainer center={[form.latitude, form.longitude]} zoom={11} style={{ height: '100%', width: '100%' }}>
          <IndiaTileLayer />
          <Recenter center={[form.latitude, form.longitude]} zoom={11} />
          <Circle center={[form.latitude, form.longitude]} radius={form.radiusMeters} pathOptions={{ color: COLORS[form.type], fillOpacity: 0.25 }} />
          <ClickHandler onPick={(lat, lng) => { set('latitude', lat); set('longitude', lng); }} />
        </MapContainer>
      </div>
      <p className="muted">Click the map to drop the zone on an Indian city. {form.latitude.toFixed(4)}, {form.longitude.toFixed(4)}</p>
      <div className="form-actions">
        <button className="btn btn-secondary" type="button" onClick={onCancel}>Cancel</button>
        <button className="btn btn-primary" type="button" onClick={() => onSubmit(form)}>{geofence ? 'Update' : 'Create'}</button>
      </div>
    </div>
  );
}

function GeofenceList() {
  const { hasRole } = useAuth();
  const [geofences, setGeofences] = useState([]);
  const [events, setEvents] = useState([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [deleteTarget, setDeleteTarget] = useState(null);

  const load = async () => {
    try {
      const [g, e] = await Promise.all([
        geofenceService.getAll({ page: 0, size: 100 }),
        geofenceService.getRecentEvents({ page: 0, size: 12 }),
      ]);
      setGeofences(g.data?.content || []);
      setEvents(e.data?.content || []);
    } catch { toast.error('Failed to load geofences'); }
  };

  useEffect(() => { load(); }, []);

  const handleSubmit = async (data) => {
    try {
      if (editing) await geofenceService.update(editing.id, data);
      else await geofenceService.create(data);
      toast.success(editing ? 'Zone updated' : 'Zone created');
      setModalOpen(false); setEditing(null); load();
    } catch { toast.error('Save failed'); }
  };

  const mapCenter = useMemo(() => INDIA_CENTER, []);

  return (
    <div>
      <div className="page-header">
        <h2 className="page-title"><FiTarget /> Zones</h2>
        <button className="btn btn-primary" onClick={() => { setEditing(null); setModalOpen(true); }}><FiPlus /> Add zone</button>
      </div>
      <p className="insight-engine">Circular control zones on an India basemap — depots, ports, delivery hubs and restricted areas. Click the map when creating a zone.</p>

      <div className="map-legend">
        <span><i style={{ background: COLORS.DEPOT }} /> Depot</span>
        <span><i style={{ background: COLORS.DELIVERY_ZONE }} /> Delivery</span>
        <span><i style={{ background: COLORS.RESTRICTED_ZONE }} /> Restricted</span>
        <span><i style={{ background: COLORS.CUSTOM }} /> Custom</span>
      </div>

      <div className="india-map-frame" style={{ height: 480, marginBottom: 20 }}>
        <MapContainer center={mapCenter} zoom={INDIA_ZOOM} minZoom={4} maxZoom={16}
          maxBounds={INDIA_BOUNDS} maxBoundsViscosity={0.85} style={{ height: '100%', width: '100%' }}>
          <IndiaTileLayer />
          {INDIA_HUBS.map((h) => (
            <CircleMarker key={h.name} center={[h.lat, h.lng]} radius={4}
              pathOptions={{ color: '#37474f', fillColor: '#eceff1', fillOpacity: 1, weight: 1 }}>
              <Popup>{h.name}</Popup>
            </CircleMarker>
          ))}
          {geofences.map((g) => (
            <React.Fragment key={g.id}>
              <CircleMarker center={[g.latitude, g.longitude]} radius={11}
                pathOptions={{ color: '#fff', weight: 2, fillColor: COLORS[g.type] || '#6a1b9a', fillOpacity: 0.95 }}>
                <Popup><strong>{g.name}</strong><br />{g.type.replace(/_/g, ' ')} · {g.radiusMeters} m<br />{g.description}</Popup>
              </CircleMarker>
              <Circle center={[g.latitude, g.longitude]} radius={g.radiusMeters}
                pathOptions={{ color: COLORS[g.type], fillColor: COLORS[g.type], fillOpacity: 0.18, weight: 2 }} />
            </React.Fragment>
          ))}
        </MapContainer>
      </div>

      <div className="charts-grid">
        <div className="chart-card">
          <h3>Zones</h3>
          <div className="table-container">
            <table className="data-table">
              <thead><tr><th>Name</th><th>Type</th><th>Radius</th><th>Active</th><th></th></tr></thead>
              <tbody>
                {geofences.map((g) => (
                  <tr key={g.id}>
                    <td>{g.name}</td>
                    <td><span className="risk-badge" style={{ background: COLORS[g.type], color: '#fff' }}>{g.type.replace(/_/g, ' ')}</span></td>
                    <td>{g.radiusMeters} m</td>
                    <td>{g.active ? 'Yes' : 'No'}</td>
                    <td>
                      <button className="btn btn-sm btn-secondary" onClick={() => { setEditing(g); setModalOpen(true); }}>Edit</button>
                      {hasRole('ADMIN') && <button className="btn btn-sm btn-danger" onClick={() => setDeleteTarget(g)}>Delete</button>}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
        <div className="chart-card">
          <h3>Recent enter / exit</h3>
          {events.length === 0 ? <p className="empty-hint">No crossings yet. GPS updates on in-progress trips generate events.</p> : (
            <ul className="insight-list">
              {events.map((ev) => (
                <li key={ev.id}><strong>{ev.eventType}</strong> {ev.vehicleNumber || 'vehicle'} · {ev.geofenceName}<br /><span className="muted">{ev.eventTime ? new Date(ev.eventTime).toLocaleString() : ''}</span></li>
              ))}
            </ul>
          )}
        </div>
      </div>

      <Modal isOpen={modalOpen} onClose={() => setModalOpen(false)} title={editing ? 'Edit zone' : 'New India zone'}>
        <GeofenceForm geofence={editing} onSubmit={handleSubmit} onCancel={() => setModalOpen(false)} />
      </Modal>
      <ConfirmDialog isOpen={!!deleteTarget} onClose={() => setDeleteTarget(null)}
        onConfirm={async () => { await geofenceService.delete(deleteTarget.id); setDeleteTarget(null); load(); }}
        title="Delete zone" message={`Delete ${deleteTarget?.name}?`} />
    </div>
  );
}

export default GeofenceList;
