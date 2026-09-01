import React, { useEffect, useState, useRef, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { FiArrowLeft, FiPlay, FiSquare } from 'react-icons/fi';
import { MapContainer, Marker, Popup, Polyline, useMap } from 'react-leaflet';
import L from 'leaflet';
import tripService from '../../services/tripService';
import locationService from '../../services/locationService';
import websocketService from '../../services/websocketService';
import IndiaTileLayer from '../../components/maps/IndiaTileLayer';
import { INDIA_CENTER } from '../../constants/indiaMap';
import 'leaflet/dist/leaflet.css';

// Fix default marker icon for Leaflet in React
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon-2x.png',
  iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-shadow.png',
});

function MapRecenter({ position }) {
  const map = useMap();
  useEffect(() => { if (position) map.setView(position, map.getZoom()); }, [position, map]);
  return null;
}

function TripTracking() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [trip, setTrip] = useState(null);
  const [position, setPosition] = useState(null);
  const [routeHistory, setRouteHistory] = useState([]);
  const [liveTrail, setLiveTrail] = useState([]);
  const [replaying, setReplaying] = useState(false);
  const [replayIdx, setReplayIdx] = useState(0);
  const replayTimer = useRef(null);

  useEffect(() => {
    const load = async () => {
      try {
        const res = await tripService.getTracking(id);
        const data = res.data;
        setTrip(data);
        const pos = data.vehicleLatitude && data.vehicleLongitude
          ? [data.vehicleLatitude, data.vehicleLongitude]
          : INDIA_CENTER;
        setPosition(pos);
        try {
          const histRes = await locationService.getRouteHistory(id);
          if (histRes.data?.length > 0) {
            setRouteHistory(histRes.data.map(p => [p.latitude, p.longitude]));
          }
        } catch { /* no history */ }
      } catch { setPosition(INDIA_CENTER); }
    };
    load();
  }, [id]);

  useEffect(() => {
    if (!trip?.vehicleId) return;
    const token = localStorage.getItem('token');
    if (!token) return;
    let mounted = true;
    const connectWs = async () => {
      try {
        await websocketService.connect(token);
        if (!mounted) return;
        websocketService.subscribe(`/topic/vehicle/${trip.vehicleId}/location`, (loc) => {
          if (!mounted) return;
          const newPos = [loc.latitude, loc.longitude];
          setPosition(newPos);
          setLiveTrail(prev => [...prev, newPos]);
        });
      } catch { /* fall back to polling */ }
    };
    connectWs();
    return () => { mounted = false; if (trip?.vehicleId) websocketService.unsubscribe(`/topic/vehicle/${trip.vehicleId}/location`); };
  }, [trip?.vehicleId]);

  const startReplay = useCallback(() => {
    if (routeHistory.length === 0) return;
    setReplaying(true);
    setReplayIdx(0);
    replayTimer.current = setInterval(() => {
      setReplayIdx(prev => {
        if (prev >= routeHistory.length - 1) { clearInterval(replayTimer.current); setReplaying(false); return prev; }
        setPosition(routeHistory[prev + 1]);
        return prev + 1;
      });
    }, 300);
  }, [routeHistory]);

  const stopReplay = () => { clearInterval(replayTimer.current); setReplaying(false); };
  useEffect(() => () => clearInterval(replayTimer.current), []);

  if (!position) return <div className="page-loader">Loading map...</div>;
  const trailPositions = routeHistory.length > 0 ? routeHistory : liveTrail;

  return (
    <div>
      <div className="page-header">
        <h2 className="page-title">
          <button className="btn-icon" onClick={() => navigate('/trips')} style={{ marginRight: 8 }}><FiArrowLeft /></button>
          Trip tracking {trip ? `— ${trip.vehicleNumber}` : ''}
        </h2>
        <div style={{ display: 'flex', gap: 8 }}>
          {routeHistory.length > 0 && !replaying && (
            <button className="btn btn-primary" onClick={startReplay}><FiPlay /> Replay Route</button>
          )}
          {replaying && (
            <button className="btn btn-secondary" onClick={stopReplay}><FiSquare /> Stop</button>
          )}
          <span style={{ fontSize: 12, color: 'var(--text-secondary)', alignSelf: 'center' }}>
            {liveTrail.length > 0 ? '🟢 Live' : routeHistory.length > 0 ? `${routeHistory.length} points` : '⏳ Waiting for GPS data'}
          </span>
        </div>
      </div>
      <div style={{ height: 550, borderRadius: 'var(--radius)', overflow: 'hidden', boxShadow: 'var(--shadow-md)' }}>
        <MapContainer center={position} zoom={6} style={{ height: '100%', width: '100%' }}>
          <IndiaTileLayer />
          {trailPositions.length > 1 && (
            <Polyline positions={replaying ? trailPositions.slice(0, replayIdx + 1) : trailPositions}
                      color="#673ab7" weight={4} opacity={0.7} />
          )}
          <Marker position={position}>
            <Popup>
              <strong>{trip?.vehicleNumber || 'Vehicle'}</strong><br />
              Driver: {trip?.driverName || 'N/A'}<br />
              Status: {trip?.status || 'Unknown'}
            </Popup>
          </Marker>
          <MapRecenter position={position} />
        </MapContainer>
      </div>
    </div>
  );
}

export default TripTracking;

