import { AlertTriangle } from 'lucide-react'

export function ErrorState({ message, onRetry }) {
  return (
    <div className="state-box" role="alert">
      <AlertTriangle size={36} />
      <h5>Couldn't load this</h5>
      <p>{message}</p>
      {onRetry && <button className="btn btn-outline-secondary" onClick={onRetry}>Try again</button>}
    </div>
  )
}

export function Skeleton({ rows = 3, height = 72 }) {
  return (
    <div role="status" aria-label="Loading">
      {Array.from({ length: rows }).map((_, i) => (
        <div key={i} className="skeleton" style={{ height }} />
      ))}
    </div>
  )
}

export function SkeletonGrid({ count = 8 }) {
  return (
    <div className="menu-grid" role="status" aria-label="Loading">
      {Array.from({ length: count }).map((_, i) => <div key={i} className="skeleton" style={{ height: 300 }} />)}
    </div>
  )
}
