import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { FiCheckCircle, FiMapPin, FiPlay, FiUser, FiClipboard } from 'react-icons/fi';
import { toast } from 'react-toastify';
import driverPortalService from '../../services/driverPortalService';
import tripService from '../../services/tripService';
import { podService } from '../../services/opsServices';
import StatCard from '../../components/stats/StatCard';
import StatusBadge from '../../components/common/StatusBadge';
import EntityAvatar from '../../components/common/EntityAvatar';
import Modal from '../../components/common/Modal';
import '../../styles/dashboard.css';

function DriverConsole() {
  const navigate = useNavigate();
  const [board, setBoard] = useState(null);
  const [loading, setLoading] = useState(true);
  const [podTrip, setPodTrip] = useState(null);
  const [statusFilter, setStatusFilter] = useState('');
  const [podForm, setPodForm] = useState({
    receiverName: '',
    receiverPhone: '',
    otp: '',
    notes: '',
  });

  const load = useCallback(async () => {
    try {
      const res = await driverPortalService.getBoard();
      setBoard(res.data);
    } catch (err) {
      toast.error(err.response?.data?.message || 'No driver profile is linked to this login');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const updateStatus = async (tripId, status) => {
    try {
      await tripService.updateStatus(tripId, status);
      toast.success(status === 'IN_PROGRESS' ? 'Trip started' : 'Trip completed');
      await load();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Could not update trip');
    }
  };

  const submitPod = async () => {
    try {
      await podService.submit(podTrip.id, {
        ...podForm,
        lrId: podTrip.lorryReceipts?.[0]?.id,
        latitude: 28.6139,
        longitude: 77.2090,
      });
      toast.success('Proof of delivery captured');
      setPodTrip(null);
      await load();
    } catch (err) {
      toast.error(err.response?.data?.message || 'POD failed');
    }
  };
  if (loading) return <div className="page-loader">Loading...</div>;
  if (!board) return <div className="page-loader">Driver profile not linked</div>;

  const trips = (board.trips || []).filter((t) => !statusFilter || t.status === statusFilter);

  return (
    <div>
      <div className="page-header">
        <h2 className="page-title"><FiUser /> My runs</h2>
      </div>
      <p className="insight-engine" style={{ marginBottom: 16 }}>
        Assigned work for {board.driverName}. Start and complete only your own trips.
      </p>

      <div className="stats-grid" style={{ marginBottom: 24 }}>
        <StatCard label="Planned" value={board.plannedCount || 0} color="#2196f3" onClick={() => setStatusFilter('PLANNED')} />
        <StatCard label="In progress" value={board.inProgressCount || 0} color="#ff9800" onClick={() => setStatusFilter('IN_PROGRESS')} />
        <StatCard label="Completed" value={board.completedCount || 0} color="#4caf50" onClick={() => setStatusFilter('COMPLETED')} />
        <StatCard label="Live map" value="GPS" color="#e65100" to="/fleet-map" />
      </div>

      <div className="table-container">
        <table className="data-table">
          <thead>
            <tr>
              <th>Vehicle</th>
              <th>Route</th>
              <th>Start</th>
              <th>ETA (min)</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {trips.length === 0 && (
              <tr><td colSpan="6" className="empty-row">No trips assigned yet</td></tr>
            )}
            {trips.map((trip) => (
              <tr
                key={trip.id}
                className="clickable-row"
                onClick={() => navigate(`/trips/${trip.id}/tracking`)}
              >
                <td>
                  <span className="entity-cell">
                    <EntityAvatar kind="vehicle" type={trip.vehicleType} size={28} />
                    {trip.vehicleNumber}
                  </span>
                </td>
                <td>{trip.routeOrigin ? `🗺️ ${trip.routeOrigin} → ${trip.routeDestination}` : '—'}</td>
                <td>{trip.startTime ? new Date(trip.startTime).toLocaleString() : '—'}</td>
                <td>{trip.routeEstimatedMinutes ?? '—'}</td>
                <td><StatusBadge status={trip.status} /></td>
                <td className="actions-cell" onClick={(e) => e.stopPropagation()}>
                  {(trip.status === 'PLANNED' || trip.status === 'IN_PROGRESS') && (
                    <button className="btn-icon btn-edit" title="Track" onClick={() => navigate(`/trips/${trip.id}/tracking`)}>
                      <FiMapPin />
                    </button>
                  )}
                  {trip.status === 'PLANNED' && (
                    <button className="btn-icon btn-start" title="Start trip" onClick={() => updateStatus(trip.id, 'IN_PROGRESS')}>
                      <FiPlay />
                    </button>
                  )}
                  {trip.status === 'IN_PROGRESS' && (
                    <>
                      <button className="btn-icon btn-complete" title="Complete trip" onClick={() => updateStatus(trip.id, 'COMPLETED')}>
                        <FiCheckCircle />
                      </button>
                      <button className="btn btn-sm btn-secondary" onClick={() => { setPodTrip(trip); setPodForm({ receiverName: 'Warehouse in-charge', receiverPhone: '9810011111', otp: '482913', notes: 'Received in good condition' }); }}>POD</button>
                    </>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <Modal isOpen={!!podTrip} onClose={() => setPodTrip(null)} title="Proof of delivery">
        <div className="entity-form">
          <p className="muted">GPS-stamped receipt for {podTrip?.vehicleNumber}. OTP is shared with the consignee.</p>
          <input className="form-input" placeholder="Receiver name" value={podForm.receiverName} onChange={(e) => setPodForm({ ...podForm, receiverName: e.target.value })} />
          <input className="form-input" placeholder="Receiver phone" value={podForm.receiverPhone} onChange={(e) => setPodForm({ ...podForm, receiverPhone: e.target.value })} style={{ marginTop: 8 }} />
          <input className="form-input" placeholder="OTP" value={podForm.otp} onChange={(e) => setPodForm({ ...podForm, otp: e.target.value })} style={{ marginTop: 8 }} />
          <textarea className="form-input" placeholder="Notes" value={podForm.notes} onChange={(e) => setPodForm({ ...podForm, notes: e.target.value })} style={{ marginTop: 8 }} />
          <div className="form-actions">
            <button className="btn btn-secondary" type="button" onClick={() => setPodTrip(null)}>Cancel</button>
            <button className="btn btn-primary" type="button" onClick={submitPod}><FiClipboard /> Capture POD</button>
          </div>
        </div>
      </Modal>
    </div>
  );
}

export default DriverConsole;
