import api from '../api/axios';

const tripService = {
  getAll: (params = {}) => api.get('/trips', { params }),
  getById: (id) => api.get(`/trips/${id}`),
  getByStatus: (status) => api.get(`/trips/status/${status}`),
  getRecent: () => api.get('/trips/recent'),
  create: (data) => api.post('/trips', data),
  update: (id, data) => api.put(`/trips/${id}`, data),
  updateStatus: (id, status) => api.patch(`/trips/${id}/status`, { status }),
  delete: (id) => api.delete(`/trips/${id}`),
  getTracking: (id) => api.get(`/trips/${id}/tracking`),
};

export default tripService;
