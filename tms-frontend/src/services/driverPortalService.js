import api from '../api/axios';

const driverPortalService = {
  getBoard: () => api.get('/me/board'),
};

export default driverPortalService;
