import { Link } from 'react-router-dom'
import { ArrowUpRight } from 'lucide-react'
import { motion } from 'framer-motion'

const ICONS = {
  'south indian': '🍛', 'north indian': '🥘', 'chinese': '🥡',
  'snacks': '🥪', 'beverages': '☕', 'desserts': '🍨',
  'combos': '🍱', 'healthy': '🥗', 'rice': '🍚',
  'breakfast': '🍳', 'default': '🍽️',
}

function emojiFor(name = '') {
  const k = name.toLowerCase()
  for (const key of Object.keys(ICONS)) if (k.includes(key)) return ICONS[key]
  return ICONS.default
}

export default function CategoryGrid({ categories = [], itemsByCategory = {} }) {
  return (
    <div className="sc-cat-grid">
      {categories.map((c, idx) => (
        <motion.div
          key={c.id}
          initial={{ opacity: 0, y: 18 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, margin: '-50px' }}
          transition={{ duration: 0.4, delay: idx * 0.04 }}
        >
          <Link to={`/menu/${c.id}`} className="sc-cat">
            <div className="img-wrap">{c.imageUrl ? <img src={c.imageUrl} alt={c.name} /> : emojiFor(c.name)}</div>
            <div className="name">{c.name}</div>
            <div className="count">{itemsByCategory[c.id] ?? c.itemCount ?? 12}+ items</div>
            <div className="arrow"><ArrowUpRight size={14} /></div>
          </Link>
        </motion.div>
      ))}
    </div>
  )
}
