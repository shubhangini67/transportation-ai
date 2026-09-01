import React, { useEffect, useState } from 'react';
import { FiPlus } from 'react-icons/fi';
import { toast } from 'react-toastify';
import webhookService from '../../services/webhookService';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import Modal from '../../components/common/Modal';

const EVENT_TYPES = ['TRIP_CREATED', 'TRIP_STATUS_CHANGED', 'TRIP_COMPLETED', 'BOOKING_CREATED', 'BOOKING_CANCELLED', 'LR_CREATED', 'LR_STATUS_CHANGED'];

function WebhookList() {
  const [webhooks, setWebhooks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [form, setForm] = useState({ url: '', eventTypes: '', secret: '', description: '' });

  const loadData = async () => {
    try { const res = await webhookService.getAll(); setWebhooks(res.data || []); }
    catch { toast.error('Failed to load webhooks'); }
    finally { setLoading(false); }
  };
  useEffect(() => { loadData(); }, []);

  const handleCreate = async (e) => {
    e.preventDefault();
    try { await webhookService.create(form); toast.success('Webhook created'); setModalOpen(false); loadData(); }
    catch (err) { toast.error(err.response?.data?.message || 'Failed'); }
  };

  const handleDelete = async () => {
    try { await webhookService.delete(deleteTarget.id); toast.success('Deleted'); setDeleteTarget(null); loadData(); }
    catch { toast.error('Delete failed'); }
  };

  if (loading) return <div className="page-loader">Loading...</div>;

  return (
    <div>
      <div className="page-header">
        <h2 className="page-title">Hooks</h2>
        <button className="btn btn-primary" onClick={() => { setForm({ url: '', eventTypes: '', secret: '', description: '' }); setModalOpen(true); }}><FiPlus /> Add Webhook</button>
      </div>
      <div className="table-container">
        <table className="data-table">
          <thead><tr><th>URL</th><th>Events</th><th>Active</th><th>Actions</th></tr></thead>
          <tbody>
            {webhooks.length === 0 ? <tr><td colSpan={4} className="empty-row">No webhooks</td></tr> :
              webhooks.map(wh => (
                <tr key={wh.id}>
                  <td style={{ fontSize: '0.8rem', wordBreak: 'break-all' }}>{wh.url}</td>
                  <td><span style={{ fontSize: '0.75rem' }}>{wh.eventTypes}</span></td>
                  <td>{wh.active ? '✅' : '❌'}</td>
                  <td><button className="btn-icon btn-delete" onClick={() => setDeleteTarget(wh)}>🗑️</button></td>
                </tr>
              ))}
          </tbody>
        </table>
      </div>
      <Modal isOpen={modalOpen} onClose={() => setModalOpen(false)} title="New Webhook">
        <form onSubmit={handleCreate} className="entity-form">
          <div className="form-grid">
            <div><label className="form-label">URL *</label><input className="form-input" value={form.url} onChange={e => setForm({ ...form, url: e.target.value })} required /></div>
            <div><label className="form-label">Events *</label>
              <select className="form-input" multiple value={form.eventTypes.split(',').filter(Boolean)} onChange={e => setForm({ ...form, eventTypes: Array.from(e.target.selectedOptions, o => o.value).join(',') })} style={{ minHeight: 100 }}>
                {EVENT_TYPES.map(t => <option key={t} value={t}>{t}</option>)}
              </select>
            </div>
            <div><label className="form-label">Secret</label><input className="form-input" value={form.secret} onChange={e => setForm({ ...form, secret: e.target.value })} /></div>
            <div><label className="form-label">Description</label><input className="form-input" value={form.description} onChange={e => setForm({ ...form, description: e.target.value })} /></div>
          </div>
          <div className="form-actions"><button type="button" className="btn btn-secondary" onClick={() => setModalOpen(false)}>Cancel</button><button type="submit" className="btn btn-primary">Create</button></div>
        </form>
      </Modal>
      <ConfirmDialog isOpen={!!deleteTarget} onClose={() => setDeleteTarget(null)} onConfirm={handleDelete} title="Delete Webhook" message="Delete this webhook?" />
    </div>
  );
}

export default WebhookList;

