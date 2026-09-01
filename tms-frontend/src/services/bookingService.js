import api from '../api/axios';

const bookingService = {
  getAll: (params = {}) => api.get('/bookings', { params }),
  getById: (id) => api.get(`/bookings/${id}`),
  getByTrip: (tripId) => api.get(`/bookings/trip/${tripId}`),
  create: (data) => api.post('/bookings', data),
  update: (id, data) => api.put(`/bookings/${id}`, data),
  cancel: (id) => api.patch(`/bookings/${id}/cancel`),
  delete: (id) => api.delete(`/bookings/${id}`),
};

export default bookingService;
