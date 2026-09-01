import React, { useEffect, useState, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import { FiPlus, FiFilter, FiChevronLeft, FiChevronRight } from 'react-icons/fi';
import { toast } from 'react-toastify';
import vehicleService from '../../services/vehicleService';
import DataTable from '../../components/common/DataTable';
import Modal from '../../components/common/Modal';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import StatusBadge from '../../components/common/StatusBadge';
import EntityAvatar from '../../components/common/EntityAvatar';
import VehicleForm from './VehicleForm';

const VEHICLE_TYPES = ['BUS', 'MINI_BUS', 'VAN', 'TRUCK'];
const VEHICLE_STATUSES = ['AVAILABLE', 'BUSY', 'MAINTENANCE'];

function VehicleList() {
  const [searchParams] = useSearchParams();
  const [vehicles, setVehicles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editVehicle, setEditVehicle] = useState(null);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const pageSize = 10;

  const [filters, setFilters] = useState({ type: '', status: searchParams.get('status') || '' });
  const [showFilters, setShowFilters] = useState(!!searchParams.get('status'));

  const columns = [
    {
      key: 'vehicleNumber',
      label: 'Vehicle',
      render: (v, row) => (
        <span className="entity-cell">
          <EntityAvatar kind="vehicle" type={row.type} size={32} />
          {v}
        </span>
      ),
    },
    { key: 'type', label: 'Type', render: (v) => v?.replace(/_/g, ' ') },
    { key: 'capacity', label: 'Capacity' },
    { key: 'status', label: 'Status', render: (v) => <StatusBadge status={v} /> },
    { key: 'make', label: 'Make' },
    { key: 'model', label: 'Model' },
    { key: 'currentLocation', label: 'Location' },
  ];

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const params = { page, size: pageSize };
      if (filters.type) params.type = filters.type;
      if (filters.status) params.status = filters.status;
      const res = await vehicleService.getAll(params);
      setVehicles(res.data.content);
      setTotalPages(res.data.totalPages);
      setTotalElements(res.data.totalElements);
    } catch (err) {
      toast.error('Failed to load vehicles');
    } finally {
      setLoading(false);
    }
  }, [page, filters]);

  useEffect(() => { loadData(); }, [loadData]);

  useEffect(() => {
    const status = searchParams.get('status') || '';
    setFilters((f) => (f.status === status ? f : { ...f, status }));
    if (status) setShowFilters(true);
  }, [searchParams]);

  const handleCreate = () => { setEditVehicle(null); setModalOpen(true); };
  const handleEdit = (vehicle) => { setEditVehicle(vehicle); setModalOpen(true); };

  const handleSubmit = async (data) => {
    try {
      if (editVehicle) {
        await vehicleService.update(editVehicle.id, data);
        toast.success('Vehicle updated');
      } else {
        await vehicleService.create(data);
        toast.success('Vehicle created');
      }
      setModalOpen(false);
      loadData();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Operation failed');
    }
  };

  const handleDelete = async () => {
    try {
      await vehicleService.delete(deleteTarget.id);
      toast.success('Vehicle deleted');
      setDeleteTarget(null);
      loadData();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Delete failed');
    }
  };

  if (loading) return <div className="page-loader">Loading...</div>;

  const handleFilterChange = (field) => (e) => {
    setFilters({ ...filters, [field]: e.target.value });
    setPage(0);
  };
  const clearFilters = () => { setFilters({ type: '', status: '' }); setPage(0); };
  const hasActiveFilters = filters.type || filters.status;

  return (
    <div>
      <div className="page-header">
        <h2 className="page-title">Fleet</h2>
        <div style={{ display: 'flex', gap: '8px' }}>
          <button
            className={`btn ${showFilters ? 'btn-secondary' : 'btn-outline'}`}
            onClick={() => setShowFilters(!showFilters)}
          >
            <FiFilter /> Filters {hasActiveFilters && <span className="filter-dot" />}
          </button>
          <button className="btn btn-primary" onClick={handleCreate}>
            <FiPlus /> Add Vehicle
          </button>
        </div>
      </div>

      {showFilters && (
        <div className="filter-bar">
          <div className="filter-group">
            <label className="filter-label">Type</label>
            <select className="form-input filter-input" value={filters.type} onChange={handleFilterChange('type')}>
              <option value="">All Types</option>
              {VEHICLE_TYPES.map(t => <option key={t} value={t}>{t.replace(/_/g, ' ')}</option>)}
            </select>
          </div>
          <div className="filter-group">
            <label className="filter-label">Status</label>
            <select className="form-input filter-input" value={filters.status} onChange={handleFilterChange('status')}>
              <option value="">All Statuses</option>
              {VEHICLE_STATUSES.map(s => <option key={s} value={s}>{s}</option>)}
            </select>
          </div>
          {hasActiveFilters && (
            <button className="btn btn-link" onClick={clearFilters} style={{ alignSelf: 'flex-end' }}>Clear</button>
          )}
        </div>
      )}

      <DataTable
        columns={columns}
        data={vehicles}
        onEdit={handleEdit}
        onDelete={(row) => setDeleteTarget(row)}
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
      <Modal isOpen={modalOpen} onClose={() => setModalOpen(false)} title={editVehicle ? 'Edit Vehicle' : 'Add Vehicle'}>
        <VehicleForm vehicle={editVehicle} onSubmit={handleSubmit} onCancel={() => setModalOpen(false)} />
      </Modal>
      <ConfirmDialog
        isOpen={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleDelete}
        title="Delete Vehicle"
        message={`Delete vehicle ${deleteTarget?.vehicleNumber}?`}
      />
    </div>
  );
}

export default VehicleList;

