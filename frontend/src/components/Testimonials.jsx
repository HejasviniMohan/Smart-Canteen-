import { motion } from 'framer-motion'
import { Star } from 'lucide-react'
import { TESTIMONIALS } from '../data/homeData'

function initials(name) {
  return name.split(' ').map((s) => s[0]).slice(0, 2).join('').toUpperCase()
}

export default function Testimonials() {
  return (
    <div className="sc-carousel">
      {TESTIMONIALS.map((t, idx) => (
        <motion.div
          key={t.id}
          className="sc-review"
          initial={{ opacity: 0, x: 30 }}
          whileInView={{ opacity: 1, x: 0 }}
          viewport={{ once: true, margin: '-50px' }}
          transition={{ duration: 0.4, delay: idx * 0.05 }}
        >
          <div className="top">
            <div className="avatar">{initials(t.name)}</div>
            <div>
              <div className="name">{t.name}</div>
              <div className="dept">{t.dept}</div>
            </div>
          </div>
          <div className="stars">
            {Array.from({ length: 5 }).map((_, i) => (
              <Star key={i} size={14} fill={i < t.rating ? '#FBBF24' : 'none'} stroke={i < t.rating ? '#FBBF24' : '#CBD5E1'} />
            ))}
          </div>
          <p className="text">"{t.text}"</p>
        </motion.div>
      ))}
    </div>
  )
}
