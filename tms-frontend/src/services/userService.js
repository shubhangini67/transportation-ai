import api from '../api/axios';

const userService = {
  getAll: (params = {}) => api.get('/users', { params }),
  changeRole: (id, role) => api.put(`/users/${id}/role`, { role }),
  deactivate: (id) => api.patch(`/users/${id}/deactivate`),
  activate: (id) => api.patch(`/users/${id}/activate`),
};

export default userService;

