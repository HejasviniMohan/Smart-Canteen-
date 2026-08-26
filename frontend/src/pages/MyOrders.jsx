import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { orderService } from '../services/orderService'
import OrderStatusTimeline from '../components/OrderStatusTimeline'

const TABS = ['All', 'Ongoing', 'Completed', 'Cancelled']
const ONGOING = ['PLACED', 'ACCEPTED', 'PREPARING', 'READY']

export default function MyOrders() {
  const [orders, setOrders] = useState([])
  const [loading, setLoading] = useState(true)
  const [tab, setTab] = useState('All')

  useEffect(() => {
    orderService.mine().then((r) => setOrders(r.data)).finally(() => setLoading(false))
  }, [])

  const filtered = orders.filter((o) => {
    if (tab === 'Ongoing') return ONGOING.includes(o.status)
    if (tab === 'Completed') return o.status === 'COMPLETED'
    if (tab === 'Cancelled') return o.status === 'CANCELLED'
    return true
  })

  if (loading) return <div className="page-pad">Loading...</div>

  return (
    <div className="page-pad">
      <h2 className="mb-3">My Orders</h2>
      <div className="d-flex gap-2 mb-4 flex-wrap">
        {TABS.map((t) => (
          <button key={t}
            className={`btn btn-sm ${tab === t ? 'brand-btn text-white' : 'btn-outline-secondary'}`}
            onClick={() => setTab(t)}>
            {t}
          </button>
        ))}
      </div>
      {filtered.length === 0 ? (
        <div className="empty-state">
          <h4>No orders in {tab.toLowerCase()} 📋</h4>
          <Link to="/menu" className="btn brand-btn text-white mt-3">Order now</Link>
        </div>
      ) : (
        <div className="row g-3">
          {filtered.map((o) => (
            <div key={o.id} className="col-12 col-md-6">
              <Link to={`/orders/${o.id}`} className="text-decoration-none text-dark">
                <div className="card shadow-sm p-3">
                  <div className="d-flex justify-content-between">
                    <div>
                      <h5 className="mb-1">Token {o.tokenNumber}</h5>
                      <small className="text-muted">{new Date(o.orderDate).toLocaleString()}</small>
                    </div>
                    <span className={`status-pill ${o.status}`}>{o.status}</span>
                  </div>
                  <div className="mt-2">
                    <OrderStatusTimeline current={o.status} />
                  </div>
                  <div className="d-flex justify-content-between mt-2">
                    <span>{o.items.length} items</span>
                    <span className="fw-semibold brand-text">₹{Number(o.totalAmount).toFixed(2)}</span>
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