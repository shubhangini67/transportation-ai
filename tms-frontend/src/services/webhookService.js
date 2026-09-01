import api from '../api/axios';

const webhookService = {
  getAll: () => api.get('/webhooks'),
  create: (data) => api.post('/webhooks', data),
  delete: (id) => api.delete(`/webhooks/${id}`),
};

export default webhookService;

