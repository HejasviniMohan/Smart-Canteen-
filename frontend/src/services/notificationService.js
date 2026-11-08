import api from './api'

export const notificationService = {
  mine:     ()   => api.get('/notifications/me'),
  markRead: (id) => api.put(`/notifications/${id}/read`),
  markAll:  ()   => api.put('/notifications/me/read-all'),
}
