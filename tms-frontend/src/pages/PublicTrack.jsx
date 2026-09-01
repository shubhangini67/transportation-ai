import React, { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { CircleMarker, MapContainer, Popup } from 'react-leaflet';
import { publicTrackService } from '../services/opsServices';
import IndiaTileLayer from '../components/maps/IndiaTileLayer';
import { INDIA_CENTER, INDIA_ZOOM, INDIA_BOUNDS } from '../constants/indiaMap';
import { DEMO_TRACK_TOKENS } from '../constants/demoAccess';
import 'leaflet/dist/leaflet.css';
import '../styles/dashboard.css';
import '../styles/forms.css';

function PublicTrack() {
  const { token } = useParams();
  const [data, setData] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    setError('');
    setData(null);
    publicTrackService.get(token)
      .then((res) => setData(res.data))
      .catch(() => setError('No run for that token. Try LANE-DEMO or LIVE-DEMO.'));
  }, [token]);

  const pos = data?.latitude && data?.longitude ? [data.latitude, data.longitude] : INDIA_CENTER;

  return (
    <div className="public-track">
      <header className="public-track-bar">
        <Link to="/login" className="auth-mini-brand">Transportation AI</Link>
        <span className="public-track-token"><code>{token}</code></span>
        <Link to="/login" className="auth-link">Sign in</Link>
      </header>
      <div className="public-track-body">
        {error && (
          <div className="form-error-banner">
            {error}
            <div className="track-demo-tokens" style={{ marginTop: 12 }}>
              {DEMO_TRACK_TOKENS.map((t) => (
                <Link key={t.token} to={`/track/${t.token}`} className="track-token-chip">
                  <code>{t.token}</code>
                  <span>{t.label}</span>
                </Link>
              ))}
            </div>
          </div>
        )}
        {data && (
          <>
            <div className="stats-grid" style={{ marginBottom: 16 }}>
              <div className="stat-card"><div className="stat-info"><span className="stat-value">{data.status.replace('_', ' ')}</span><span className="stat-label">Status</span></div></div>
              <div className="stat-card"><div className="stat-info"><span className="stat-value">{data.vehicleNumber}</span><span className="stat-label">Truck</span></div></div>
              <div className="stat-card"><div className="stat-info"><span className="stat-value">{data.driverName}</span><span className="stat-label">Crew</span></div></div>
              <div className="stat-card"><div className="stat-info"><span className="stat-value">{data.routeLabel}</span><span className="stat-label">Lane</span></div></div>
            </div>
            <div className="india-map-frame" style={{ height: 420, marginBottom: 16 }}>
              <MapContainer center={pos} zoom={data.latitude ? 8 : INDIA_ZOOM} minZoom={4} maxZoom={16}
                maxBounds={INDIA_BOUNDS} style={{ height: '100%', width: '100%' }}>
                <IndiaTileLayer />
                {data.latitude && (
                  <CircleMarker center={pos} radius={10} pathOptions={{ fillColor: '#c45c26', color: '#fff', fillOpacity: 1 }}>
                    <Popup>{data.currentLocation}</Popup>
                  </CircleMarker>
                )}
              </MapContainer>
            </div>
            {data.consignmentHints?.length > 0 && (
              <div className="chart-card"><h3>Consignments</h3><ul>{data.consignmentHints.map((h) => <li key={h}>{h}</li>)}</ul></div>
            )}
          </>
        )}
      </div>
    </div>
  );
}

export default PublicTrack;
