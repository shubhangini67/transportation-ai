import api from '../api/axios';

const expenseService = {
  getAll: (params = {}) => api.get('/expenses', { params }),
  getSummary: (params = {}) => api.get('/expenses/summary', { params }),
  create: (data) => api.post('/expenses', data),
  update: (id, data) => api.put(`/expenses/${id}`, data),
  delete: (id) => api.delete(`/expenses/${id}`),
};

export default expenseService;

