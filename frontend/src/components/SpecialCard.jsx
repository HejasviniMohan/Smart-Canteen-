import { Link } from 'react-router-dom'
import { motion } from 'framer-motion'
import { Star, Heart, Plus, ShoppingBag, Flame } from 'lucide-react'
import { useCart } from '../context/CartContext'
import { toast } from 'react-toastify'

export default function SpecialCard({ food }) {
  const { add } = useCart()
  if (!food) return null

  const mrp      = Number(food.price)
  const offer    = food.specialPrice ? Number(food.specialPrice) : Math.round(mrp * 0.85)
  const savePct  = Math.round((1 - offer / mrp) * 100)
  const rating   = food.rating ?? food.avgRating ?? 4.6

  const handleAdd = () => {
    add(food, 1)
    toast.success(`${food.name} added to cart`)
  }

  return (
    <motion.div
      className="sc-special"
      initial={{ opacity: 0, y: 24 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true, margin: '-80px' }}
      transition={{ duration: 0.5 }}
    >
      <div>
        <span className="tag"><Flame size={12} style={{ verticalAlign: 'middle', marginRight: 4 }} /> TODAY'S SPECIAL</span>
        <h3>{food.name}</h3>
        <p>{food.description || 'Hand-picked by our chef for today — fresh, flavorful, and at a special price for a limited time.'}</p>
        <div className="meta">
          <span className="rating"><Star size={12} fill="#fff" /> {Number(rating).toFixed(1)}</span>
          <span className="veg" />
          <span style={{ color: 'var(--muted)', fontSize: '0.85rem' }}>{food.prepTimeMin ? `⏱ ${food.prepTimeMin} min` : '⏱ 10-15 min'}</span>
        </div>
        <div className="prices">
          <span className="new">₹{offer}</span>
          <span className="old">₹{mrp}</span>
          <span className="save">{savePct}% OFF</span>
        </div>
        <div className="actions">
          <button className="btn-primary-sc" onClick={handleAdd} style={{ border: 0, cursor: 'pointer' }}>
            <ShoppingBag size={16} /> Add to Cart
          </button>
          <Link to="/menu" className="btn-secondary-sc">View Details</Link>
        </div>
      </div>
      <div className="img">
        {food.imageUrl
          ? <img src={food.imageUrl} alt={food.name} />
          : <span>{food.isVeg ? '🥗' : '🍗'}</span>}
      </div>
    </motion.div>
  )
}
