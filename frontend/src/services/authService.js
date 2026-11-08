import api from './api'

export const authService = {
  register: (data) => api.post('/auth/register', data),
  login:    (data) => api.post('/auth/login', data),
  me:       ()     => api.get('/auth/me'),
  update:   (data) => api.put('/auth/me', data),
  changePassword: (data) => api.put('/auth/me/password', data),
  logout:   ()     => api.post('/auth/logout'),
}
