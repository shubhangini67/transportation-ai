import api from '../api/axios';

const vehicleService = {
  getAll: (params = {}) => api.get('/vehicles', { params }),
  getById: (id) => api.get(`/vehicles/${id}`),
  getAvailable: () => api.get('/vehicles/available'),
  getFleet: () => api.get('/vehicles/fleet'),
  create: (data) => api.post('/vehicles', data),
  update: (id, data) => api.put(`/vehicles/${id}`, data),
  delete: (id) => api.delete(`/vehicles/${id}`),
};

export default vehicleService;
