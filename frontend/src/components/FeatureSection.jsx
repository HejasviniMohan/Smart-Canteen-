import { motion } from 'framer-motion'
import { Zap, ShieldCheck, Bell, Rocket } from 'lucide-react'
import { FEATURES } from '../data/homeData'

const ICONS = { zap: Zap, shield: ShieldCheck, bell: Bell, rocket: Rocket }

export default function FeatureSection() {
  return (
    <div className="sc-features">
      {FEATURES.map((f, idx) => {
        const Icon = ICONS[f.icon] || Zap
        return (
          <motion.div
            key={idx}
            className="sc-feature"
            initial={{ opacity: 0, y: 24 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true, margin: '-50px' }}
            transition={{ duration: 0.4, delay: idx * 0.08 }}
          >
            <div className="ico" style={{ background: `linear-gradient(135deg, ${f.from} 0%, ${f.to} 100%)` }}>
              <Icon size={26} />
            </div>
            <h4>{f.title}</h4>
            <p>{f.desc}</p>
          </motion.div>
        )
      })}
    </div>
  )
}
