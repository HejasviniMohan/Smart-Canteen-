import api from './api'

export const notificationService = {
  mine:    ()        => api.get('/notifications/me'),
  markRead:(id)      => api.put(`/notifications/${id}/read`),
}