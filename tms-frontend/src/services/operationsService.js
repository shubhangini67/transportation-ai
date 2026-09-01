import api from '../api/axios';

const operationsService = {
  getAlerts: () => api.get('/operations/alerts'),
};

export default operationsService;
