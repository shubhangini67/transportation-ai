import api from '../api/axios';

const aiService = {
  status: () => api.get('/ai/status'),
  briefing: () => api.get('/ai/briefing'),
  ask: (message, extras = {}) => api.post('/ai/ask', {
    message,
    pagePath: extras.pagePath || '',
    history: extras.history || [],
  }),
  reset: () => api.post('/ai/reset'),
};

export default aiService;
