import React, { useEffect, useState, useCallback } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { FiPlus, FiPlay, FiCheckCircle, FiFilter, FiChevronLeft, FiChevronRight, FiMapPin, FiLink } from 'react-icons/fi';
import { toast } from 'react-toastify';
import tripService from '../../services/tripService';
import DataTable from '../../components/common/DataTable';
import Modal from '../../components/common/Modal';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import StatusBadge from '../../components/common/StatusBadge';
import EntityAvatar from '../../components/common/EntityAvatar';
import TripForm from './TripForm';
import { useAuth } from '../../context/AuthContext';

const TRIP_STATUSES = ['PLANNED', 'IN_PROGRESS', 'COMPLETED'];

function TripList() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { user } = useAuth();
  const canManage = ['ADMIN', 'DISPATCHER'].includes(user?.role);
  const [trips, setTrips] = useState([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editTrip, setEditTrip] = useState(null);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const pageSize = 10;

  const [filters, setFilters] = useState({ status: searchParams.get('status') || '', startFrom: '', startTo: '' });
  const [showFilters, setShowFilters] = useState(!!searchParams.get('status'));

  const formatDate = (d) => d ? new Date(d).toLocaleString() : '—';

  const columns = [
    {
      key: 'vehicleNumber',
      label: 'Vehicle',
      render: (v, row) => (
        <span className="entity-cell">
          <EntityAvatar kind="vehicle" type={row.vehicleType} size={32} />
          {v}
        </span>
      ),
    },
    {
      key: 'driverName',
      label: 'Driver',
      render: (v) => (
        <span className="entity-cell">
          <EntityAvatar name={v} size={32} />
          {v}
        </span>
      ),
    },
    { key: 'routeOrigin', label: 'Route', render: (v, row) => row.routeOrigin ? `🗺️ ${row.routeOrigin} → ${row.routeDestination}` : '—' },
    { key: 'startTime', label: 'Start Time', render: (v) => formatDate(v) },
    { key: 'endTime', label: 'End Time', render: (v) => formatDate(v) },
    { key: 'status', label: 'Status', render: (v) => <StatusBadge status={v} /> },
    { key: 'lorryReceipts', label: 'LRs', render: (lrs) => lrs?.length || 0 },
  ];

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const params = { page, size: pageSize };
      if (filters.status) params.status = filters.status;
      if (filters.startFrom) params.startFrom = filters.startFrom;
      if (filters.startTo) params.startTo = filters.startTo;
      const res = await tripService.getAll(params);
      setTrips(res.data.content);
      setTotalPages(res.data.totalPages);
      setTotalElements(res.data.totalElements);
    } catch (err) {
      toast.error('Failed to load trips');
    } finally {
      setLoading(false);
    }
  }, [page, filters]);

  useEffect(() => { loadData(); }, [loadData]);

  useEffect(() => {
    const status = searchParams.get('status') || '';
    setFilters((f) => (f.status === status ? f : { ...f, status }));
    if (status) setShowFilters(true);
    setPage(0);
  }, [searchParams]);

  const handleCreate = () => { setEditTrip(null); setModalOpen(true); };
  const handleEdit = (trip) => { setEditTrip(trip); setModalOpen(true); };

  const handleSubmit = async (data) => {
    try {
      if (editTrip) {
        await tripService.update(editTrip.id, data);
        toast.success('Trip updated');
      } else {
        await tripService.create(data);
        toast.success('Trip created');
      }
      setModalOpen(false);
      loadData();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Operation failed');
    }
  };

  const handleStatusChange = async (tripId, status) => {
    try {
      await tripService.updateStatus(tripId, status);
      toast.success(`Trip ${status.toLowerCase().replace(/_/g, ' ')}`);
      loadData();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Status update failed');
    }
  };

  const handleDelete = async () => {
    try {
      await tripService.delete(deleteTarget.id);
      toast.success('Trip deleted');
      setDeleteTarget(null);
      loadData();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Delete failed');
    }
  };

  const tripActions = (row) => (
    <>
      {(row.status === 'IN_PROGRESS' || row.status === 'PLANNED') && (
        <button className="btn-icon btn-edit" onClick={() => navigate(`/trips/${row.id}/tracking`)} title="Track on Map">
          <FiMapPin />
        </button>
      )}
      {row.trackingToken && (
        <button
          className="btn-icon btn-edit"
          title="Copy public tracking link"
          onClick={() => {
            navigator.clipboard.writeText(`${window.location.origin}/track/${row.trackingToken}`);
            toast.success('Public tracking link copied');
          }}
        >
          <FiLink />
        </button>
      )}
      {row.status === 'PLANNED' && (
        <button className="btn-icon btn-start" onClick={() => handleStatusChange(row.id, 'IN_PROGRESS')} title="Start Trip">
          <FiPlay />
        </button>
      )}
      {row.status === 'IN_PROGRESS' && (
        <button className="btn-icon btn-complete" onClick={() => handleStatusChange(row.id, 'COMPLETED')} title="Complete Trip">
          <FiCheckCircle />
        </button>
      )}
    </>
  );

  if (loading) return <div className="page-loader">Loading...</div>;

  const handleFilterChange = (field) => (e) => {
    setFilters({ ...filters, [field]: e.target.value });
    setPage(0);
  };
  const clearFilters = () => { setFilters({ status: '', startFrom: '', startTo: '' }); setPage(0); };
  const hasActiveFilters = filters.status || filters.startFrom || filters.startTo;

  return (
    <div>
      <div className="page-header">
        <h2 className="page-title">Runs</h2>
        <div style={{ display: 'flex', gap: '8px' }}>
          <button
            className={`btn ${showFilters ? 'btn-secondary' : 'btn-outline'}`}
            onClick={() => setShowFilters(!showFilters)}
          >
            <FiFilter /> Filters {hasActiveFilters && <span className="filter-dot" />}
          </button>
          {canManage && (
            <button className="btn btn-primary" onClick={handleCreate}><FiPlus /> Add Trip</button>
          )}
        </div>
      </div>

      {showFilters && (
        <div className="filter-bar">
          <div className="filter-group">
            <label className="filter-label">Status</label>
            <select className="form-input filter-input" value={filters.status} onChange={handleFilterChange('status')}>
              <option value="">All Statuses</option>
              {TRIP_STATUSES.map(s => <option key={s} value={s}>{s.replace(/_/g, ' ')}</option>)}
            </select>
          </div>
          <div className="filter-group">
            <label className="filter-label">Start From</label>
            <input className="form-input filter-input" type="datetime-local" value={filters.startFrom} onChange={handleFilterChange('startFrom')} />
          </div>
          <div className="filter-group">
            <label className="filter-label">Start To</label>
            <input className="form-input filter-input" type="datetime-local" value={filters.startTo} onChange={handleFilterChange('startTo')} />
          </div>
          {hasActiveFilters && (
            <button className="btn btn-link" onClick={clearFilters} style={{ alignSelf: 'flex-end' }}>Clear</button>
          )}
        </div>
      )}

      <DataTable
        columns={columns}
        data={trips}
        onEdit={canManage ? handleEdit : undefined}
        onDelete={canManage ? (row) => setDeleteTarget(row) : undefined}
        actions={tripActions}
        onRowClick={(row) => navigate(`/trips/${row.id}/tracking`)}
      />
      {totalPages > 1 && (
        <div className="pagination">
          <button disabled={page === 0} onClick={() => setPage(p => p - 1)}>
            <FiChevronLeft />
          </button>
          <span>Page {page + 1} of {totalPages} ({totalElements} total)</span>
          <button disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)}>
            <FiChevronRight />
          </button>
        </div>
      )}
      <Modal isOpen={modalOpen} onClose={() => setModalOpen(false)} title={editTrip ? 'Edit Trip' : 'Create Trip'}>
        <TripForm trip={editTrip} onSubmit={handleSubmit} onCancel={() => setModalOpen(false)} />
      </Modal>
      <ConfirmDialog
        isOpen={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleDelete}
        title="Delete Trip"
        message="Delete this trip?"
      />
    </div>
  );
}

export default TripList;

