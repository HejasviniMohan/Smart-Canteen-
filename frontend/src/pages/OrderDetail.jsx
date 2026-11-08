import { useCallback, useEffect, useState } from 'react'
import { useParams, useNavigate, useSearchParams } from 'react-router-dom'
import { QRCodeCanvas } from 'qrcode.react'
import { toast } from 'react-toastify'
import { orderService, reviewService } from '../services/orderService'
import { errorMessage } from '../services/api'
import { useAuth } from '../context/AuthContext'
import { useLiveRefresh } from '../context/LiveUpdatesContext'
import { canStudentCancel, isOngoing, STATUS_LABEL } from '../utils/orderStatus'
import { dateTime, money, slotLabel } from '../utils/format'
import OrderStatusTimeline from '../components/OrderStatusTimeline'
import StatusBadge, { PaymentBadge } from '../components/StatusBadge'
import PaymentPanel from '../components/PaymentPanel'
import ReviewForm from '../components/ReviewForm'
import { ErrorState, Skeleton } from '../components/Feedback'

export default function OrderDetail() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { user } = useAuth()
  const [searchParams] = useSearchParams()
  const [order, setOrder] = useState(null)
  const [reviews, setReviews] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const isOwner = order && user && order.userId === (user.userId ?? user.id)

  const load = useCallback(() => {
    orderService.get(id)
      .then((r) => { setOrder(r.data); setError(null) })
      .catch((e) => setError(errorMessage(e, 'Order not found')))
      .finally(() => setLoading(false))
  }, [id])

  const loadReviews = useCallback(() => {
    reviewService.mine().then((r) => setReviews(r.data)).catch(() => {})
  }, [])

  useEffect(load, [load])
  useLiveRefresh(load, { filter: (e) => String(e.orderId) === String(id) })

  useEffect(() => {
    if (user?.role === 'STUDENT' && order?.status === 'COMPLETED') loadReviews()
  }, [user?.role, order?.status, loadReviews])

  const handleCancel = async () => {
    const paid = order.paymentStatus === 'PAID'
    if (!window.confirm(paid ? 'Cancel this order? Your payment will be refunded.' : 'Cancel this order?')) return
    try {
      await orderService.cancel(id)
      toast.success(paid ? 'Order cancelled. Your refund is on its way.' : 'Order cancelled')
      load()
    } catch (e) {
      toast.error(errorMessage(e, 'Could not cancel the order'))
    }
  }

  if (loading) return <div className="page-pad" style={{ maxWidth: 680, margin: '0 auto' }}><Skeleton rows={3} height={160} /></div>
  if (error && !order) return <div className="page-pad"><ErrorState message={error} onRetry={load} /></div>

  const awaitingPayment = order.status === 'PENDING_PAYMENT'
  const showQr = isOwner && order.pickupCode

  return (
    <div className="page-pad" style={{ maxWidth: 680, margin: '0 auto' }}>
      <div className="od-header">
        <div>
          <h1 className="od-title">{STATUS_LABEL[order.status]}</h1>
          <p className="od-subtitle text-muted">
            {isOngoing(order.status) ? `Pickup: ${slotLabel(order.pickupSlot)}` : `Ordered ${dateTime(order.orderDate)}`}
          </p>
        </div>
        <div className="od-token">
          <div className="od-token-label">Token</div>
          <div className="od-token-number" data-testid="order-token">{order.tokenNumber}</div>
        </div>
      </div>

      <div className="mb-3" data-testid="order-status"><StatusBadge status={order.status} /></div>
      <div className="mb-3"><OrderStatusTimeline current={order.status} /></div>

      {awaitingPayment && isOwner && (
        <PaymentPanel order={order} autoStart={searchParams.get('pay') === '1'} onPaid={load} />
      )}

      {showQr && (
        <div className="od-qr-section">
          <p className="text-muted mb-3" style={{ fontSize: '0.88rem', textAlign: 'center' }}>
            {order.status === 'READY' ? 'Your food is ready! Show this at the counter.' : 'Show this at the counter when your order is ready.'}
          </p>
          <div className="od-qr-box">
            <QRCodeCanvas value={order.pickupCode} size={180} level="M" role="img" aria-label={`Pickup QR code for token ${order.tokenNumber}`} />
          </div>
          <div className="od-token-big">{order.tokenNumber}</div>
          {order.paymentStatus !== 'PAID' && <p className="text-muted mt-2 mb-0">Please have {money(order.totalAmount)} in cash ready.</p>}
        </div>
      )}

      {order.status === 'COMPLETED' && isOwner && (
        <ReviewForm order={order} reviews={reviews} onSaved={loadReviews} />
      )}

      <div className="od-card">
        <div className="od-section-title">Order Details</div>
        <div className="od-meta-grid">
          <div className="od-meta-item"><span>Order ID</span><strong>#{order.id}</strong></div>
          {!isOwner && <div className="od-meta-item"><span>Customer</span><strong>{order.userName}</strong></div>}
          <div className="od-meta-item"><span>Ordered</span><strong>{dateTime(order.orderDate)}</strong></div>
          <div className="od-meta-item"><span>Payment</span><strong><PaymentBadge order={order} /></strong></div>
          {order.collectedAt && <div className="od-meta-item"><span>Collected</span><strong>{dateTime(order.collectedAt)}</strong></div>}
          {order.notes && <div className="od-meta-item od-notes"><span>Notes</span><strong>{order.notes}</strong></div>}
        </div>
      </div>

      <div className="od-card">
        <div className="od-section-title">Items ({order.items.length})</div>
        <div className="od-items">
          {order.items.map((i) => (
            <div key={i.id} className="od-item">
              <div className="od-item-info">
                <span className="od-item-name">{i.foodItemName}</span>
                <span className="od-item-qty">× {i.quantity}</span>
              </div>
              <span className="od-item-price">{money(i.subtotal)}</span>
            </div>
          ))}
        </div>
        <div className="od-total-row">
          <span>Total</span>
          <span className="od-total-amount">{money(order.totalAmount)}</span>
        </div>
      </div>

      <div className="d-flex gap-2 mt-3 flex-wrap">
        {isOwner && canStudentCancel(order.status) && (
          <button className="btn od-cancel-btn" onClick={handleCancel}>Cancel Order</button>
        )}
        <button className="btn od-secondary-btn" onClick={() => navigate(-1)}>Back</button>
      </div>
      {isOwner && ['PREPARING', 'READY'].includes(order.status) && (
        <p className="text-muted small mt-2 mb-0">
          The kitchen has started on this order, so it can no longer be cancelled.
        </p>
      )}
    </div>
  )
}
