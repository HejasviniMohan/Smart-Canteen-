import { useEffect, useState } from 'react'
import { motion } from 'framer-motion'
import { Check, Loader2, Bell, MapPin } from 'lucide-react'
import { Link } from 'react-router-dom'

const STEPS = [
  { key: 'placed',    label: 'Order Confirmed',  time: '10:42 AM' },
  { key: 'preparing', label: 'Preparing',        time: '10:45 AM' },
  { key: 'ready',     label: 'Ready for Pickup', time: '~10:50 AM' },
  { key: 'completed', label: 'Completed',        time: '—' },
]

export default function TokenCard() {
  const [eta, setEta] = useState(8 * 60)
  const [idx, setIdx] = useState(1)

  useEffect(() => {
    const t = setInterval(() => setEta((e) => (e > 0 ? e - 1 : 0)), 1000)
    return () => clearInterval(t)
  }, [])

  useEffect(() => {
    const cycle = setInterval(() => setIdx((i) => (i + 1) % 4), 4000)
    return () => clearInterval(cycle)
  }, [])

  const mins = String(Math.floor(eta / 60)).padStart(2, '0')
  const secs = String(eta % 60).padStart(2, '0')
  const progress = Math.min(100, ((idx + 1) / STEPS.length) * 100)

  return (
    <motion.div
      className="sc-token"
      initial={{ opacity: 0, y: 24 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true, margin: '-80px' }}
      transition={{ duration: 0.5 }}
    >
      <div className="left">
        <h3>Your Token</h3>
        <div className="token-num">#{`A${100 + idx * 7}`}</div>
        <div className="counter">
          Estimated pickup in <strong>{mins}:{secs}</strong>
        </div>
        <div className="bar" style={{ marginTop: 14, maxWidth: 260 }}>
          <div style={{ width: `${progress}%` }} />
        </div>
        <div style={{ marginTop: 18, display: 'flex', gap: 10, flexWrap: 'wrap' }}>
          <Link to="/orders" className="btn-primary-sc" style={{ background: '#FBBF24', color: '#1E293B', boxShadow: '0 8px 20px rgba(251,191,36,0.35)' }}>
            <Bell size={16} /> Track Order
          </Link>
          <div style={{ display: 'flex', alignItems: 'center', gap: 6, color: '#94A3B8', fontSize: '0.88rem' }}>
            <MapPin size={14} /> Counter 2
          </div>
        </div>
      </div>

      <div className="right">
        <div className="timeline-vert">
          {STEPS.map((s, i) => (
            <div key={s.key} className={`ts ${i < idx ? 'done' : ''} ${i === idx ? 'active' : ''}`}>
              <div className="dot">
                {i < idx ? <Check size={16} /> : i === idx ? <Loader2 size={16} className="spin" /> : <span style={{ fontSize: 12, fontWeight: 700 }}>{i + 1}</span>}
              </div>
              <div>
                <div className="label">{s.label}</div>
                <div className="time">{s.time}</div>
              </div>
            </div>
          ))}
        </div>
      </div>

      <style>{`.spin { animation: spin 1.4s linear infinite; } @keyframes spin { to { transform: rotate(360deg); } }`}</style>
    </motion.div>
  )
}
