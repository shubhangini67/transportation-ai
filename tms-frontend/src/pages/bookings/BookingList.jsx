import React, { useEffect, useState, useCallback } from 'react';
import { FiPlus, FiXCircle, FiFilter, FiChevronLeft, FiChevronRight } from 'react-icons/fi';
import { toast } from 'react-toastify';
import bookingService from '../../services/bookingService';
import DataTable from '../../components/common/DataTable';
import Modal from '../../components/common/Modal';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import StatusBadge from '../../components/common/StatusBadge';
import BookingForm from './BookingForm';

const BOOKING_STATUSES = ['CONFIRMED', 'CANCELLED', 'COMPLETED'];

function BookingList() {
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editBooking, setEditBooking] = useState(null);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const pageSize = 10;

  const [filters, setFilters] = useState({ status: '', customerName: '' });
  const [showFilters, setShowFilters] = useState(false);

  const columns = [
    { key: 'customerName', label: 'Customer' },
    { key: 'customerPhone', label: 'Phone' },
    { key: 'tripRoute', label: 'Trip Route' },
    { key: 'seatCount', label: 'Seats' },
    { key: 'status', label: 'Status', render: (v) => <StatusBadge status={v} /> },
    { key: 'createdAt', label: 'Booked', render: (v) => v ? new Date(v).toLocaleDateString() : '—' },
  ];

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const params = { page, size: pageSize };
      if (filters.status) params.status = filters.status;
      if (filters.customerName.trim()) params.customerName = filters.customerName.trim();
      const res = await bookingService.getAll(params);
      setBookings(res.data.content);
      setTotalPages(res.data.totalPages);
      setTotalElements(res.data.totalElements);
    } catch (err) {
      toast.error('Failed to load bookings');
    } finally {
      setLoading(false);
    }
  }, [page, filters]);

  useEffect(() => { loadData(); }, [loadData]);

  const handleCreate = () => { setEditBooking(null); setModalOpen(true); };
  const handleEdit = (booking) => { setEditBooking(booking); setModalOpen(true); };

  const handleSubmit = async (data) => {
    try {
      if (editBooking) {
        await bookingService.update(editBooking.id, data);
        toast.success('Booking updated');
      } else {
        await bookingService.create(data);
        toast.success('Booking created');
      }
      setModalOpen(false);
      loadData();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Operation failed');
    }
  };

  const handleCancel = async (bookingId) => {
    try {
      await bookingService.cancel(bookingId);
      toast.success('Booking cancelled');
      loadData();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Cancel failed');
    }
  };

  const handleDelete = async () => {
    try {
      await bookingService.delete(deleteTarget.id);
      toast.success('Booking deleted');
      setDeleteTarget(null);
      loadData();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Delete failed');
    }
  };

  const bookingActions = (row) => (
    <>
      {row.status === 'CONFIRMED' && (
        <button className="btn-icon btn-cancel" onClick={() => handleCancel(row.id)} title="Cancel Booking">
          <FiXCircle />
        </button>
      )}
    </>
  );

  if (loading) return <div className="page-loader">Loading...</div>;

  const handleFilterChange = (field) => (e) => {
    setFilters({ ...filters, [field]: e.target.value });
    setPage(0);
  };
  const clearFilters = () => { setFilters({ status: '', customerName: '' }); setPage(0); };
  const hasActiveFilters = filters.status || filters.customerName;

  return (
    <div>
      <div className="page-header">
        <h2 className="page-title">Bookings</h2>
        <div style={{ display: 'flex', gap: '8px' }}>
          <button
            className={`btn ${showFilters ? 'btn-secondary' : 'btn-outline'}`}
            onClick={() => setShowFilters(!showFilters)}
          >
            <FiFilter /> Filters {hasActiveFilters && <span className="filter-dot" />}
          </button>
          <button className="btn btn-primary" onClick={handleCreate}><FiPlus /> Add Booking</button>
        </div>
      </div>

      {showFilters && (
        <div className="filter-bar">
          <div className="filter-group">
            <label className="filter-label">Status</label>
            <select className="form-input filter-input" value={filters.status} onChange={handleFilterChange('status')}>
              <option value="">All Statuses</option>
              {BOOKING_STATUSES.map(s => <option key={s} value={s}>{s}</option>)}
            </select>
          </div>
          <div className="filter-group">
            <label className="filter-label">Customer</label>
            <input className="form-input filter-input" placeholder="Search customer..." value={filters.customerName} onChange={handleFilterChange('customerName')} />
          </div>
          {hasActiveFilters && (
            <button className="btn btn-link" onClick={clearFilters} style={{ alignSelf: 'flex-end' }}>Clear</button>
          )}
        </div>
      )}

      <DataTable
        columns={columns}
        data={bookings}
        onEdit={handleEdit}
        onDelete={(row) => setDeleteTarget(row)}
        actions={bookingActions}
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
      <Modal isOpen={modalOpen} onClose={() => setModalOpen(false)} title={editBooking ? 'Edit Booking' : 'New Booking'}>
        <BookingForm booking={editBooking} onSubmit={handleSubmit} onCancel={() => setModalOpen(false)} />
      </Modal>
      <ConfirmDialog
        isOpen={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleDelete}
        title="Delete Booking"
        message={`Delete booking for ${deleteTarget?.customerName}?`}
      />
    </div>
  );
}

export default BookingList;

