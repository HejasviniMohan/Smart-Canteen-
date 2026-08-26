import { Clock, ChefHat, Store, TrendingUp } from 'lucide-react'
import { useEffect, useState } from 'react'

const BASE = { open: true, waitMin: 5, preparing: 18, counters: 3, crowd: 'Low' }

export default function StatusStrip() {
  const [data, setData] = useState(BASE)

  useEffect(() => {
    const t = setInterval(() => {
      setData((d) => ({
        ...d,
        preparing: Math.max(0, d.preparing + Math.round(Math.random() * 4 - 2)),
        waitMin:   Math.max(2, Math.min(12, d.waitMin + Math.round(Math.random() * 2 - 1))),
      }))
    }, 8000)
    return () => clearInterval(t)
  }, [])

  return (
    <div className="container" style={{ maxWidth: 1320, margin: '0 auto', padding: '0 24px' }}>
      <div className="sc-status">
        <div className="item">
          <div className="ico green"><Store size={20} /></div>
          <div>
            <div className="label">Canteen Status</div>
            <div className="value"><span className="live-dot" /> Open Now</div>
          </div>
        </div>
        <div className="item">
          <div className="ico orange"><Clock size={20} /></div>
          <div>
            <div className="label">Average Wait</div>
            <div className="value">~{data.waitMin} min</div>
          </div>
        </div>
        <div className="item">
          <div className="ico purple"><ChefHat size={20} /></div>
          <div>
            <div className="label">Orders Preparing</div>
            <div className="value">{data.preparing}</div>
          </div>
        </div>
        <div className="item">
          <div className="ico amber"><TrendingUp size={20} /></div>
          <div>
            <div className="label">Counters Active</div>
            <div className="value">{data.counters}</div>
          </div>
        </div>
      </div>
    </div>
  )
}
