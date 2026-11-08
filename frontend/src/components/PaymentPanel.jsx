import { useEffect, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { toast } from 'react-toastify'
import { CreditCard, FlaskConical } from 'lucide-react'
import { token } from '../theme'
import { paymentService } from '../services/orderService'
import { errorMessage } from '../services/api'
import { useAuth } from '../context/AuthContext'
import { money, timeOnly } from '../utils/format'

const RAZORPAY_SCRIPT = 'https://checkout.razorpay.com/v1/checkout.js'

function loadRazorpay() {
  if (window.Razorpay) return Promise.resolve()
  return new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = RAZORPAY_SCRIPT
    script.onload = resolve
    script.onerror = () => reject(new Error('Could not load the payment page. Check your connection.'))
    document.body.appendChild(script)
  })
}

/**
 * Pays for an order, or retries a failed attempt. Razorpay's hosted checkout collects card
 * and UPI details, so they never touch this app. Either way, the server only marks the
 * order paid after verifying a signature.
 */
export default function PaymentPanel({ order, autoStart, onPaid }) {
  const { user } = useAuth()
  const [busy, setBusy] = useState(false)
  const [sandbox, setSandbox] = useState(null)
  const started = useRef(false)

  const verify = async (paymentId, signed) => {
    await paymentService.verify({ paymentId, ...signed })
    toast.success('Payment successful')
    onPaid()
  }

  const openRazorpay = async (checkout) => {
    await loadRazorpay()
    const instance = new window.Razorpay({
      key: checkout.keyId,
      amount: checkout.amountInPaise,
      currency: checkout.currency,
      order_id: checkout.razorpayOrderId,
      name: 'Smart Canteen',
      description: `Order ${order.tokenNumber}`,
      prefill: { name: user?.name, email: user?.email },
      theme: { color: token('--accent') },
      handler: (response) => verify(checkout.paymentId, {
        razorpayOrderId: response.razorpay_order_id,
        razorpayPaymentId: response.razorpay_payment_id,
        razorpaySignature: response.razorpay_signature,
      }).catch((e) => toast.error(errorMessage(e, 'Payment could not be verified'))),
      modal: { ondismiss: () => setBusy(false) },
    })
    instance.on('payment.failed', () => toast.error('Payment failed. You can try again.'))
    instance.open()
  }

  const start = async () => {
    setBusy(true)
    try {
      const { data: checkout } = await paymentService.create(order.id)
      if (checkout.gateway === 'sandbox') {
        setSandbox(checkout)
        return
      }
      await openRazorpay(checkout)
    } catch (e) {
      toast.error(errorMessage(e, e.message))
      setBusy(false)
    }
  }

  const sandboxResult = async (success) => {
    try {
      const { data: signed } = await paymentService.sandboxPay(sandbox.paymentId, success)
      await verify(sandbox.paymentId, signed)
    } catch (e) {
      toast.error(e.response?.status === 402 ? 'Payment failed. You can try again.' : errorMessage(e))
    } finally {
      setSandbox(null)
      setBusy(false)
    }
  }

  useEffect(() => {
    if (autoStart && !started.current) {
      started.current = true
      start()
    }
    // Runs once on arrival from checkout.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [autoStart])

  return (
    <div className="od-card pay-panel">
      <div>
        <strong>Payment needed</strong>
        <p className="text-muted mb-0">
          Complete payment by {timeOnly(order.paymentDeadline)} to keep your order and pickup slot.
        </p>
      </div>
      <button className="btn brand-btn" onClick={start} disabled={busy}>
        <CreditCard size={16} /> {busy ? 'Opening...' : `Pay ${money(order.totalAmount)}`}
      </button>

      {/* Rendered into <body>, so no animated or clipped parent can confine the dialog. */}
      {sandbox && createPortal(
        <div className="modal-backdrop-sc" role="dialog" aria-modal="true" aria-labelledby="sandbox-title">
          <div className="sandbox-box">
            <div className="sandbox-tag"><FlaskConical size={14} /> Sandbox: no real money moves</div>
            <h4 id="sandbox-title">Pay {money(sandbox.amount)}</h4>
            <p className="text-muted">
              This simulated provider stands in for Razorpay in the demo. The server still signs and
              verifies the result the same way.
            </p>
            <div className="d-flex gap-2 flex-wrap">
              <button className="btn brand-btn" onClick={() => sandboxResult(true)}>Simulate success</button>
              <button className="btn btn-outline-danger" onClick={() => sandboxResult(false)}>Simulate failure</button>
            </div>
          </div>
        </div>,
        document.body,
      )}
    </div>
  )
}
