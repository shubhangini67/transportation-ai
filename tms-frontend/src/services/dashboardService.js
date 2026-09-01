import api from '../api/axios';

const dashboardService = {
  getStats: () => api.get('/dashboard/stats'),
  getMetrics: () => api.get('/dashboard/metrics'),
  getTrends: () => api.get('/dashboard/trends'),
};

export default dashboardService;
