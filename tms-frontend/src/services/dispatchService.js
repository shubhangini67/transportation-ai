import api from '../api/axios';

const dispatchService = {
  recommend: (routeId, requiredCapacity = 1) =>
    api.get('/dispatch/recommend', { params: { routeId, requiredCapacity } }),
};

export default dispatchService;
