import api from '../api/axios';

const routeService = {
  getAll: (params = {}) => api.get('/routes', { params }),
  getById: (id) => api.get(`/routes/${id}`),
  getActive: () => api.get('/routes/active'),
  search: (query) => api.get(`/routes/search`, { params: { query } }),
  create: (data) => api.post('/routes', data),
  update: (id, data) => api.put(`/routes/${id}`, data),
  delete: (id) => api.delete(`/routes/${id}`),
};

export default routeService;
