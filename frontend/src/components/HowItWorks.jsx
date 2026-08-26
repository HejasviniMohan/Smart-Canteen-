import { motion } from 'framer-motion'
import { Search, ShoppingBag, Ticket, Coffee } from 'lucide-react'

const STEPS = [
  { icon: Search,       title: 'Browse Menu',    desc: 'Explore daily specials, combos, and your favourites.' },
  { icon: ShoppingBag,  title: 'Place Order',    desc: 'Customize, add to cart, and pay securely in seconds.' },
  { icon: Ticket,       title: 'Get Token',      desc: 'Receive a digital token + QR instantly after payment.' },
  { icon: Coffee,       title: 'Pickup Meal',    desc: 'Show token at counter and walk out with hot food.' },
]

export default function HowItWorks() {
  return (
    <div className="sc-how">
      {STEPS.map((s, idx) => {
        const Icon = s.icon
        return (
          <motion.div
            key={idx}
            className="step"
            initial={{ opacity: 0, y: 24 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true, margin: '-50px' }}
            transition={{ duration: 0.4, delay: idx * 0.1 }}
          >
            <div className="circle">
              <Icon size={28} />
              <span className="num">{idx + 1}</span>
            </div>
            <h4>{s.title}</h4>
            <p>{s.desc}</p>
          </motion.div>
        )
      })}
    </div>
  )
}
