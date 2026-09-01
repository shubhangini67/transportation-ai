import api from '../api/axios';

export const maintenanceService = { getAlerts: () => api.get('/maintenance/alerts') };
export const performanceService = { getDrivers: () => api.get('/performance/drivers') };
export const rateService = {
  list: () => api.get('/rates'),
  quote: (routeId, vehicleType) => api.get('/rates/quote', { params: { routeId, vehicleType } }),
};
export const podService = {
  list: (tripId) => api.get(`/trips/${tripId}/pod`),
  submit: (tripId, data) => api.post(`/trips/${tripId}/pod`, data),
};
export const publicTrackService = {
  get: (token) => api.get(`/public/track/${token}`),
};
