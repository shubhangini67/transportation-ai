import React, { useEffect, useState, useCallback } from 'react';
import { FiUpload, FiDownload, FiTrash2, FiFile, FiImage, FiFileText } from 'react-icons/fi';
import { toast } from 'react-toastify';
import attachmentService from '../../services/attachmentService';
import ConfirmDialog from './ConfirmDialog';
import '../../styles/components.css';

function fileIcon(type) {
  if (type?.startsWith('image/')) return <FiImage />;
  if (type?.includes('pdf')) return <FiFileText />;
  return <FiFile />;
}

function formatSize(bytes) {
  if (!bytes) return '0 B';
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB';
  return (bytes / 1048576).toFixed(1) + ' MB';
}

function AttachmentSection({ entityType, entityId }) {
  const [attachments, setAttachments] = useState([]);
  const [uploading, setUploading] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState(null);

  const loadAttachments = useCallback(async () => {
    if (!entityId) return;
    try {
      const res = await attachmentService.listByEntity(entityType, entityId);
      setAttachments(res.data || []);
    } catch { /* ignore */ }
  }, [entityType, entityId]);

  useEffect(() => { loadAttachments(); }, [loadAttachments]);

  const handleUpload = async (e) => {
    const file = e.target.files[0];
    if (!file) return;
    setUploading(true);
    try {
      await attachmentService.upload(file, entityType, entityId);
      toast.success('File uploaded');
      loadAttachments();
    } catch { toast.error('Upload failed'); }
    finally { setUploading(false); e.target.value = ''; }
  };

  const handleDownload = async (att) => {
    try {
      const res = await attachmentService.download(att.id);
      const url = window.URL.createObjectURL(new Blob([res.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', att.fileName);
      document.body.appendChild(link);
      link.click();
      link.remove();
    } catch { toast.error('Download failed'); }
  };

  const handleDelete = async () => {
    try {
      await attachmentService.delete(deleteTarget.id);
      toast.success('File deleted');
      setDeleteTarget(null);
      loadAttachments();
    } catch { toast.error('Delete failed'); }
  };

  if (!entityId) return null;

  return (
    <div className="attachment-section">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
        <h4 style={{ margin: 0 }}>📎 Attachments ({attachments.length})</h4>
        <label className="btn btn-secondary" style={{ cursor: 'pointer', margin: 0 }}>
          <FiUpload /> {uploading ? 'Uploading...' : 'Upload'}
          <input type="file" style={{ display: 'none' }} onChange={handleUpload} disabled={uploading} />
        </label>
      </div>
      {attachments.length === 0 ? (
        <div style={{ color: 'var(--text-secondary)', padding: 16, textAlign: 'center' }}>No attachments yet</div>
      ) : (
        <div className="attachment-list">
          {attachments.map(att => (
            <div key={att.id} className="attachment-item">
              <span className="attachment-icon">{fileIcon(att.fileType)}</span>
              <div className="attachment-info">
                <div className="attachment-name">{att.fileName}</div>
                <div className="attachment-meta">{formatSize(att.fileSize)} · {att.createdBy} · {new Date(att.createdAt).toLocaleDateString()}</div>
              </div>
              <div className="attachment-actions">
                <button className="btn-icon" title="Download" onClick={() => handleDownload(att)}><FiDownload /></button>
                <button className="btn-icon" title="Delete" onClick={() => setDeleteTarget(att)}><FiTrash2 /></button>
              </div>
            </div>
          ))}
        </div>
      )}
      <ConfirmDialog isOpen={!!deleteTarget} onClose={() => setDeleteTarget(null)} onConfirm={handleDelete} title="Delete Attachment" message={`Delete "${deleteTarget?.fileName}"?`} />
    </div>
  );
}

export default AttachmentSection;

