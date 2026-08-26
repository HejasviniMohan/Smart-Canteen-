import api from './api'

export const foodService = {
  list:    (params) => api.get('/foods', { params }),
  get:     (id)     => api.get(`/foods/${id}`),
  create:  (data)   => api.post('/foods', data),
  update:  (id, d)  => api.put(`/foods/${id}`, d),
  toggle:  (id)     => api.put(`/foods/${id}/availability`),
  remove:  (id)     => api.delete(`/foods/${id}`),
}

export const categoryService = {
  list:   ()       => api.get('/categories'),
  create: (data)   => api.post('/categories', data),
  update: (id, d)  => api.put(`/categories/${id}`, d),
  remove: (id)     => api.delete(`/categories/${id}`),
}