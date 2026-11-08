import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { toast } from 'react-toastify'
import { ShoppingBag, ArrowLeft, CreditCard, Banknote } from 'lucide-react'
import { useCart } from '../context/CartContext'
import { canteenService, orderService } from '../services/orderService'
import { foodService } from '../services/foodService'
import { errorMessage } from '../services/api'
import { syncWithMenu } from '../utils/cart'
import { money, slotLabel } from '../utils/format'
import { Skeleton } from '../components/Feedback'

const PAYMENT_METHODS = [
  { id: 'ONLINE', label: 'Pay online', hint: 'UPI, card or wallet', icon: CreditCard },
  { id: 'CASH_ON_PICKUP', label: 'Cash at pickup', hint: 'Pay when you collect', icon: Banknote },
]

const newKey = () => `${Date.now()}-${Math.random().toString(36).slice(2)}`

export default function Checkout() {
  const { items, total, clear, replace } = useCart()
  const navigate = useNavigate()
  const [notes, setNotes] = useState('')
  const [placing, setPlacing] = useState(false)
  const [error, setError] = useState(null)
  const [paymentMethod, setPaymentMethod] = useState('ONLINE')
  const [slots, setSlots] = useState(null)
  const [slot, setSlot] = useState(null)
  // The same key on every retry of this checkout, so a double-click or a retried
  // request returns the original order rather than creating a second one.
  const idempotencyKey = useRef(newKey())

  const loadSlots = () => canteenService.slots()
    .then(({ data }) => {
      setSlots(data)
      setSlot((current) => (data.some((s) => s.start === current && s.remaining > 0)
        ? current
        : data.find((s) => s.remaining > 0)?.start ?? null))
    })
    .catch((e) => setError(errorMessage(e, 'Could not load pickup slots')))

  useEffect(() => {
    loadSlots()
    // The cart may be days old, so check it against today's menu and prices.
    foodService.list().then(({ data }) => {
      const result = syncWithMenu(items, data)
      if (result.removed.length > 0) toast.warn(`No longer available: ${result.removed.join(', ')}`)
      if (result.repriced.length > 0) toast.info(`Price updated: ${result.repriced.join(', ')}`)
      if (result.removed.length > 0 || result.repriced.length > 0) replace(result.items)
    }).catch(() => {
      // The server re-checks prices when the order is placed, so nothing can be overcharged.
    })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const placeOrder = async () => {
    setError(null)
    setPlacing(true)
    try {
      const { data: order } = await orderService.place({
        items: items.map((i) => ({ foodItemId: i.id, quantity: i.quantity })),
        notes: notes.trim() || undefined,
        paymentMethod,
        pickupSlot: slot,
        expectedTotal: total,
      }, idempotencyKey.current)

      clear()
      if (paymentMethod === 'ONLINE') {
        // The order page handles payment, so a failed attempt can be retried from there.
        navigate(`/orders/${order.id}?pay=1`, { replace: true })
      } else {
        toast.success(`Order ${order.tokenNumber} placed`)
        navigate(`/orders/${order.id}`, { replace: true })
      }
    } catch (err) {
      const message = errorMessage(err, 'Could not place the order. Please try again.')
      setError(message)
      toast.error(message)
      if (err.response?.status === 409) {
        // A slot may just have filled up, so show the latest availability.
        loadSlots()
      }
    } finally {
      setPlacing(false)
    }
  }

  if (items.length === 0) {
    return (
      <div className="checkout-page">
        <div className="checkout-empty">
          <ShoppingBag size={48} style={{ color: 'var(--muted)' }} />
          <h4>Your cart is empty</h4>
          <p>Add something from the menu first.</p>
          <button className="btn brand-btn" onClick={() => navigate('/menu')}>Browse Menu</button>
        </div>
      </div>
    )
  }

  return (
    <div className="checkout-page">
      <div className="checkout-container">
        <div className="checkout-left">
          <div className="checkout-card">
            <div className="checkout-card-header">
              <h1 className="h4 mb-0">Order Summary</h1>
              <button className="btn-text" onClick={() => navigate('/cart')}>
                <ArrowLeft size={14} /> Edit cart
              </button>
            </div>
            <div className="checkout-items">
              {items.map((i) => (
                <div key={i.id} className="checkout-item">
                  <div className="checkout-item-info">
                    <span className="checkout-item-name">{i.name}</span>
                    <span className="checkout-item-qty">× {i.quantity}</span>
                  </div>
                  <span className="checkout-item-price">{money(i.price * i.quantity)}</span>
                </div>
              ))}
            </div>
            <div className="checkout-totals">
              <div className="checkout-total-row checkout-grand-total">
                <span>Total</span>
                <span className="brand-text" data-testid="checkout-total">{money(total)}</span>
              </div>
              <p className="checkout-demo-note">Prices include all taxes.</p>
            </div>
          </div>

          <div className="checkout-card">
            <label className="checkout-section-title" htmlFor="kitchen-notes">Kitchen Notes</label>
            <textarea
              id="kitchen-notes" className="checkout-notes" rows="3" maxLength={500} value={notes}
              onChange={(e) => setNotes(e.target.value)}
              placeholder="E.g. Less spicy, no onion, extra chutney..."
            />
          </div>
        </div>

        <div className="checkout-right">
          <div className="checkout-card">
            <h2 className="checkout-section-title" id="slot-heading">Pickup Time</h2>
            {slots === null && !error && <Skeleton rows={2} height={48} />}
            {slots !== null && slots.length === 0 && (
              <p className="text-muted mb-0">No pickup slots are available right now. Please check back during opening hours.</p>
            )}
            {slots !== null && slots.length > 0 && (
              <div className="slot-grid" role="radiogroup" aria-labelledby="slot-heading">
                {slots.map((s) => (
                  <button
                    key={s.start} type="button" role="radio" aria-checked={slot === s.start}
                    disabled={s.remaining === 0}
                    className={`slot-option ${slot === s.start ? 'selected' : ''}`}
                    onClick={() => setSlot(s.start)}
                  >
                    <span>{slotLabel(s.start)}</span>
                    <small>{s.remaining === 0 ? 'Full' : s.remaining <= 3 ? `${s.remaining} left` : 'Available'}</small>
                  </button>
                ))}
              </div>
            )}
          </div>

          <div className="checkout-card">
            <h2 className="checkout-section-title">Payment Method</h2>
            <div className="checkout-payment-options">
              {PAYMENT_METHODS.map(({ id, label, hint, icon: Icon }) => (
                <label key={id} className={`checkout-payment-option ${paymentMethod === id ? 'selected' : ''}`}>
                  <input
                    type="radio" name="payment" value={id} checked={paymentMethod === id}
                    onChange={() => { setPaymentMethod(id); setError(null) }}
                  />
                  <Icon size={16} />
                  <span>{label}<small className="d-block text-muted">{hint}</small></span>
                </label>
              ))}
            </div>
          </div>

          {error && (
            <div className="checkout-error" role="alert">
              <strong>Error:</strong> {error}
            </div>
          )}

          <button className="checkout-place-btn" onClick={placeOrder} disabled={placing || !slot}>
            {placing ? 'Placing order...' : paymentMethod === 'ONLINE' ? `Continue to Payment: ${money(total)}` : `Place Order: ${money(total)}`}
          </button>
        </div>
      </div>
    </div>
  )
}
