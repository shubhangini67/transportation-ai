import api from '../api/axios';

const locationService = {
  getRouteHistory: (tripId) => api.get(`/trips/${tripId}/route-history`),
  getVehicleLocationHistory: (vehicleId, from, to) => api.get(`/vehicles/${vehicleId}/location-history`, { params: { from, to } }),
  submitLocation: (vehicleId, data) => api.post(`/vehicles/${vehicleId}/location`, data),
};

export default locationService;

