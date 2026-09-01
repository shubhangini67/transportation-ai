import api from '../api/axios';

const auditService = {
  getAll: (params = {}) => api.get('/audit-logs', { params }),
};

export default auditService;

