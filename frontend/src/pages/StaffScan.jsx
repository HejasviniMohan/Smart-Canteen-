import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { toast } from 'react-toastify'
import { Banknote, Camera, QrCode, CheckCircle, XCircle, Clock, ShoppingBag } from 'lucide-react'
import { pickupService } from '../services/orderService'
import { errorMessage } from '../services/api'
import { useLiveRefresh } from '../context/LiveUpdatesContext'
import { money, slotLabel } from '../utils/format'
import StatusBadge, { PaymentBadge } from '../components/StatusBadge'
import QrScanner from '../components/QrScanner'

// Signed QR codes start with this. Handheld USB scanners type the code into the box,
// so the same input accepts a scanned code as well as a typed token.
const isSignedCode = (value) => value.startsWith('SC1.')

export default function StaffScan() {
  const [searchParams] = useSearchParams()
  const [input, setInput] = useState(searchParams.get('token') || '')
  const [order, setOrder] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [scanning, setScanning] = useState(false)
  const [working, setWorking] = useState(false)
  const lastQuery = useRef(null)
  const inputRef = useRef(null)

  const lookup = useCallback(async (raw) => {
    const value = raw.trim()
    if (!value) return
    setLoading(true)
    setError(null)
    setOrder(null)
    try {
      const { data } = await (isSignedCode(value) ? pickupService.lookupCode(value) : pickupService.lookupToken(value))
      lastQuery.current = value
      setOrder(data)
    } catch (err) {
      setError(errorMessage(err, 'Order not found.'))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    inputRef.current?.focus()
    const token = searchParams.get('token')
    if (token) lookup(token)
  }, [lookup, searchParams])

  const refresh = useCallback(() => {
    if (!lastQuery.current) return
    const value = lastQuery.current
    const request = isSignedCode(value) ? pickupService.lookupCode(value) : pickupService.lookupToken(value)
    request.then(({ data }) => setOrder(data)).catch(() => {})
  }, [])

  useLiveRefresh(refresh, { filter: (e) => e.orderId === order?.id })

  const act = async (action, message) => {
    setWorking(true)
    try {
      const { data } = await action(order.id)
      setOrder(data)
      toast.success(message)
    } catch (err) {
      // For example, the other counter just collected this order: show the current state.
      toast.error(errorMessage(err))
      refresh()
    } finally {
      setWorking(false)
    }
  }

  const reset = () => {
    setOrder(null)
    setError(null)
    setInput('')
    lastQuery.current = null
    inputRef.current?.focus()
  }

  return (
    <div className="page-pad" style={{ maxWidth: 640, margin: '0 auto' }}>
      <div className="d-flex align-items-center justify-content-between mb-4">
        <div>
          <h1 className="h2 mb-1">Verify Pickup</h1>
          <p className="text-muted mb-0" style={{ fontSize: '0.9rem' }}>Scan the student's QR code, or enter their token</p>
        </div>
        <Link className="btn btn-outline-secondary" to="/staff">← Back</Link>
      </div>

      {scanning ? (
        <QrScanner
          onScan={(text) => { setScanning(false); setInput(text); lookup(text) }}
          onClose={() => setScanning(false)}
        />
      ) : (
        <button className="btn brand-btn w-100 mb-3 scan-open-btn" onClick={() => setScanning(true)}>
          <Camera size={18} /> Scan QR with camera
        </button>
      )}

      <form onSubmit={(e) => { e.preventDefault(); lookup(input) }} className="d-flex gap-2 mb-4">
        <div style={{ position: 'relative', flex: 1 }}>
          <QrCode size={18} style={{ position: 'absolute', left: 12, top: '50%', transform: 'translateY(-50%)', color: 'var(--muted)' }} />
          <input
            ref={inputRef}
            className="form-control"
            style={{ paddingLeft: 40, fontSize: '1rem', height: 44 }}
            placeholder="Token, e.g. A012"
            aria-label="Token or QR code"
            value={input}
            onChange={(e) => setInput(e.target.value)}
          />
        </div>
        <button type="submit" className="btn btn-outline-secondary" style={{ height: 44, padding: '0 24px' }} disabled={loading}>
          {loading ? 'Searching...' : 'Find'}
        </button>
      </form>

      {error && (
        <div className="scan-error" role="alert">
          <XCircle size={20} />
          <div>
            <strong>Not found</strong>
            <p className="mb-0 mt-1">{error}</p>
          </div>
        </div>
      )}

      {order && (
        <div className="scan-result" data-testid="scan-result">
          <div className="scan-result-header">
            <div>
              <div className="scan-token">Token {order.tokenNumber}</div>
              <div className="scan-order-id">Order #{order.id}</div>
            </div>
            <StatusBadge status={order.status} />
          </div>

          <div className="scan-info-grid">
            <div className="scan-info-item"><span className="scan-info-label">Customer</span><span className="scan-info-value">{order.userName}</span></div>
            <div className="scan-info-item"><span className="scan-info-label">Pickup</span><span className="scan-info-value">{slotLabel(order.pickupSlot)}</span></div>
            <div className="scan-info-item"><span className="scan-info-label">Total</span><span className="scan-info-value brand-text">{money(order.totalAmount)}</span></div>
            <div className="scan-info-item"><span className="scan-info-label">Payment</span><span className="scan-info-value"><PaymentBadge order={order} /></span></div>
          </div>

          <div className="scan-items">
            <h2 className="h6 mb-2">Order Items</h2>
            {order.items.map((item) => (
              <div key={item.id} className="scan-item">
                <span><ShoppingBag size={14} /> {item.foodItemName}</span>
                <span>× {item.quantity}</span>
                <span className="fw-semibold">{money(item.subtotal)}</span>
              </div>
            ))}
          </div>

          {order.notes && <div className="scan-notes"><strong>Notes:</strong> {order.notes}</div>}

          {order.status === 'READY' && order.paymentStatus !== 'PAID' && (
            <div className="scan-action-box">
              <div className="scan-warning">
                <Clock size={20} />
                <span>Collect <strong>{money(order.totalAmount)}</strong> in cash before handing over.</span>
              </div>
              <button className="scan-paid-btn" disabled={working}
                      onClick={() => act(pickupService.cashReceived, 'Cash recorded')}>
                <Banknote size={18} aria-hidden="true" /> Cash received
              </button>
            </div>
          )}

          {order.status === 'READY' && order.paymentStatus === 'PAID' && (
            <div className="scan-action-box">
              <div className="scan-success"><CheckCircle size={20} /><span>Paid and ready for pickup.</span></div>
              <button className="scan-ready-btn" disabled={working}
                      onClick={() => act(pickupService.collect, 'Order handed over')}>
                <CheckCircle size={18} /> Hand Over Food
              </button>
            </div>
          )}

          {['PLACED', 'ACCEPTED', 'PREPARING'].includes(order.status) && (
            <div className="scan-warning">
              <Clock size={20} />
              <span>Not ready yet. The kitchen hasn't marked this order ready.</span>
            </div>
          )}

          {order.status === 'PENDING_PAYMENT' && (
            <div className="scan-warning"><Clock size={20} /><span>This order hasn't been paid for yet. Do not hand over.</span></div>
          )}

          {order.status === 'COMPLETED' && (
            <div className="scan-completed"><CheckCircle size={20} /><span>Already <strong>collected</strong>. Do not hand over again.</span></div>
          )}

          {order.status === 'CANCELLED' && (
            <div className="scan-cancelled"><XCircle size={20} /><span>This order was <strong>cancelled</strong>. Do not hand over.</span></div>
          )}

          <button className="btn btn-outline-secondary scan-next-btn" onClick={reset}>Next customer</button>
        </div>
      )}

      {!order && !error && !loading && !scanning && (
        <div className="scan-empty">
          <QrCode size={48} style={{ color: 'var(--muted)' }} />
          <h2 className="h5">Ready to scan</h2>
          <p>Use the camera, a handheld scanner, or type the student's token.</p>
        </div>
      )}
    </div>
  )
}
