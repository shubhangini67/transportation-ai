import React, { useEffect, useState, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import { FiPlus, FiFilter, FiChevronLeft, FiChevronRight } from 'react-icons/fi';
import { toast } from 'react-toastify';
import driverService from '../../services/driverService';
import DataTable from '../../components/common/DataTable';
import Modal from '../../components/common/Modal';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import StatusBadge from '../../components/common/StatusBadge';
import EntityAvatar from '../../components/common/EntityAvatar';
import DriverForm from './DriverForm';

const DRIVER_STATUSES = ['ACTIVE', 'INACTIVE'];

function DriverList() {
  const [searchParams] = useSearchParams();
  const [drivers, setDrivers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editDriver, setEditDriver] = useState(null);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const pageSize = 10;

  const [filters, setFilters] = useState({ status: searchParams.get('status') || '', name: '' });
  const [showFilters, setShowFilters] = useState(!!searchParams.get('status'));

  const columns = [
    {
      key: 'name',
      label: 'Name',
      render: (v) => (
        <span className="entity-cell">
          <EntityAvatar name={v} size={32} />
          {v}
        </span>
      ),
    },
    { key: 'phone', label: 'Phone' },
    { key: 'licenseNumber', label: 'License Number' },
    { key: 'email', label: 'Email' },
    { key: 'status', label: 'Status', render: (v) => <StatusBadge status={v} /> },
  ];

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const params = { page, size: pageSize };
      if (filters.status) params.status = filters.status;
      if (filters.name.trim()) params.name = filters.name.trim();
      const res = await driverService.getAll(params);
      setDrivers(res.data.content);
      setTotalPages(res.data.totalPages);
      setTotalElements(res.data.totalElements);
    } catch (err) {
      toast.error('Failed to load drivers');
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

  const handleCreate = () => { setEditDriver(null); setModalOpen(true); };
  const handleEdit = (driver) => { setEditDriver(driver); setModalOpen(true); };

  const handleSubmit = async (data) => {
    try {
      if (editDriver) {
        await driverService.update(editDriver.id, data);
        toast.success('Driver updated');
      } else {
        await driverService.create(data);
        toast.success('Driver created');
      }
      setModalOpen(false);
      loadData();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Operation failed');
    }
  };

  const handleDelete = async () => {
    try {
      await driverService.delete(deleteTarget.id);
      toast.success('Driver deleted');
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
  const clearFilters = () => { setFilters({ status: '', name: '' }); setPage(0); };
  const hasActiveFilters = filters.status || filters.name;

  return (
    <div>
      <div className="page-header">
        <h2 className="page-title">Crew</h2>
        <div style={{ display: 'flex', gap: '8px' }}>
          <button
            className={`btn ${showFilters ? 'btn-secondary' : 'btn-outline'}`}
            onClick={() => setShowFilters(!showFilters)}
          >
            <FiFilter /> Filters {hasActiveFilters && <span className="filter-dot" />}
          </button>
          <button className="btn btn-primary" onClick={handleCreate}><FiPlus /> Add Driver</button>
        </div>
      </div>

      {showFilters && (
        <div className="filter-bar">
          <div className="filter-group">
            <label className="filter-label">Status</label>
            <select className="form-input filter-input" value={filters.status} onChange={handleFilterChange('status')}>
              <option value="">All Statuses</option>
              {DRIVER_STATUSES.map(s => <option key={s} value={s}>{s}</option>)}
            </select>
          </div>
          <div className="filter-group">
            <label className="filter-label">Name</label>
            <input className="form-input filter-input" placeholder="Search name..." value={filters.name} onChange={handleFilterChange('name')} />
          </div>
          {hasActiveFilters && (
            <button className="btn btn-link" onClick={clearFilters} style={{ alignSelf: 'flex-end' }}>Clear</button>
          )}
        </div>
      )}

      <DataTable columns={columns} data={drivers} onEdit={handleEdit} onDelete={(row) => setDeleteTarget(row)} />
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
      <Modal isOpen={modalOpen} onClose={() => setModalOpen(false)} title={editDriver ? 'Edit Driver' : 'Add Driver'}>
        <DriverForm driver={editDriver} onSubmit={handleSubmit} onCancel={() => setModalOpen(false)} />
      </Modal>
      <ConfirmDialog
        isOpen={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleDelete}
        title="Delete Driver"
        message={`Delete driver ${deleteTarget?.name}?`}
      />
    </div>
  );
}

export default DriverList;

