import api from '../api/axios';

const driverService = {
  getAll: (params = {}) => api.get('/drivers', { params }),
  getById: (id) => api.get(`/drivers/${id}`),
  getActive: () => api.get('/drivers/active'),
  create: (data) => api.post('/drivers', data),
  update: (id, data) => api.put(`/drivers/${id}`, data),
  delete: (id) => api.delete(`/drivers/${id}`),
};

export default driverService;
