import api from '../api/axios';

const invoiceService = {
  getAll: (params) => api.get('/invoices', { params }),
  getById: (id) => api.get(`/invoices/${id}`),
  create: (data) => api.post('/invoices', data),
  generateFromTrip: (tripId) => api.post(`/invoices/generate/${tripId}`),
  updateStatus: (id, status) => api.patch(`/invoices/${id}/status`, { status }),
  downloadPdf: (id) => api.get(`/invoices/${id}/pdf`, { responseType: 'blob' }),
  delete: (id) => api.delete(`/invoices/${id}`),
};

export default invoiceService;

