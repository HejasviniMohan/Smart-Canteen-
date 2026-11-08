import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { toast } from 'react-toastify'
import { QrCode } from 'lucide-react'
import { orderService } from '../services/orderService'
import { foodService } from '../services/foodService'
import { errorMessage } from '../services/api'
import { useLiveConnected, useLiveRefresh } from '../context/LiveUpdatesContext'
import { canStaffCancel, NEXT_KITCHEN_STATUS, STATUS_LABEL } from '../utils/orderStatus'
import { money, slotLabel } from '../utils/format'
import StatusBadge, { PaymentBadge } from '../components/StatusBadge'
import { ErrorState, Skeleton } from '../components/Feedback'

export default function StaffDashboard() {
  const [active, setActive] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [foods, setFoods] = useState([])
  const live = useLiveConnected()

  const loadOrders = useCallback(() => {
    orderService.active()
      .then((r) => { setActive(r.data); setError(null) })
      .catch((e) => setError(errorMessage(e)))
      .finally(() => setLoading(false))
  }, [])

  const loadFoods = useCallback(() => {
    foodService.list().then((r) => setFoods(r.data)).catch((e) => toast.error(errorMessage(e, 'Could not load foods')))
  }, [])

  useEffect(() => { loadOrders(); loadFoods() }, [loadOrders, loadFoods])
  useLiveRefresh(loadOrders, { fallbackMs: 30000 })

  const advance = async (o) => {
    const next = NEXT_KITCHEN_STATUS[o.status]
    try {
      await orderService.setStatus(o.id, next)
      toast.success(`${o.tokenNumber}: ${STATUS_LABEL[next]}`)
      loadOrders()
    } catch (e) {
      toast.error(errorMessage(e))
      loadOrders()
    }
  }

  const cancel = async (o) => {
    const paid = o.paymentStatus === 'PAID'
    if (!window.confirm(`Cancel order ${o.tokenNumber}?${paid ? ' The student will be refunded.' : ''}`)) return
    try {
      await orderService.setStatus(o.id, 'CANCELLED')
      toast.success(`Order ${o.tokenNumber} cancelled`)
      loadOrders()
    } catch (e) {
      toast.error(errorMessage(e))
    }
  }

  const toggleFood = async (f) => {
    try {
      await foodService.toggle(f.id)
      loadFoods()
    } catch (e) {
      toast.error(errorMessage(e, 'Could not update availability'))
    }
  }

  return (
    <div className="page-pad">
      <div className="d-flex justify-content-between align-items-center flex-wrap gap-2 mb-3">
        <div>
          <h1 className="h2 mb-1">Staff Dashboard</h1>
          <small className={live ? 'live-on' : 'text-muted'} data-testid="live-indicator">
            {live ? '● Live' : 'Connecting to live updates...'}
          </small>
        </div>
        <Link to="/staff/scan" className="btn brand-btn"><QrCode size={16} /> Verify Pickup</Link>
      </div>

      <h2 className="h5">Active orders ({active.length})</h2>
      {loading && <Skeleton rows={3} height={150} />}
      {!loading && error && <ErrorState message={error} onRetry={loadOrders} />}
      {!loading && !error && active.length === 0 && <div className="empty-state">No active orders.</div>}

      <div className="row g-3 mb-5">
        {active.map((o) => (
          <div key={o.id} className="col-12 col-md-6" data-testid={`order-card-${o.id}`}>
            <div className="card shadow-sm">
              <div className="card-body">
                <div className="d-flex justify-content-between gap-2">
                  <div>
                    <h3 className="h5 mb-1">Token {o.tokenNumber}</h3>
                    <small className="text-muted">{o.userName} • Pickup {slotLabel(o.pickupSlot)}</small>
                  </div>
                  <div className="text-end">
                    <StatusBadge status={o.status} />
                    <div className="mt-1"><PaymentBadge order={o} /></div>
                  </div>
                </div>
                <ul className="small mt-2 mb-2">
                  {o.items.map((i) => <li key={i.id}>{i.foodItemName} × {i.quantity}</li>)}
                </ul>
                {o.notes && <div className="alert alert-light small py-1"><strong>Note:</strong> {o.notes}</div>}
                <div className="d-flex justify-content-between align-items-center flex-wrap gap-2">
                  <strong>{money(o.totalAmount)}</strong>
                  <div className="d-flex gap-2 flex-wrap">
                    {canStaffCancel(o.status) && (
                      <button className="btn btn-outline-danger btn-sm" onClick={() => cancel(o)}>Cancel</button>
                    )}
                    {NEXT_KITCHEN_STATUS[o.status] && (
                      <button className="btn brand-btn btn-sm" onClick={() => advance(o)}>
                        Mark {STATUS_LABEL[NEXT_KITCHEN_STATUS[o.status]].toLowerCase()}
                      </button>
                    )}
                    {o.status === 'READY' && (
                      <Link to={`/staff/scan?token=${o.tokenNumber}`} className="btn btn-outline-success btn-sm">Hand over</Link>
                    )}
                  </div>
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>

      <h2 className="h5">Food availability</h2>
      <div className="row g-2">
        {foods.map((f) => (
          <div key={f.id} className="col-6 col-md-4 col-lg-3">
            <div className={`card p-2 ${!f.availability ? 'bg-light text-muted' : ''}`}>
              <div className="d-flex justify-content-between align-items-center">
                <label htmlFor={`avail-${f.id}`} className="mb-0">{f.name}</label>
                <div className="form-check form-switch mb-0">
                  <input id={`avail-${f.id}`} className="form-check-input" type="checkbox" role="switch"
                         checked={f.availability} onChange={() => toggleFood(f)} />
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
