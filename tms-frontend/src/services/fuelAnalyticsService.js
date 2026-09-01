import api from '../api/axios';

const fuelAnalyticsService = {
  getAnalytics: (params = {}) => api.get('/analytics/fuel', { params }),
  getVehicleDetail: (vehicleId, params = {}) => api.get(`/analytics/fuel/vehicle/${vehicleId}`, { params }),
};

export default fuelAnalyticsService;

