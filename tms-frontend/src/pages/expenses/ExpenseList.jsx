import React, { useEffect, useState, useCallback } from 'react';
import { FiPlus, FiChevronLeft, FiChevronRight, FiDollarSign } from 'react-icons/fi';
import { toast } from 'react-toastify';
import expenseService from '../../services/expenseService';
import DataTable from '../../components/common/DataTable';
import Modal from '../../components/common/Modal';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import ExpenseForm from './ExpenseForm';

const CATEGORIES = ['FUEL', 'TOLL', 'MAINTENANCE', 'DRIVER_ALLOWANCE', 'OTHER'];

function ExpenseList() {
  const [expenses, setExpenses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editExpense, setEditExpense] = useState(null);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [filters, setFilters] = useState({ category: '', from: '', to: '' });

  const columns = [
    { key: 'expenseDate', label: 'Date', render: v => v || '—' },
    { key: 'category', label: 'Category' },
    { key: 'amount', label: 'Amount', render: v => `₹${Number(v).toLocaleString()}` },
    { key: 'vehicleNumber', label: 'Vehicle', render: v => v || '—' },
    { key: 'description', label: 'Description', render: v => v || '—' },
  ];

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const params = { page, size: 10 };
      if (filters.category) params.category = filters.category;
      if (filters.from) params.from = filters.from;
      if (filters.to) params.to = filters.to;
      const res = await expenseService.getAll(params);
      setExpenses(res.data.content);
      setTotalPages(res.data.totalPages);
    } catch { toast.error('Failed to load expenses'); }
    finally { setLoading(false); }
  }, [page, filters]);

  useEffect(() => { loadData(); }, [loadData]);

  const handleSubmit = async (data) => {
    try {
      if (editExpense) { await expenseService.update(editExpense.id, data); toast.success('Expense updated'); }
      else { await expenseService.create(data); toast.success('Expense created'); }
      setModalOpen(false);
      loadData();
    } catch (err) { toast.error(err.response?.data?.message || 'Failed'); }
  };

  const handleDelete = async () => {
    try { await expenseService.delete(deleteTarget.id); toast.success('Deleted'); setDeleteTarget(null); loadData(); }
    catch (err) { toast.error('Delete failed'); }
  };

  if (loading) return <div className="page-loader">Loading...</div>;

  return (
    <div>
      <div className="page-header">
        <h2 className="page-title"><FiDollarSign /> Costs</h2>
        <button className="btn btn-primary" onClick={() => { setEditExpense(null); setModalOpen(true); }}><FiPlus /> Add Expense</button>
      </div>
      <div style={{ display: 'flex', gap: 12, marginBottom: 16, flexWrap: 'wrap' }}>
        <select className="form-input" style={{ width: 160 }} value={filters.category} onChange={e => { setFilters({ ...filters, category: e.target.value }); setPage(0); }}>
          <option value="">All Categories</option>
          {CATEGORIES.map(c => <option key={c} value={c}>{c.replace(/_/g, ' ')}</option>)}
        </select>
        <input className="form-input" type="date" value={filters.from} onChange={e => { setFilters({ ...filters, from: e.target.value }); setPage(0); }} />
        <input className="form-input" type="date" value={filters.to} onChange={e => { setFilters({ ...filters, to: e.target.value }); setPage(0); }} />
      </div>
      <DataTable columns={columns} data={expenses} onEdit={e => { setEditExpense(e); setModalOpen(true); }} onDelete={e => setDeleteTarget(e)} />
      {totalPages > 1 && (
        <div className="pagination">
          <button disabled={page === 0} onClick={() => setPage(p => p - 1)}><FiChevronLeft /></button>
          <span>Page {page + 1} of {totalPages}</span>
          <button disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)}><FiChevronRight /></button>
        </div>
      )}
      <Modal isOpen={modalOpen} onClose={() => setModalOpen(false)} title={editExpense ? 'Edit Expense' : 'Add Expense'}>
        <ExpenseForm expense={editExpense} onSubmit={handleSubmit} onCancel={() => setModalOpen(false)} />
      </Modal>
      <ConfirmDialog isOpen={!!deleteTarget} onClose={() => setDeleteTarget(null)} onConfirm={handleDelete} title="Delete Expense" message="Delete this expense?" />
    </div>
  );
}

export default ExpenseList;

