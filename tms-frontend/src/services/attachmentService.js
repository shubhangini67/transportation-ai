import api from '../api/axios';

const attachmentService = {
  upload: (file, entityType, entityId) => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('entityType', entityType);
    formData.append('entityId', entityId);
    return api.post('/attachments/upload', formData, { headers: { 'Content-Type': 'multipart/form-data' } });
  },
  listByEntity: (entityType, entityId) => api.get('/attachments', { params: { entityType, entityId } }),
  download: (id) => api.get(`/attachments/${id}/download`, { responseType: 'blob' }),
  delete: (id) => api.delete(`/attachments/${id}`),
};

export default attachmentService;

