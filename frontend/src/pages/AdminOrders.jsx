import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { orderService } from '../services/orderService'
import { Clock, CheckCircle, XCircle, ShoppingBag, Filter } from 'lucide-react'

export default function AdminOrders() {
  const [orders, setOrders] = useState([])
  const [loading, setLoading] = useState(true)
  const [statusFilter, setStatusFilter] = useState('ALL')

  const fetchOrders = () => {
    setLoading(true)
    orderService.all(statusFilter === 'ALL' ? null : statusFilter)
      .then((r) => setOrders(r.data || []))
      .catch(() => {})
      .finally(() => setLoading(false))
  }

  useEffect(() => { fetchOrders() }, [statusFilter])

  const statusColor = (s) => {
    switch (s) {
      case 'PLACED': return { bg: '#fef3c7', text: '#92400e' }
      case 'ACCEPTED': return { bg: '#dbeafe', text: '#1e40af' }
      case 'PREPARING': return { bg: '#ede9fe', text: '#5b21b6' }
      case 'READY': return { bg: '#d1fae5', text: '#065f46' }
      case 'COMPLETED': return { bg: '#e5e7eb', text: '#1f2937' }
      case 'CANCELLED': return { bg: '#fee2e2', text: '#991b1b' }
      default: return { bg: '#f1f5f9', text: '#475569' }
    }
  }

  const statuses = ['ALL', 'PLACED', 'ACCEPTED', 'PREPARING', 'READY', 'COMPLETED', 'CANCELLED']

  return (
    <div className="page-pad">
      <div className="d-flex align-items-center justify-content-between mb-4">
        <div>
          <h2 className="mb-1">Order Management</h2>
          <p className="text-muted mb-0" style={{ fontSize: '0.9rem' }}>{orders.length} orders found</p>
        </div>
      </div>

      <div className="order-filters mb-4">
        {statuses.map((s) => (
          <button
            key={s}
            className={`filter-chip ${statusFilter === s ? 'active' : ''}`}
            onClick={() => setStatusFilter(s)}
          >
            {s === 'ALL' ? 'All Orders' : s}
          </button>
        ))}
      </div>

      {loading && <div className="text-center py-5 text-muted">Loading orders...</div>}

      {!loading && orders.length === 0 && (
        <div className="text-center py-5">
          <ShoppingBag size={48} style={{ color: 'var(--muted)' }} />
          <h5 className="mt-3">No orders found</h5>
          <p className="text-muted">{statusFilter === 'ALL' ? 'No orders have been placed yet.' : `No ${statusFilter} orders.`}</p>
        </div>
      )}

      {!loading && orders.length > 0 && (
        <div className="admin-order-list">
          {orders.map((o) => {
            const c = statusColor(o.status)
            return (
              <div key={o.id} className="admin-order-card">
                <div className="admin-order-header">
                  <div>
                    <span className="fw-bold" style={{ fontSize: '1.1rem' }}>#{o.tokenNumber}</span>
                    <span className="ms-2 text-muted" style={{ fontSize: '0.85rem' }}>Order #{o.id}</span>
                  </div>
                  <span className="order-status-badge" style={{ background: c.bg, color: c.text }}>
                    {o.status}
                  </span>
                </div>
                <div className="admin-order-body">
                  <div className="admin-order-meta">
                    <span><strong>Customer:</strong> {o.userName}</span>
                    <span><strong>Items:</strong> {o.items?.length || 0}</span>
                    <span><strong>Total:</strong> <span className="brand-text fw-semibold">₹{Number(o.totalAmount).toFixed(0)}</span></span>
                    <span><strong>Payment:</strong> <span className={o.paymentStatus === 'PAID' ? 'text-success' : 'text-danger'}>{o.paymentStatus}</span></span>
                    <span><strong>Date:</strong> {new Date(o.createdAt).toLocaleString()}</span>
                  </div>
                  <div className="admin-order-items">
                    {o.items?.map((item) => (
                      <span key={item.id} className="order-item-chip">
                        {item.foodItemName} × {item.quantity}
                      </span>
                    ))}
                  </div>
                </div>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
