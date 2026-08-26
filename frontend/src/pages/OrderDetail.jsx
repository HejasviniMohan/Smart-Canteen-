import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { QRCodeCanvas } from 'qrcode.react'
import { orderService } from '../services/orderService'
import { toast } from 'react-toastify'

export default function OrderDetail() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [order, setOrder] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let timer
    const load = () => {
      orderService.get(id).then((r) => setOrder(r.data)).catch(() => {}).finally(() => setLoading(false))
    }
    load()
    timer = setInterval(load, 5000)
    return () => clearInterval(timer)
  }, [id])

  const handleCancel = async () => {
    if (!confirm('Cancel this order?')) return
    try {
      await orderService.cancel(id)
      toast.success('Order cancelled')
      navigate('/orders')
    } catch (e) {
      toast.error(e.response?.data?.message || 'Cannot cancel')
    }
  }

  if (loading) return <div className="page-pad">Loading...</div>
  if (!order) return <div className="page-pad">Order not found.</div>

  const statusLabel = {
    PLACED: 'Order Placed',
    ACCEPTED: 'Accepted',
    PREPARING: 'Preparing',
    READY: 'Ready for Pickup',
    COMPLETED: 'Completed',
    CANCELLED: 'Cancelled',
  }[order.status] || order.status

  const statusColor = {
    PLACED: '#fef3c7', ACCEPTED: '#dbeafe', PREPARING: '#ede9fe',
    READY: '#d1fae5', COMPLETED: '#e5e7eb', CANCELLED: '#fee2e2',
  }[order.status] || '#f1f5f9'

  const statusTextColor = {
    PLACED: '#92400e', ACCEPTED: '#1e40af', PREPARING: '#5b21b6',
    READY: '#065f46', COMPLETED: '#1f2937', CANCELLED: '#991b1b',
  }[order.status] || '#475569'

  return (
    <div className="page-pad" style={{ maxWidth: 680, margin: '0 auto' }}>
      <div className="od-header">
        <div>
          <h2 className="od-title">Order Confirmed!</h2>
          <p className="od-subtitle text-muted">Thank you for your order</p>
        </div>
        <div className="od-token">
          <div className="od-token-label">Your Token</div>
          <div className="od-token-number">{order.tokenNumber}</div>
        </div>
      </div>

      <div style={{ background: statusColor, color: statusTextColor, padding: '12px 18px', borderRadius: '12px', fontSize: '0.92rem', fontWeight: 600, marginBottom: '20px', textAlign: 'center' }}>
        {statusLabel}
      </div>

      <div className="od-qr-section">
        <p className="text-muted mb-3" style={{ fontSize: '0.88rem', textAlign: 'center' }}>Show this QR code at the counter</p>
        <div className="od-qr-box">
          <QRCodeCanvas value={`SMARTCANTEEN|TOKEN:${order.tokenNumber}|ORDER:${order.id}`} size={160} />
        </div>
        <div className="od-token-big">{order.tokenNumber}</div>
      </div>

      <div className="od-card">
        <div className="od-section-title">Order Details</div>
        <div className="od-meta-grid">
          <div className="od-meta-item"><span>Order ID</span><strong>#{order.id}</strong></div>
          <div className="od-meta-item"><span>Date</span><strong>{new Date(order.createdAt || order.orderDate).toLocaleString()}</strong></div>
          <div className="od-meta-item"><span>Payment</span><strong className={order.paymentStatus === 'PAID' ? 'text-success' : 'text-danger'}>{order.paymentStatus}</strong></div>
          {order.notes && <div className="od-meta-item od-notes"><span>Notes</span><strong>{order.notes}</strong></div>}
        </div>
      </div>

      <div className="od-card">
        <div className="od-section-title">Items ({order.items?.length || 0})</div>
        <div className="od-items">
          {order.items.map((i) => (
            <div key={i.id} className="od-item">
              <div className="od-item-info">
                <span className="od-item-name">{i.foodItemName}</span>
                <span className="od-item-qty">× {i.quantity}</span>
              </div>
              <span className="od-item-price">₹{Number(i.subtotal).toFixed(0)}</span>
            </div>
          ))}
        </div>
        <div className="od-total-row">
          <span>Total Paid</span>
          <span className="od-total-amount">₹{Number(order.totalAmount).toFixed(0)}</span>
        </div>
      </div>

      <div className="d-flex gap-2 mt-3 flex-wrap">
        {['PLACED', 'ACCEPTED'].includes(order.status) && (
          <button className="btn od-cancel-btn" onClick={handleCancel}>Cancel Order</button>
        )}
        <button className="btn od-secondary-btn" onClick={() => navigate('/orders')}>All My Orders</button>
      </div>
    </div>
  )
}