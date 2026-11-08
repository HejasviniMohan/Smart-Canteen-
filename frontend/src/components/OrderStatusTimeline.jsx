import { timelineSteps } from '../utils/orderStatus'

export default function OrderStatusTimeline({ current }) {
  if (current === 'CANCELLED' || current === 'PENDING_PAYMENT') return null
  return (
    <ol className="timeline" aria-label="Order progress">
      {timelineSteps(current).map(({ step, label, state }) => (
        <li key={step} className={`timeline-step ${state === 'todo' ? '' : state}`} aria-current={state === 'active' ? 'step' : undefined}>
          {label}
        </li>
      ))}
    </ol>
  )
}
