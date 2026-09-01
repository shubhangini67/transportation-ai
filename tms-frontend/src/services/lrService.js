import api from '../api/axios';

const lrService = {
  getAll: (params = {}) => api.get('/lrs', { params }),
  getById: (id) => api.get(`/lrs/${id}`),
  create: (data) => api.post('/lrs', data),
  update: (id, data) => api.put(`/lrs/${id}`, data),
  delete: (id) => api.delete(`/lrs/${id}`),
  downloadPdf: (id) => api.get(`/lrs/${id}/pdf`, { responseType: 'blob' }),
};

export default lrService;

