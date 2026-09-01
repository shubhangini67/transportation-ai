import React, { useEffect, useState, useCallback } from 'react';
import { FiPlus, FiChevronLeft, FiChevronRight, FiFileText, FiDownload, FiZap } from 'react-icons/fi';
import { toast } from 'react-toastify';
import invoiceService from '../../services/invoiceService';
import DataTable from '../../components/common/DataTable';
import Modal from '../../components/common/Modal';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import StatusBadge from '../../components/common/StatusBadge';
import InvoiceForm from './InvoiceForm';

const STATUSES = ['DRAFT', 'SENT', 'PAID', 'CANCELLED'];

function InvoiceList() {
  const [invoices, setInvoices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [generateModalOpen, setGenerateModalOpen] = useState(false);
  const [tripIdInput, setTripIdInput] = useState('');
  const [editInvoice, setEditInvoice] = useState(null);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [statusFilter, setStatusFilter] = useState('');

  const columns = [
    { key: 'invoiceNumber', label: 'Invoice #' },
    { key: 'clientName', label: 'Client' },
    { key: 'tripVehicleNumber', label: 'Vehicle', render: v => v || '—' },
    { key: 'issuedDate', label: 'Issued', render: v => v || '—' },
    { key: 'dueDate', label: 'Due', render: v => v || '—' },
    { key: 'totalAmount', label: 'Total', render: v => `₹${Number(v).toLocaleString()}` },
    { key: 'status', label: 'Status', render: v => <StatusBadge status={v} /> },
  ];

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const params = { page, size: 10 };
      if (statusFilter) params.status = statusFilter;
      const res = await invoiceService.getAll(params);
      setInvoices(res.data.content);
      setTotalPages(res.data.totalPages);
    } catch { toast.error('Failed to load invoices'); }
    finally { setLoading(false); }
  }, [page, statusFilter]);

  useEffect(() => { loadData(); }, [loadData]);

  const handleSubmit = async (data) => {
    try {
      await invoiceService.create(data);
      toast.success('Invoice created');
      setModalOpen(false);
      loadData();
    } catch (err) { toast.error(err.response?.data?.message || 'Failed'); }
  };

  const handleGenerate = async () => {
    if (!tripIdInput.trim()) return;
    try {
      await invoiceService.generateFromTrip(tripIdInput.trim());
      toast.success('Invoice generated from trip expenses');
      setGenerateModalOpen(false);
      setTripIdInput('');
      loadData();
    } catch (err) { toast.error(err.response?.data?.message || 'Generation failed'); }
  };

  const handleDownloadPdf = async (invoice) => {
    try {
      const res = await invoiceService.downloadPdf(invoice.id);
      const url = window.URL.createObjectURL(new Blob([res.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `${invoice.invoiceNumber}.pdf`);
      document.body.appendChild(link);
      link.click();
      link.remove();
    } catch { toast.error('PDF download failed'); }
  };

  const handleDelete = async () => {
    try {
      await invoiceService.delete(deleteTarget.id);
      toast.success('Invoice deleted');
      setDeleteTarget(null);
      loadData();
    } catch { toast.error('Delete failed'); }
  };

  const handleStatusChange = async (invoice, status) => {
    try {
      await invoiceService.updateStatus(invoice.id, status);
      toast.success(`Invoice marked as ${status}`);
      loadData();
    } catch { toast.error('Status update failed'); }
  };

  if (loading) return <div className="page-loader">Loading...</div>;

  return (
    <div>
      <div className="page-header">
        <h2 className="page-title"><FiFileText /> Billing</h2>
        <div style={{ display: 'flex', gap: 8 }}>
          <button className="btn btn-secondary" onClick={() => setGenerateModalOpen(true)}><FiZap /> Generate from Trip</button>
          <button className="btn btn-primary" onClick={() => { setEditInvoice(null); setModalOpen(true); }}><FiPlus /> Create Invoice</button>
        </div>
      </div>
      <div style={{ display: 'flex', gap: 12, marginBottom: 16 }}>
        <select className="form-input" style={{ width: 160 }} value={statusFilter} onChange={e => { setStatusFilter(e.target.value); setPage(0); }}>
          <option value="">All Statuses</option>
          {STATUSES.map(s => <option key={s} value={s}>{s}</option>)}
        </select>
      </div>
      <DataTable columns={columns} data={invoices}
        onEdit={inv => { setEditInvoice(inv); setModalOpen(true); }}
        onDelete={inv => setDeleteTarget(inv)}
        actions={(inv) => (
          <>
            <button className="btn-icon" title="Download PDF" onClick={() => handleDownloadPdf(inv)}><FiDownload /></button>
            {inv.status === 'DRAFT' && <button className="btn-icon" title="Mark Sent" onClick={() => handleStatusChange(inv, 'SENT')}>📤</button>}
            {inv.status === 'SENT' && <button className="btn-icon" title="Mark Paid" onClick={() => handleStatusChange(inv, 'PAID')}>💰</button>}
          </>
        )}
      />
      {totalPages > 1 && (
        <div className="pagination">
          <button disabled={page === 0} onClick={() => setPage(p => p - 1)}><FiChevronLeft /></button>
          <span>Page {page + 1} of {totalPages}</span>
          <button disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)}><FiChevronRight /></button>
        </div>
      )}
      <Modal isOpen={modalOpen} onClose={() => setModalOpen(false)} title={editInvoice ? 'Edit Invoice' : 'Create Invoice'}>
        <InvoiceForm invoice={editInvoice} onSubmit={handleSubmit} onCancel={() => setModalOpen(false)} />
      </Modal>
      <Modal isOpen={generateModalOpen} onClose={() => setGenerateModalOpen(false)} title="Generate Invoice from Trip">
        <div>
          <p style={{ marginBottom: 12 }}>Enter the Trip ID to auto-generate an invoice from its expenses:</p>
          <input className="form-input" placeholder="Trip UUID" value={tripIdInput} onChange={e => setTripIdInput(e.target.value)} />
          <div className="form-actions" style={{ marginTop: 16 }}>
            <button className="btn btn-secondary" onClick={() => setGenerateModalOpen(false)}>Cancel</button>
            <button className="btn btn-primary" onClick={handleGenerate}>Generate</button>
          </div>
        </div>
      </Modal>
      <ConfirmDialog isOpen={!!deleteTarget} onClose={() => setDeleteTarget(null)} onConfirm={handleDelete} title="Delete Invoice" message="Delete this invoice?" />
    </div>
  );
}

export default InvoiceList;


