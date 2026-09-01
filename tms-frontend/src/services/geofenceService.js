import api from '../api/axios';

const geofenceService = {
  getAll: (params = {}) => api.get('/geofences', { params }),
  getById: (id) => api.get(`/geofences/${id}`),
  create: (data) => api.post('/geofences', data),
  update: (id, data) => api.put(`/geofences/${id}`, data),
  delete: (id) => api.delete(`/geofences/${id}`),
  getEvents: (id, params = {}) => api.get(`/geofences/${id}/events`, { params }),
  getRecentEvents: (params = {}) => api.get('/geofences/events/recent', { params }),
  getVehicleEvents: (vehicleId, params = {}) => api.get(`/geofences/events/vehicle/${vehicleId}`, { params }),
};

export default geofenceService;

