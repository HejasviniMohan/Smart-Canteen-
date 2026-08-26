export default function OrderStatusTimeline({ current }) {
  const steps = ['PLACED', 'ACCEPTED', 'PREPARING', 'READY', 'COMPLETED']
  const idx = steps.indexOf(current)
  return (
    <div className="timeline">
      {steps.map((s, i) => {
        let cls = 'timeline-step'
        if (i < idx) cls += ' done'
        else if (i === idx) cls += ' active'
        return <div key={s} className={cls}>{s}</div>
      })}
    </div>
  )
}