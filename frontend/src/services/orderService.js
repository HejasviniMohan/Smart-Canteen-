import api from './api'

export const orderService = {
  place:  (data, idempotencyKey) => api.post('/orders', data, { headers: { 'Idempotency-Key': idempotencyKey } }),
  mine:   ()       => api.get('/orders/me'),
  get:    (id)     => api.get(`/orders/${id}`),
  active: ()       => api.get('/orders/active'),
  all:    (params) => api.get('/orders', { params }),
  setStatus: (id, status) => api.put(`/orders/${id}/status`, { status }),
  cancel: (id)     => api.delete(`/orders/${id}`),
}

export const pickupService = {
  lookupCode:   (code)  => api.post('/pickup/lookup', { code }),
  lookupToken:  (token) => api.post('/pickup/lookup', { token }),
  cashReceived: (id)    => api.post(`/pickup/${id}/cash-received`),
  collect:      (id)    => api.post(`/pickup/${id}/collect`),
}

export const paymentService = {
  create:      (orderId) => api.post('/payments/create-order', { orderId }),
  verify:      (data)    => api.post('/payments/verify', data),
  sandboxPay:  (paymentId, success) => api.post('/payments/sandbox/pay', { paymentId, success }),
}

export const canteenService = {
  status: () => api.get('/canteen/status'),
  slots:  () => api.get('/canteen/slots'),
}

export const reviewService = {
  create:  (data) => api.post('/reviews', data),
  forFood: (id)   => api.get(`/reviews/food/${id}`),
  mine:    ()     => api.get('/reviews/me'),
}

export const adminService = {
  dashboard: ()     => api.get('/admin/dashboard'),
  topFoods:  (days) => api.get('/admin/top-foods', { params: { days } }),
  peakHours: (days) => api.get('/admin/peak-hours', { params: { days } }),
  revenue:   (days) => api.get('/admin/revenue-trend', { params: { days } }),
  payments:  (params) => api.get('/admin/payments', { params }),
  reviews:   (params) => api.get('/admin/reviews', { params }),
  deleteReview: (id)  => api.delete(`/admin/reviews/${id}`),
  users:     ()     => api.get('/admin/users'),
  createUser: (data) => api.post('/admin/users', data),
  setEnabled: (id, enabled) => api.put(`/admin/users/${id}/enabled`, { enabled }),
}
