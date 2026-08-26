import { motion } from 'framer-motion'
import { Plus } from 'lucide-react'
import { toast } from 'react-toastify'
import { useCart } from '../context/CartContext'
import { COMBOS } from '../data/homeData'

export default function ComboCarousel() {
  const { add } = useCart()

  const handleAddCombo = (c) => {
    toast.error('Combos must be ordered in-person at the canteen')
  }

  return (
    <div className="sc-carousel">
      {COMBOS.map((c, idx) => (
        <motion.div
          key={c.id}
          className="sc-combo"
          initial={{ opacity: 0, x: 30 }}
          whileInView={{ opacity: 1, x: 0 }}
          viewport={{ once: true, margin: '-50px' }}
          transition={{ duration: 0.4, delay: idx * 0.05 }}
        >
          <div className="img-box">
            <span className="save-badge">SAVE ₹{c.save}</span>
            <span>{c.emoji}</span>
          </div>
          <div className="body">
            <h6 className="name">{c.name}</h6>
            <p className="includes">Includes: {c.includes}</p>
            <div className="price-row">
              <span className="new">₹{c.price}</span>
              <span className="old">₹{c.mrp}</span>
            </div>
            <button className="add-combo" onClick={() => handleAddCombo(c)}>
              <Plus size={14} style={{ verticalAlign: 'middle', marginRight: 4 }} /> Add Combo
            </button>
          </div>
        </motion.div>
      ))}
    </div>
  )
}
