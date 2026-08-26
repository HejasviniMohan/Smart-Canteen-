import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useCart } from '../context/CartContext'
import { orderService, paymentService } from '../services/orderService'
import { toast } from 'react-toastify'
import { ShoppingBag, ArrowLeft, CreditCard, Smartphone, Wallet, Banknote } from 'lucide-react'

const PAYMENT_METHODS = [
  { id: 'UPI', label: 'UPI', icon: Smartphone },
  { id: 'CARD', label: 'Card', icon: CreditCard },
  { id: 'WALLET', label: 'Wallet', icon: Wallet },
  { id: 'CASH_ON_PICKUP', label: 'Cash on Pickup', icon: Banknote },
]

export default function Checkout() {
  const { items, total, clear } = useCart()
  const navigate = useNavigate()
  const [notes, setNotes] = useState('')
  const [placing, setPlacing] = useState(false)
  const [error, setError] = useState(null)
  const [paymentMethod, setPaymentMethod] = useState('UPI')
  const [cardDetails, setCardDetails] = useState({ number: '', expiry: '', cvv: '', name: '' })
  const [cardErrors, setCardErrors] = useState({})

  const tax = Math.round(total * 0.05)
  const grandTotal = total + tax

  const validateCard = () => {
    const errs = {}
    if (!cardDetails.name.trim()) errs.name = 'Cardholder name required'
    if (!cardDetails.number.replace(/\s/g, '').match(/^\d{16}$/)) errs.number = 'Enter 16-digit card number'
    if (!cardDetails.expiry.match(/^\d{2}\/\d{2}$/)) errs.expiry = 'Enter MM/YY format'
    if (!cardDetails.cvv.match(/^\d{3,4}$/)) errs.cvv = 'Enter 3 or 4 digit CVV'
    setCardErrors(errs)
    return Object.keys(errs).length === 0
  }

  const handlePaymentChange = (method) => {
    setPaymentMethod(method)
    setError(null)
    setCardErrors({})
  }

  const handlePlaceOrder = async () => {
    if (items.length === 0) return

    if (paymentMethod === 'CARD' && !validateCard()) {
      return
    }

    setError(null)
    setPlacing(true)

    try {
      const orderRes = await orderService.place({
        items: items.map((i) => ({ foodItemId: i.id, quantity: i.quantity })),
        notes,
        paymentMethod,
      })
      const order = orderRes.data

      if (paymentMethod !== 'CASH_ON_PICKUP') {
        try {
          const payRes = await paymentService.create(order.id)
          const rp = payRes.data
          await paymentService.verify({
            paymentId: rp.paymentId,
            razorpayOrderId: rp.razorpayOrderId,
            razorpayPaymentId: 'pay_DEMO_' + Date.now(),
            razorpaySignature: 'demo-signature',
          })
        } catch (payErr) {
          const msg = payErr.response?.data?.message || 'Payment verification failed'
          toast.error(msg)
          setError(msg)
          setPlacing(false)
          return
        }
      }

      clear()
      toast.success(`Order ${order.tokenNumber} placed successfully!`)
      navigate(`/orders/${order.id}`)
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Could not place order. Please try again.'
      setError(msg)
      toast.error(msg)
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
          <p>Add some delicious items from the menu first.</p>
          <button className="btn brand-btn text-white" onClick={() => navigate('/menu')}>
            Browse Menu
          </button>
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
              <h3>Order Summary</h3>
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
                  <span className="checkout-item-price">₹{(i.price * i.quantity).toFixed(0)}</span>
                </div>
              ))}
            </div>
            <div className="checkout-totals">
              <div className="checkout-total-row">
                <span>Subtotal</span>
                <span>₹{total.toFixed(0)}</span>
              </div>
              <div className="checkout-total-row">
                <span>Tax (5%)</span>
                <span>₹{tax.toFixed(0)}</span>
              </div>
              <div className="checkout-total-row checkout-grand-total">
                <span>Total</span>
                <span className="brand-text">₹{grandTotal.toFixed(0)}</span>
              </div>
            </div>
          </div>

          <div className="checkout-card">
            <h4 className="checkout-section-title">Kitchen Notes</h4>
            <textarea
              className="checkout-notes"
              rows="3"
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              placeholder="E.g. Less spicy, no onion, extra chutney..."
            />
          </div>
        </div>

        <div className="checkout-right">
          <div className="checkout-card">
            <h4 className="checkout-section-title">Payment Method</h4>
            <div className="checkout-payment-options">
              {PAYMENT_METHODS.map(({ id, label, icon: Icon }) => (
                <label
                  key={id}
                  className={`checkout-payment-option ${paymentMethod === id ? 'selected' : ''}`}
                >
                  <input
                    type="radio"
                    name="payment"
                    value={id}
                    checked={paymentMethod === id}
                    onChange={() => handlePaymentChange(id)}
                  />
                  <Icon size={16} />
                  <span>{label}</span>
                </label>
              ))}
            </div>
            {paymentMethod === 'CARD' && (
              <div className="card-form">
                <div className="card-field">
                  <label>Card Number</label>
                  <input
                    type="text"
                    placeholder="1234 5678 9012 3456"
                    maxLength={19}
                    value={cardDetails.number}
                    onChange={(e) => setCardDetails((d) => ({ ...d, number: e.target.value.replace(/\D/g, '').replace(/(.{4})/g, '$1 ').trim() }))}
                  />
                  {cardErrors.number && <span className="card-error">{cardErrors.number}</span>}
                </div>
                <div className="card-row">
                  <div className="card-field">
                    <label>Expiry</label>
                    <input
                      type="text"
                      placeholder="MM/YY"
                      maxLength={5}
                      value={cardDetails.expiry}
                      onChange={(e) => setCardDetails((d) => ({ ...d, expiry: e.target.value }))}
                    />
                    {cardErrors.expiry && <span className="card-error">{cardErrors.expiry}</span>}
                  </div>
                  <div className="card-field">
                    <label>CVV</label>
                    <input
                      type="password"
                      placeholder="•••"
                      maxLength={4}
                      value={cardDetails.cvv}
                      onChange={(e) => setCardDetails((d) => ({ ...d, cvv: e.target.value.replace(/\D/g, '') }))}
                    />
                    {cardErrors.cvv && <span className="card-error">{cardErrors.cvv}</span>}
                  </div>
                </div>
                <div className="card-field">
                  <label>Cardholder Name</label>
                  <input
                    type="text"
                    placeholder="As on card"
                    value={cardDetails.name}
                    onChange={(e) => setCardDetails((d) => ({ ...d, name: e.target.value }))}
                  />
                  {cardErrors.name && <span className="card-error">{cardErrors.name}</span>}
                </div>
                <p className="card-demo-note">Demo mode — no real card data is stored</p>
              </div>
            )}
            {paymentMethod === 'CASH_ON_PICKUP' && (
              <p className="checkout-demo-note" style={{ color: 'var(--green)', fontWeight: 600 }}>
                Pay ₹{grandTotal.toFixed(0)} in cash when you pick up your order.
              </p>
            )}
            {paymentMethod !== 'CASH_ON_PICKUP' && (
              <p className="checkout-demo-note">Demo mode — payment is simulated</p>
            )}
          </div>

          {error && (
            <div className="checkout-error">
              <strong>Error:</strong> {error}
            </div>
          )}

          <button
            className="checkout-place-btn"
            onClick={handlePlaceOrder}
            disabled={placing}
          >
            {placing ? 'Processing...' : paymentMethod === 'CASH_ON_PICKUP' ? `Confirm Order — ₹${grandTotal.toFixed(0)}` : `Pay Now — ₹${grandTotal.toFixed(0)}`}
          </button>

          <p className="checkout-terms">
            By placing the order, you agree to the Smart Canteen terms of service.
          </p>
        </div>
      </div>
    </div>
  )
}
