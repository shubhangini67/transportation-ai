import React, { useEffect, useState, useCallback } from 'react';
import { FiPlus, FiFilter, FiChevronLeft, FiChevronRight } from 'react-icons/fi';
import { toast } from 'react-toastify';
import lrService from '../../services/lrService';
import Modal from '../../components/common/Modal';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import StatusBadge from '../../components/common/StatusBadge';
import LrForm from './LrForm';
import { FiEdit2, FiTrash2, FiDownload } from 'react-icons/fi';
import '../../styles/components.css';

const LR_STATUSES = ['', 'CREATED', 'IN_TRANSIT', 'DELIVERED'];

function LrList() {
  const [lrs, setLrs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editLr, setEditLr] = useState(null);
  const [deleteTarget, setDeleteTarget] = useState(null);

  // Pagination
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const pageSize = 10;

  // Filters
  const [filters, setFilters] = useState({ status: '', origin: '', destination: '' });
  const [showFilters, setShowFilters] = useState(false);

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const params = { page, size: pageSize, sortBy: 'createdAt', sortDir: 'desc' };
      if (filters.status) params.status = filters.status;
      if (filters.origin.trim()) params.origin = filters.origin.trim();
      if (filters.destination.trim()) params.destination = filters.destination.trim();

      const res = await lrService.getAll(params);
      setLrs(res.data.content);
      setTotalPages(res.data.totalPages);
      setTotalElements(res.data.totalElements);
    } catch (err) {
      toast.error('Failed to load lorry receipts');
    } finally {
      setLoading(false);
    }
  }, [page, filters]);

  useEffect(() => { loadData(); }, [loadData]);

  const handleCreate = () => { setEditLr(null); setModalOpen(true); };
  const handleEdit = (lr) => { setEditLr(lr); setModalOpen(true); };

  const handleSubmit = async (data) => {
    try {
      if (editLr) {
        await lrService.update(editLr.id, data);
        toast.success('LR updated successfully');
      } else {
        await lrService.create(data);
        toast.success('LR created successfully');
      }
      setModalOpen(false);
      loadData();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Operation failed');
    }
  };

  const handleDelete = async () => {
    try {
      await lrService.delete(deleteTarget.id);
      toast.success('LR deleted successfully');
      setDeleteTarget(null);
      loadData();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Delete failed');
    }
  };

  const handleFilterChange = (field) => (e) => {
    setFilters({ ...filters, [field]: e.target.value });
    setPage(0);
  };

  const handleDownloadPdf = async (lr) => {
    try {
      const res = await lrService.downloadPdf(lr.id);
      const url = window.URL.createObjectURL(new Blob([res.data], { type: 'application/pdf' }));
      const link = document.createElement('a');
      link.href = url;
      link.download = `LR-${lr.lrNumber}.pdf`;
      link.click();
      window.URL.revokeObjectURL(url);
    } catch {
      toast.error('Failed to download PDF');
    }
  };

  const clearFilters = () => {
    setFilters({ status: '', origin: '', destination: '' });
    setPage(0);
  };

  const hasActiveFilters = filters.status || filters.origin || filters.destination;

  return (
    <div>
      <div className="page-header">
        <h2 className="page-title">Waybills</h2>
        <div style={{ display: 'flex', gap: '8px' }}>
          <button
            className={`btn ${showFilters ? 'btn-secondary' : 'btn-outline'}`}
            onClick={() => setShowFilters(!showFilters)}
          >
            <FiFilter /> Filters {hasActiveFilters && <span className="filter-dot" />}
          </button>
          <button className="btn btn-primary" onClick={handleCreate}>
            <FiPlus /> Add LR
          </button>
        </div>
      </div>

      {showFilters && (
        <div className="filter-bar">
          <div className="filter-group">
            <label className="filter-label">Status</label>
            <select className="form-input filter-input" value={filters.status} onChange={handleFilterChange('status')}>
              <option value="">All Statuses</option>
              {LR_STATUSES.filter(Boolean).map(s => (
                <option key={s} value={s}>{s.replace(/_/g, ' ')}</option>
              ))}
            </select>
          </div>
          <div className="filter-group">
            <label className="filter-label">Origin</label>
            <input
              className="form-input filter-input"
              placeholder="Search origin..."
              value={filters.origin}
              onChange={handleFilterChange('origin')}
            />
          </div>
          <div className="filter-group">
            <label className="filter-label">Destination</label>
            <input
              className="form-input filter-input"
              placeholder="Search destination..."
              value={filters.destination}
              onChange={handleFilterChange('destination')}
            />
          </div>
          {hasActiveFilters && (
            <button className="btn btn-link" onClick={clearFilters} style={{ alignSelf: 'flex-end' }}>
              Clear
            </button>
          )}
        </div>
      )}

      {loading ? (
        <div className="page-loader">Loading...</div>
      ) : (
        <div className="table-container">
          <table className="data-table">
            <thead>
              <tr>
                <th>LR Number</th>
                <th>Consignor</th>
                <th>Consignee</th>
                <th>Origin</th>
                <th>Destination</th>
                <th>Weight (kg)</th>
                <th>Qty</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {lrs.length === 0 ? (
                <tr><td colSpan={9} className="empty-row">No lorry receipts found</td></tr>
              ) : (
                lrs.map((lr) => (
                  <tr key={lr.id}>
                    <td><strong>{lr.lrNumber}</strong></td>
                    <td>{lr.consignor}</td>
                    <td>{lr.consignee}</td>
                    <td>{lr.origin}</td>
                    <td>{lr.destination}</td>
                    <td>{lr.weight}</td>
                    <td>{lr.quantity}</td>
                    <td><StatusBadge status={lr.status} /></td>
                    <td className="actions-cell">
                      <button className="btn-icon btn-edit" onClick={() => handleDownloadPdf(lr)} title="Download PDF">
                        <FiDownload />
                      </button>
                      <button className="btn-icon btn-edit" onClick={() => handleEdit(lr)} title="Edit">
                        <FiEdit2 />
                      </button>
                      <button className="btn-icon btn-delete" onClick={() => setDeleteTarget(lr)} title="Delete">
                        <FiTrash2 />
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>

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
        </div>
      )}

      <Modal isOpen={modalOpen} onClose={() => setModalOpen(false)} title={editLr ? 'Edit Lorry Receipt' : 'Create Lorry Receipt'}>
        <LrForm lr={editLr} onSubmit={handleSubmit} onCancel={() => setModalOpen(false)} />
      </Modal>

      <ConfirmDialog
        isOpen={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleDelete}
        title="Delete Lorry Receipt"
        message={`Delete LR "${deleteTarget?.lrNumber}"? This action cannot be undone.`}
      />
    </div>
  );
}

export default LrList;

