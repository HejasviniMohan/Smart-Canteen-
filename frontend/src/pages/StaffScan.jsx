import { useState, useRef, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { orderService } from '../services/orderService'
import { toast } from 'react-toastify'
import { QrCode, Search, CheckCircle, XCircle, Clock, ShoppingBag } from 'lucide-react'

export default function StaffScan() {
  const navigate = useNavigate()
  const [token, setToken] = useState('')
  const [order, setOrder] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [searched, setSearched] = useState(false)
  const inputRef = useRef(null)

  useEffect(() => {
    inputRef.current?.focus()
  }, [])

  const handleTokenSearch = async (e) => {
    e.preventDefault()
    if (!token.trim()) return
    setLoading(true)
    setError(null)
    setSearched(true)
    try {
      const r = await orderService.getByToken(token.trim())
      setOrder(r.data)
    } catch (err) {
      setOrder(null)
      const msg = err.response?.data?.message || 'Order not found for this token.'
      setError(msg)
    } finally {
      setLoading(false)
    }
  }

  const handleMarkCollected = async () => {
    if (!order) return
    try {
      await orderService.setStatus(order.id, 'COMPLETED')
      toast.success('Order marked as Collected')
      const r = await orderService.getByToken(token.trim())
      setOrder(r.data)
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to update order')
    }
  }

  const handleMarkPaid = async () => {
    if (!order) return
    try {
      await orderService.setPaymentStatus(order.id, 'PAID')
      toast.success('Payment marked as PAID')
      const r = await orderService.getByToken(token.trim())
      setOrder(r.data)
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to update payment')
    }
  }

  const statusColor = (status) => {
    switch (status) {
      case 'PLACED': return '#fef3c7'
      case 'ACCEPTED': return '#dbeafe'
      case 'PREPARING': return '#ede9fe'
      case 'READY': return '#d1fae5'
      case 'COMPLETED': return '#e5e7eb'
      case 'CANCELLED': return '#fee2e2'
      default: return '#f1f5f9'
    }
  }

  const statusTextColor = (status) => {
    switch (status) {
      case 'PLACED': return '#92400e'
      case 'ACCEPTED': return '#1e40af'
      case 'PREPARING': return '#5b21b6'
      case 'READY': return '#065f46'
      case 'COMPLETED': return '#1f2937'
      case 'CANCELLED': return '#991b1b'
      default: return '#475569'
    }
  }

  return (
    <div className="page-pad" style={{ maxWidth: 640, margin: '0 auto' }}>
      <div className="d-flex align-items-center justify-content-between mb-4">
        <div>
          <h2 className="mb-1">Verify Order</h2>
          <p className="text-muted mb-0" style={{ fontSize: '0.9rem' }}>Scan QR or enter token number</p>
        </div>
        <button className="btn btn-outline-secondary" onClick={() => navigate('/staff')}>
          ← Back
        </button>
      </div>

      <form onSubmit={handleTokenSearch} className="d-flex gap-2 mb-4">
        <div style={{ position: 'relative', flex: 1 }}>
          <QrCode size={18} style={{ position: 'absolute', left: 12, top: '50%', transform: 'translateY(-50%)', color: 'var(--muted)' }} />
          <input
            ref={inputRef}
            className="form-control"
            style={{ paddingLeft: 40, fontSize: '1rem', height: 44 }}
            placeholder="Enter token number (e.g. A100)"
            value={token}
            onChange={(e) => setToken(e.target.value)}
          />
        </div>
        <button type="submit" className="btn brand-btn text-white" style={{ height: 44, padding: '0 24px' }} disabled={loading}>
          {loading ? 'Searching...' : <>Search</>}
        </button>
      </form>

      {error && (
        <div className="scan-error">
          <XCircle size={20} />
          <div>
            <strong>Order not found</strong>
            <p className="mb-0 mt-1">{error}</p>
          </div>
        </div>
      )}

      {order && (
        <div className="scan-result">
          <div className="scan-result-header">
            <div>
              <div className="scan-token">Token #{order.tokenNumber}</div>
              <div className="scan-order-id">Order #{order.id}</div>
            </div>
            <span
              className="scan-status-badge"
              style={{ background: statusColor(order.status), color: statusTextColor(order.status) }}
            >
              {order.status}
            </span>
          </div>

          <div className="scan-info-grid">
            <div className="scan-info-item">
              <span className="scan-info-label">Customer</span>
              <span className="scan-info-value">{order.userName}</span>
            </div>
            <div className="scan-info-item">
              <span className="scan-info-label">Items</span>
              <span className="scan-info-value">{order.items?.length || 0} items</span>
            </div>
            <div className="scan-info-item">
              <span className="scan-info-label">Total</span>
              <span className="scan-info-value brand-text">₹{Number(order.totalAmount).toFixed(0)}</span>
            </div>
            <div className="scan-info-item">
              <span className="scan-info-label">Payment</span>
              <span className={`scan-info-value ${order.paymentStatus === 'PAID' ? 'text-success' : 'text-danger'}`}>
                {order.paymentStatus}
              </span>
            </div>
          </div>

          <div className="scan-items">
            <h6 className="mb-2">Order Items</h6>
            {order.items?.map((item) => (
              <div key={item.id} className="scan-item">
                <span><ShoppingBag size={14} /> {item.foodItemName}</span>
                <span>× {item.quantity}</span>
                <span className="fw-semibold">₹{Number(item.subtotal).toFixed(0)}</span>
              </div>
            ))}
          </div>

          {order.notes && (
            <div className="scan-notes">
              <strong>Notes:</strong> {order.notes}
            </div>
          )}

          {order.status === 'READY' && (
            <div className="scan-success">
              <CheckCircle size={20} />
              <span>Order is <strong>READY</strong> for pickup.</span>
            </div>
          )}

          {order.status === 'READY' && order.paymentStatus !== 'PAID' && (
            <div className="scan-action-box">
              <div className="scan-warning" style={{ background: '#fef3c7', color: '#92400e' }}>
                <Clock size={20} />
                <span>Payment is <strong>PENDING</strong> — collect cash and mark as paid.</span>
              </div>
              <button className="scan-paid-btn" onClick={handleMarkPaid}>
                💵 Mark Payment as PAID
              </button>
            </div>
          )}

          {order.status === 'READY' && order.paymentStatus === 'PAID' && (
            <div className="scan-action-box">
              <button className="scan-ready-btn" onClick={handleMarkCollected}>
                <CheckCircle size={18} /> Mark as Collected & Hand Over Food
              </button>
            </div>
          )}

          {order.status === 'PREPARING' && (
            <div className="scan-warning">
              <Clock size={20} />
              <span>Order is currently <strong>PREPARING</strong>. Kitchen is working on it.</span>
            </div>
          )}

          {order.status === 'COMPLETED' && (
            <div className="scan-completed">
              <CheckCircle size={20} />
              <span>This order has already been <strong>COLLECTED</strong>.</span>
            </div>
          )}

          {order.status === 'CANCELLED' && (
            <div className="scan-cancelled">
              <XCircle size={20} />
              <span>This order has been <strong>CANCELLED</strong>.</span>
            </div>
          )}

          {order.status === 'PLACED' || order.status === 'ACCEPTED' ? (
            <div className="scan-action">
              <p className="text-muted mb-2">This order has not been accepted yet. Accept it from the Staff Dashboard first.</p>
              <button className="btn btn-outline-secondary" onClick={() => navigate('/staff')}>
                Go to Staff Dashboard
              </button>
            </div>
          ) : null}
        </div>
      )}

      {!searched && !order && (
        <div className="scan-empty">
          <QrCode size={48} style={{ color: 'var(--muted)' }} />
          <h5>Enter a token number</h5>
          <p>Type the token shown on the customer's QR code (e.g. A100, B125) and press Enter or click Search.</p>
        </div>
      )}
    </div>
  )
}
