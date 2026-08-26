import { useEffect, useState } from 'react'
import { orderService } from '../services/orderService'
import { foodService } from '../services/foodService'
import { useAuth } from '../context/AuthContext'
import { toast } from 'react-toastify'

const NEXT = {
  PLACED: 'ACCEPTED', ACCEPTED: 'PREPARING', PREPARING: 'READY', READY: 'COMPLETED',
}

export default function StaffDashboard() {
  const { user } = useAuth()
  const [active, setActive] = useState([])
  const [loading, setLoading] = useState(true)
  const [foods, setFoods] = useState([])

  const load = () => {
    orderService.all().then((r) => setActive(r.data.filter((o) => o.status !== 'COMPLETED' && o.status !== 'CANCELLED'))).finally(() => setLoading(false))
    foodService.list().then((r) => setFoods(r.data))
  }
  useEffect(() => {
    load()
    const t = setInterval(load, 5000)
    return () => clearInterval(t)
  }, [])

  const advance = async (o) => {
    const next = NEXT[o.status]
    if (!next) return
    try {
      await orderService.setStatus(o.id, next)
      toast.success(`Order ${o.tokenNumber} -> ${next}`)
      load()
    } catch (e) {
      toast.error(e.response?.data?.message || 'Failed')
    }
  }

  const markPaid = async (o) => {
    try {
      await orderService.setPaymentStatus(o.id, 'PAID')
      toast.success(`Payment marked as PAID for ${o.tokenNumber}`)
      load()
    } catch (e) {
      toast.error(e.response?.data?.message || 'Failed to update payment')
    }
  }

  const toggleFood = async (f) => {
    try {
      await foodService.toggle(f.id)
      load()
    } catch (e) { toast.error('Failed to toggle') }
  }

  if (user?.role !== 'ADMIN' && user?.role !== 'STAFF') return <div className="page-pad">Forbidden.</div>

  return (
    <div className="page-pad">
      <h2 className="mb-3">Staff Dashboard</h2>

      <h5>Active orders ({active.length})</h5>
      {loading && <p>Loading...</p>}
      {!loading && active.length === 0 && <div className="empty-state">No active orders.</div>}

      <div className="row g-3 mb-5">
        {active.map((o) => (
          <div key={o.id} className="col-12 col-md-6">
            <div className="card shadow-sm">
              <div className="card-body">
                <div className="d-flex justify-content-between">
                  <div>
                    <h5 className="mb-1">Token #{o.tokenNumber}</h5>
                    <small className="text-muted">{o.userName} • {new Date(o.orderDate).toLocaleTimeString()}</small>
                  </div>
                  <div className="text-end">
                    <span className={`status-pill ${o.status}`}>{o.status}</span>
                    {o.paymentStatus !== 'PAID' && (
                      <div className="mt-1">
                        <span className="badge bg-warning text-dark" style={{ fontSize: '0.7rem' }}>
                          {o.paymentStatus === 'PENDING' ? '💵 Pay at pickup' : o.paymentStatus}
                        </span>
                      </div>
                    )}
                    {o.paymentStatus === 'PAID' && (
                      <div className="mt-1"><span className="badge bg-success" style={{ fontSize: '0.7rem' }}>PAID</span></div>
                    )}
                  </div>
                </div>
                <ul className="small mt-2 mb-2">
                  {o.items.map((i) => (
                    <li key={i.id}>{i.foodItemName} × {i.quantity}</li>
                  ))}
                </ul>
                {o.notes && <div className="alert alert-light small py-1">📝 {o.notes}</div>}
                <div className="d-flex justify-content-between align-items-center">
                  <strong>₹{Number(o.totalAmount).toFixed(2)}</strong>
                  <div className="d-flex gap-2">
                    {o.paymentStatus !== 'PAID' && (o.status === 'READY' || o.status === 'PREPARING') && (
                      <button className="btn btn-outline-success btn-sm" onClick={() => markPaid(o)}>
                        Mark Paid
                      </button>
                    )}
                    {NEXT[o.status] && (
                      <button className="btn brand-btn text-white" onClick={() => advance(o)}>
                        → {NEXT[o.status]}
                      </button>
                    )}
                  </div>
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>

      <h5>Food availability</h5>
      <div className="row g-2">
        {foods.map((f) => (
          <div key={f.id} className="col-6 col-md-4 col-lg-3">
            <div className={`card p-2 ${!f.availability ? 'bg-light text-muted' : ''}`}>
              <div className="d-flex justify-content-between align-items-center">
                <span>{f.name}</span>
                <div className="form-check form-switch">
                  <input className="form-check-input" type="checkbox" checked={f.availability} onChange={() => toggleFood(f)} />
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}