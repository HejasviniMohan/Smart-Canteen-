import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { adminService } from '../services/orderService'
import { errorMessage } from '../services/api'
import { dateTime, money } from '../utils/format'
import AdminNav from '../components/AdminNav'
import Pager from '../components/Pager'
import { ErrorState, Skeleton } from '../components/Feedback'

const STATUSES = ['ALL', 'PENDING', 'PAID', 'FAILED', 'REFUNDED']
const BADGE = { PAID: 'tag-success', PENDING: 'tag-warning', FAILED: 'tag-danger', REFUNDED: 'tag-neutral' }

export default function AdminPayments() {
  const [result, setResult] = useState(null)
  const [error, setError] = useState(null)
  const [status, setStatus] = useState('ALL')
  const [page, setPage] = useState(0)

  const load = useCallback(() => {
    setError(null)
    adminService.payments({ page, size: 20, ...(status === 'ALL' ? {} : { status }) })
      .then((r) => setResult(r.data))
      .catch((e) => setError(errorMessage(e)))
  }, [status, page])

  useEffect(load, [load])

  return (
    <div className="page-pad">
      <h1 className="h2 mb-3">Payments</h1>
      <AdminNav />

      <div className="order-filters mb-4">
        {STATUSES.map((s) => (
          <button key={s} aria-pressed={status === s}
                  className={`filter-chip ${status === s ? 'active' : ''}`}
                  onClick={() => { setStatus(s); setPage(0) }}>
            {s === 'ALL' ? 'All' : s}
          </button>
        ))}
      </div>

      {error && <ErrorState message={error} onRetry={load} />}
      {!error && !result && <Skeleton rows={4} height={48} />}

      {result && (
        <>
          <div className="table-responsive">
            <table className="table">
              <thead>
                <tr><th>Order</th><th>Amount</th><th>Method</th><th>Provider</th><th>Status</th><th>Transaction</th><th>Date</th></tr>
              </thead>
              <tbody>
                {result.items.map((p) => (
                  <tr key={p.id}>
                    <td><Link to={`/orders/${p.orderId}`}>{p.tokenNumber}</Link></td>
                    <td>{money(p.amount)}</td>
                    <td>{p.paymentMethod}</td>
                    <td>{p.gateway}</td>
                    <td><span className={`tag ${BADGE[p.paymentStatus]}`}>{p.paymentStatus}</span></td>
                    <td className="text-truncate" style={{ maxWidth: 160 }}>{p.transactionId || '-'}</td>
                    <td>{dateTime(p.paymentDate || p.createdAt)}</td>
                  </tr>
                ))}
                {result.items.length === 0 && <tr><td colSpan="7" className="text-center text-muted py-4">No payments found.</td></tr>}
              </tbody>
            </table>
          </div>
          <Pager page={result.page} totalPages={result.totalPages} onChange={setPage} />
        </>
      )}
    </div>
  )
}
