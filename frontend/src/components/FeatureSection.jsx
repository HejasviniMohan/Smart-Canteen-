import { CalendarClock, Radio, ShieldCheck, Timer } from 'lucide-react'
import { FEATURES } from '../data/homeData'

const ICONS = { ahead: Timer, shield: ShieldCheck, live: Radio, slots: CalendarClock }

export default function FeatureSection() {
  return (
    <div className="sc-features">
      {FEATURES.map((f) => {
        const Icon = ICONS[f.icon] || Timer
        return (
          <div key={f.title} className="sc-feature">
            <span className="ico" aria-hidden="true"><Icon size={20} strokeWidth={1.75} /></span>
            <div>
              <h3>{f.title}</h3>
              <p>{f.desc}</p>
            </div>
          </div>
        )
      })}
    </div>
  )
}
