import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ShoppingBag } from 'lucide-react'
import { orderService } from '../services/orderService'
import { errorMessage } from '../services/api'
import { STATUS_LABEL } from '../utils/orderStatus'
import { dateTime, money, slotLabel } from '../utils/format'
import AdminNav from '../components/AdminNav'
import Pager from '../components/Pager'
import StatusBadge, { PaymentBadge } from '../components/StatusBadge'
import { ErrorState, Skeleton } from '../components/Feedback'

const STATUSES = ['ALL', 'PENDING_PAYMENT', 'PLACED', 'ACCEPTED', 'PREPARING', 'READY', 'COMPLETED', 'CANCELLED']

export default function AdminOrders() {
  const [result, setResult] = useState(null)
  const [error, setError] = useState(null)
  const [status, setStatus] = useState('ALL')
  const [page, setPage] = useState(0)

  const load = useCallback(() => {
    setError(null)
    orderService.all({ page, size: 20, ...(status === 'ALL' ? {} : { status }) })
      .then((r) => setResult(r.data))
      .catch((e) => setError(errorMessage(e)))
  }, [status, page])

  useEffect(load, [load])

  return (
    <div className="page-pad">
      <h1 className="h2 mb-3">Orders</h1>
      <AdminNav />

      <div className="order-filters mb-4">
        {STATUSES.map((s) => (
          <button key={s} aria-pressed={status === s}
                  className={`filter-chip ${status === s ? 'active' : ''}`}
                  onClick={() => { setStatus(s); setPage(0) }}>
            {s === 'ALL' ? 'All Orders' : STATUS_LABEL[s]}
          </button>
        ))}
      </div>

      {error && <ErrorState message={error} onRetry={load} />}
      {!error && !result && <Skeleton rows={4} height={120} />}

      {result?.items.length === 0 && (
        <div className="text-center py-5">
          <ShoppingBag size={48} style={{ color: 'var(--muted)' }} />
          <h2 className="h5 mt-3">No orders found</h2>
        </div>
      )}

      {result?.items.length > 0 && (
        <>
          <p className="text-muted">{result.totalItems} orders</p>
          <div className="admin-order-list">
            {result.items.map((o) => (
              <div key={o.id} className="admin-order-card">
                <div className="admin-order-header">
                  <div>
                    <Link to={`/orders/${o.id}`} className="fw-bold" style={{ fontSize: '1.1rem' }}>{o.tokenNumber}</Link>
                    <span className="ms-2 text-muted" style={{ fontSize: '0.85rem' }}>Order #{o.id}</span>
                  </div>
                  <StatusBadge status={o.status} />
                </div>
                <div className="admin-order-body">
                  <div className="admin-order-meta">
                    <span><strong>Customer:</strong> {o.userName}</span>
                    <span><strong>Total:</strong> <span className="brand-text fw-semibold">{money(o.totalAmount)}</span></span>
                    <span><strong>Payment:</strong> <PaymentBadge order={o} /></span>
                    <span><strong>Ordered:</strong> {dateTime(o.orderDate)}</span>
                    <span><strong>Pickup:</strong> {slotLabel(o.pickupSlot)}</span>
                  </div>
                  <div className="admin-order-items">
                    {o.items.map((item) => (
                      <span key={item.id} className="order-item-chip">{item.foodItemName} × {item.quantity}</span>
                    ))}
                  </div>
                </div>
              </div>
            ))}
          </div>
          <Pager page={result.page} totalPages={result.totalPages} onChange={setPage} />
        </>
      )}
    </div>
  )
}
