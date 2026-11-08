import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { orderService } from '../services/orderService'
import { errorMessage } from '../services/api'
import { useLiveRefresh } from '../context/LiveUpdatesContext'
import { isOngoing } from '../utils/orderStatus'
import { dateTime, money, slotLabel } from '../utils/format'
import OrderStatusTimeline from '../components/OrderStatusTimeline'
import StatusBadge from '../components/StatusBadge'
import { ErrorState, Skeleton } from '../components/Feedback'

const TABS = ['All', 'Ongoing', 'Completed', 'Cancelled']

export default function MyOrders() {
  const [orders, setOrders] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [tab, setTab] = useState('All')

  const load = useCallback(() => {
    orderService.mine()
      .then((r) => { setOrders(r.data); setError(null) })
      .catch((e) => setError(errorMessage(e)))
      .finally(() => setLoading(false))
  }, [])

  useEffect(load, [load])
  useLiveRefresh(load)

  const filtered = orders.filter((o) => {
    if (tab === 'Ongoing') return isOngoing(o.status)
    if (tab === 'Completed') return o.status === 'COMPLETED'
    if (tab === 'Cancelled') return o.status === 'CANCELLED'
    return true
  })

  if (loading) return <div className="page-pad"><Skeleton rows={4} height={140} /></div>
  if (error) return <div className="page-pad"><ErrorState message={error} onRetry={load} /></div>

  return (
    <div className="page-pad">
      <h1 className="h2 mb-3">My Orders</h1>
      <div className="d-flex gap-2 mb-4 flex-wrap">
        {TABS.map((t) => (
          <button key={t} aria-pressed={tab === t}
            className={`btn btn-sm ${tab === t ? 'brand-btn' : 'btn-outline-secondary'}`}
            onClick={() => setTab(t)}>
            {t}
          </button>
        ))}
      </div>
      {filtered.length === 0 ? (
        <div className="empty-state">
          <h4>{tab === 'All' ? 'No orders yet' : `No ${tab.toLowerCase()} orders`}</h4>
          <Link to="/menu" className="btn brand-btn mt-3">Order now</Link>
        </div>
      ) : (
        <div className="row g-3">
          {filtered.map((o) => (
            <div key={o.id} className="col-12 col-md-6">
              <Link to={`/orders/${o.id}`} className="text-decoration-none text-dark">
                <div className="card shadow-sm p-3">
                  <div className="d-flex justify-content-between gap-2">
                    <div>
                      <h2 className="h5 mb-1">Token {o.tokenNumber}</h2>
                      <small className="text-muted">
                        {isOngoing(o.status) ? `Pickup: ${slotLabel(o.pickupSlot)}` : dateTime(o.orderDate)}
                      </small>
                    </div>
                    <div><StatusBadge status={o.status} /></div>
                  </div>
                  <div className="mt-2"><OrderStatusTimeline current={o.status} /></div>
                  <div className="d-flex justify-content-between mt-2">
                    <span>{o.items.length} item{o.items.length === 1 ? '' : 's'}</span>
                    <span className="fw-semibold brand-text">{money(o.totalAmount)}</span>
                  </div>
                </div>
              </Link>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
