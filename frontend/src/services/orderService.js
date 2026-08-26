import api from './api'

export const orderService = {
  place:       (data)   => api.post('/orders', data),
  mine:        ()       => api.get('/orders/me'),
  get:         (id)     => api.get(`/orders/${id}`),
  getByToken:  (token)  => api.get(`/orders/token/${token}`),
  all:         (status) => api.get('/orders', { params: status ? { status } : {} }),
  setStatus:   (id, s)  => api.put(`/orders/${id}/status`, { status: s }),
  setPaymentStatus: (id, s) => api.put(`/orders/${id}/payment-status`, { paymentStatus: s }),
  cancel:      (id)     => api.delete(`/orders/${id}`),
}

export const paymentService = {
  create:  (orderId) => api.post('/payments/create-order', { orderId }),
  verify:  (data)    => api.post('/payments/verify', data),
  get:     (id)      => api.get(`/payments/${id}`),
}

export const reviewService = {
  create:  (data)   => api.post('/reviews', data),
  forFood: (id)     => api.get(`/reviews/food/${id}`),
  mine:    ()       => api.get('/reviews/me'),
}

export const adminService = {
  dashboard: () => api.get('/admin/dashboard'),
  topFoods:  () => api.get('/admin/top-foods'),
  peakHours: () => api.get('/admin/peak-hours'),
}