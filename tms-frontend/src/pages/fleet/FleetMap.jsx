import React, { useEffect, useMemo, useState } from 'react';
import { MapContainer, Marker, Popup, useMap } from 'react-leaflet';
import L from 'leaflet';
import { FiRadio, FiTruck } from 'react-icons/fi';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import vehicleService from '../../services/vehicleService';
import tripService from '../../services/tripService';
import StatCard from '../../components/stats/StatCard';
import EntityAvatar from '../../components/common/EntityAvatar';
import IndiaTileLayer from '../../components/maps/IndiaTileLayer';
import { INDIA_CENTER, INDIA_ZOOM, INDIA_BOUNDS } from '../../constants/indiaMap';
import { vehicleEmoji } from '../../constants/entityVisuals';
import 'leaflet/dist/leaflet.css';
import '../../styles/dashboard.css';

const STATUS_COLOR = { AVAILABLE: '#2e7d32', BUSY: '#e65100', MAINTENANCE: '#c62828' };

function vehicleMapIcon(type, status) {
  const color = STATUS_COLOR[status] || '#1565c0';
  const pulse = status === 'BUSY' ? 'is-live' : '';
  return L.divIcon({
    className: 'live-pin',
    html: `<span class="map-vehicle-pin ${pulse}" style="border-color:${color}">${vehicleEmoji(type)}</span>`,
    iconSize: [34, 34],
    iconAnchor: [17, 17],
  });
}

function FlyTo({ position, zoom }) {
  const map = useMap();
  useEffect(() => {
    if (position) map.flyTo(position, zoom || Math.max(map.getZoom(), 8), { duration: 0.8 });
  }, [position, zoom, map]);
  return null;
}

function FleetMap() {
  const navigate = useNavigate();
  const [vehicles, setVehicles] = useState([]);
  const [liveTrips, setLiveTrips] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [query, setQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('');

  const load = async () => {
    try {
      const [fleetRes, tripRes] = await Promise.all([
        vehicleService.getFleet(),
        tripService.getAll({ status: 'IN_PROGRESS', page: 0, size: 50 }),
      ]);
      setVehicles(fleetRes.data || []);
      setLiveTrips(tripRes.data?.content || []);
    } catch {
      toast.error('Failed to load live positions');
    }
  };

  useEffect(() => {
    load();
    const id = setInterval(load, 5000);
    return () => clearInterval(id);
  }, []);

  const tripByVehicle = useMemo(() => {
    const map = {};
    liveTrips.forEach((t) => { if (t.vehicleId) map[t.vehicleId] = t; });
    return map;
  }, [liveTrips]);

  const filtered = vehicles.filter((v) => {
    if (statusFilter && v.status !== statusFilter) return false;
    if (query && !`${v.vehicleNumber} ${v.currentLocation || ''}`.toLowerCase().includes(query.toLowerCase())) return false;
    return true;
  });
  const withGps = filtered.filter((v) => v.latitude && v.longitude);
  const selected = vehicles.find((v) => v.id === selectedId);
  const selectedPos = selected?.latitude ? [selected.latitude, selected.longitude] : null;

  return (
    <div>
      <div className="page-header">
        <h2 className="page-title"><FiRadio /> Live map — India</h2>
        <span className="live-pill">LIVE GPS · updates every 5–7s</span>
      </div>

      <div className="stats-grid" style={{ marginBottom: 16 }}>
        <StatCard icon={<FiTruck />} label="Fleet" value={vehicles.length} to="/vehicles" />
        <StatCard icon={<span className="stat-emoji">🛣️</span>} label="On trip" value={vehicles.filter((v) => v.status === 'BUSY').length} color="#e65100" to="/trips?status=IN_PROGRESS" />
        <StatCard icon={<span className="stat-emoji">🔧</span>} label="Workshop" value={vehicles.filter((v) => v.status === 'MAINTENANCE').length} color="#c62828" to="/vehicles?status=MAINTENANCE" />
        <StatCard icon={<span className="stat-emoji">📡</span>} label="With GPS" value={vehicles.filter((v) => v.latitude).length} color="#2e7d32" onClick={() => setStatusFilter('')} />
      </div>

      <div className="live-track-layout">
        <aside className="live-track-list">
          <input
            className="form-input"
            placeholder="Search vehicle…"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
          <div className="hub-chips" style={{ marginTop: 10 }}>
            {[
              { id: '', label: 'All 🧭' },
              { id: 'BUSY', label: '🟠 On trip' },
              { id: 'AVAILABLE', label: '🟢 Free' },
              { id: 'MAINTENANCE', label: '🔧 Shop' },
            ].map((s) => (
              <button key={s.id || 'all'} type="button" className={`hub-chip ${statusFilter === s.id ? 'hub-chip-active' : ''}`}
                onClick={() => setStatusFilter(s.id)}>
                {s.label}
              </button>
            ))}
          </div>
          <div className="live-track-scroll">
            {filtered.map((v) => {
              const trip = tripByVehicle[v.id];
              return (
                <button
                  type="button"
                  key={v.id}
                  className={`live-vehicle-card ${selectedId === v.id ? 'is-selected' : ''}`}
                  onClick={() => setSelectedId(v.id)}
                >
                  <EntityAvatar kind="vehicle" type={v.type} size={40} />
                  <span>
                    <strong>{v.vehicleNumber}</strong>
                    <small>{trip ? `🛣️ ${trip.routeOrigin} → ${trip.routeDestination}` : `${vehicleEmoji(v.type)} ${v.status}`}</small>
                    <small>{v.currentLocation || 'No last location'}</small>
                  </span>
                </button>
              );
            })}
          </div>
        </aside>

        <div className="india-map-frame live-track-map">
          <MapContainer center={INDIA_CENTER} zoom={INDIA_ZOOM} minZoom={4} maxZoom={16}
            maxBounds={INDIA_BOUNDS} maxBoundsViscosity={0.85} style={{ height: '100%', width: '100%' }}>
            <IndiaTileLayer />
            {selectedPos && <FlyTo position={selectedPos} zoom={8} />}
            {withGps.map((v) => (
              <Marker key={v.id} position={[v.latitude, v.longitude]} icon={vehicleMapIcon(v.type, v.status)}
                eventHandlers={{ click: () => setSelectedId(v.id) }}>
                <Popup>
                  <strong>{vehicleEmoji(v.type)} {v.vehicleNumber}</strong><br />
                  {v.type?.replace(/_/g, ' ')} · {v.status}<br />
                  {v.currentLocation}<br />
                  {tripByVehicle[v.id] && (
                    <button className="btn btn-sm btn-primary" style={{ marginTop: 8 }}
                      onClick={() => navigate(`/trips/${tripByVehicle[v.id].id}/tracking`)}>
                      Open trip track
                    </button>
                  )}
                </Popup>
              </Marker>
            ))}
          </MapContainer>
        </div>
      </div>
    </div>
  );
}

export default FleetMap;
