import React, { useEffect, useState, useCallback } from 'react';
import { FiChevronLeft, FiChevronRight } from 'react-icons/fi';
import { toast } from 'react-toastify';
import auditService from '../../services/auditService';

const ENTITY_TYPES = ['', 'Vehicle', 'Driver', 'Trip', 'Booking', 'LorryReceipt', 'Route'];

function AuditLogList() {
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [filters, setFilters] = useState({ entityType: '', entityId: '', changedBy: '' });

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const params = { page, size: 20 };
      if (filters.entityType) params.entityType = filters.entityType;
      if (filters.entityId) params.entityId = filters.entityId;
      if (filters.changedBy) params.changedBy = filters.changedBy;
      const res = await auditService.getAll(params);
      setLogs(res.data.content);
      setTotalPages(res.data.totalPages);
    } catch { toast.error('Failed to load audit logs'); }
    finally { setLoading(false); }
  }, [page, filters]);

  useEffect(() => { loadData(); }, [loadData]);

  return (
    <div>
      <div className="page-header"><h2 className="page-title">Activity</h2></div>
      <div className="filter-bar" style={{ marginBottom: 16, display: 'flex', gap: 12, flexWrap: 'wrap' }}>
        <select className="form-input" style={{ width: 150 }} value={filters.entityType} onChange={e => { setFilters({ ...filters, entityType: e.target.value }); setPage(0); }}>
          <option value="">All Entities</option>
          {ENTITY_TYPES.filter(Boolean).map(t => <option key={t} value={t}>{t}</option>)}
        </select>
        <input className="form-input" style={{ width: 200 }} placeholder="Entity ID..." value={filters.entityId} onChange={e => { setFilters({ ...filters, entityId: e.target.value }); setPage(0); }} />
        <input className="form-input" style={{ width: 150 }} placeholder="Changed by..." value={filters.changedBy} onChange={e => { setFilters({ ...filters, changedBy: e.target.value }); setPage(0); }} />
      </div>
      {loading ? <div className="page-loader">Loading...</div> : (
        <div className="table-container">
          <table className="data-table">
            <thead><tr><th>Timestamp</th><th>Entity</th><th>ID</th><th>Action</th><th>Changed By</th></tr></thead>
            <tbody>
              {logs.length === 0 ? <tr><td colSpan={5} className="empty-row">No audit logs</td></tr> :
                logs.map(l => (
                  <tr key={l.id}>
                    <td>{new Date(l.timestamp).toLocaleString()}</td>
                    <td>{l.entityType}</td>
                    <td style={{ fontSize: '0.75rem', fontFamily: 'monospace' }}>{l.entityId?.substring(0, 8)}...</td>
                    <td><span className={`status-badge status-${l.action?.toLowerCase()}`}>{l.action}</span></td>
                    <td>{l.changedBy}</td>
                  </tr>
                ))}
            </tbody>
          </table>
          {totalPages > 1 && (
            <div className="pagination">
              <button disabled={page === 0} onClick={() => setPage(p => p - 1)}><FiChevronLeft /></button>
              <span>Page {page + 1} of {totalPages}</span>
              <button disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)}><FiChevronRight /></button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

export default AuditLogList;

