import api from '../api/axios';

const reportService = {
  getTripReport: (from, to) => api.get('/reports/trips', { params: { from, to } }),
  getTripReportCsv: (from, to) => api.get('/reports/trips/csv', { params: { from, to }, responseType: 'blob' }),
  getVehicleReport: () => api.get('/reports/vehicles'),
  getDriverReport: () => api.get('/reports/drivers'),
  getFleetInsights: () => api.get('/reports/insights'),
};

export default reportService;

