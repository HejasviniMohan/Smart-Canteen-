import { useCallback, useEffect, useState } from 'react'
import { toast } from 'react-toastify'
import { adminService } from '../services/orderService'
import { errorMessage } from '../services/api'
import { dateTime } from '../utils/format'
import AdminNav from '../components/AdminNav'
import Pager from '../components/Pager'
import { ErrorState, Skeleton } from '../components/Feedback'

export default function AdminReviews() {
  const [result, setResult] = useState(null)
  const [error, setError] = useState(null)
  const [page, setPage] = useState(0)

  const load = useCallback(() => {
    setError(null)
    adminService.reviews({ page, size: 20 })
      .then((r) => setResult(r.data))
      .catch((e) => setError(errorMessage(e)))
  }, [page])

  useEffect(load, [load])

  const remove = async (r) => {
    if (!window.confirm(`Remove this review of ${r.foodItemName}?`)) return
    try {
      await adminService.deleteReview(r.id)
      toast.success('Review removed')
      load()
    } catch (e) {
      toast.error(errorMessage(e))
    }
  }

  return (
    <div className="page-pad">
      <h1 className="h2 mb-3">Reviews</h1>
      <AdminNav />

      {error && <ErrorState message={error} onRetry={load} />}
      {!error && !result && <Skeleton rows={4} height={48} />}

      {result && (
        <>
          <div className="table-responsive">
            <table className="table">
              <thead><tr><th>Food</th><th>Rating</th><th>Comment</th><th>By</th><th>Date</th><th></th></tr></thead>
              <tbody>
                {result.items.map((r) => (
                  <tr key={r.id}>
                    <td>{r.foodItemName}</td>
                    <td aria-label={`${r.rating} out of 5`}>{'★'.repeat(r.rating)}</td>
                    <td>{r.comment || '-'}</td>
                    <td>{r.userName}</td>
                    <td>{dateTime(r.createdAt)}</td>
                    <td><button className="btn btn-sm btn-outline-danger" onClick={() => remove(r)}>Remove</button></td>
                  </tr>
                ))}
                {result.items.length === 0 && <tr><td colSpan="6" className="text-center text-muted py-4">No reviews yet.</td></tr>}
              </tbody>
            </table>
          </div>
          <Pager page={result.page} totalPages={result.totalPages} onChange={setPage} />
        </>
      )}
    </div>
  )
}
