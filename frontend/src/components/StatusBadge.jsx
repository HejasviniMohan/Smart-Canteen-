import { STATUS_LABEL } from '../utils/orderStatus'

export default function StatusBadge({ status }) {
  return <span className={`status-pill ${status}`}>{STATUS_LABEL[status] || status}</span>
}

export function PaymentBadge({ order }) {
  const { paymentStatus, paymentMethod } = order
  if (paymentStatus === 'PAID') return <span className="tag tag-success">Paid</span>
  if (paymentStatus === 'REFUNDED') return <span className="tag tag-neutral">Refunded</span>
  if (paymentMethod === 'CASH_ON_PICKUP') return <span className="tag tag-warning">Cash at pickup</span>
  return <span className="tag tag-danger">Not paid</span>
}
