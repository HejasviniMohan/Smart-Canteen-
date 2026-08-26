import { useState } from 'react'
import { motion } from 'framer-motion'
import { Plus, Minus, Star, Heart, Clock } from 'lucide-react'
import { toast } from 'react-toastify'
import { useCart } from '../context/CartContext'

export default function FoodCard({ food }) {
  const { items, add, updateQty } = useCart()
  const cartItem = items.find((i) => i.id === food.id)
  const qty = cartItem?.quantity || 0
  const [fav, setFav] = useState(false)
  const unavailable = !food.availability

  const mrp = Number(food.price)
  const offer = food.specialPrice && Number(food.specialPrice) < mrp ? Number(food.specialPrice) : null
  const savePct = offer ? Math.round((1 - offer / mrp) * 100) : 0
  const rating = food.rating ?? food.avgRating ?? 4.5

  const handleAdd = () => {
    if (unavailable) return
    add(food, 1)
    toast.success(`${food.name} added to cart`)
  }

  return (
    <motion.div
      className={`sc-food ${unavailable ? 'unavailable' : ''}`}
      whileHover={{ y: -4 }}
      transition={{ duration: 0.2 }}
    >
      <div className="img-box">
        {food.imageUrl
          ? <img src={food.imageUrl} alt={food.name} />
          : <span>{food.isVeg ? '🥗' : '🍗'}</span>}
        {savePct > 0 && <span className="ribbon">{savePct}% OFF</span>}
        <button
          className={`heart ${fav ? 'active' : ''}`}
          onClick={() => setFav((v) => !v)}
          aria-label="Add to favorites"
        >
          <Heart size={16} fill={fav ? '#DC2626' : 'none'} />
        </button>
        <span className="rating-badge"><Star size={11} fill="#FBBF24" stroke="#FBBF24" /> {Number(rating).toFixed(1)}</span>
      </div>
      <div className="body">
        <div className="name-row">
          <span className={`veg-dot ${food.isVeg ? '' : 'nonveg'}`} />
          <h6 className="name">{food.name}</h6>
        </div>
        <div className="cat">{food.categoryName || 'Canteen'}</div>
        <div className="row-bot">
          <div className="price">
            {offer ? (
              <>
                <span className="strike">₹{mrp}</span>₹{offer}
                <span className="save">{savePct}% off</span>
              </>
            ) : (
              <>₹{mrp}</>
            )}
          </div>
          {unavailable ? (
            <span className="sold">Sold out</span>
          ) : qty > 0 ? (
            <div className="qty">
              <button onClick={() => updateQty(food.id, qty - 1)} aria-label="Decrease"><Minus size={14} /></button>
              <span>{qty}</span>
              <button onClick={() => updateQty(food.id, qty + 1)} aria-label="Increase"><Plus size={14} /></button>
            </div>
          ) : (
            <button className="add-btn" onClick={handleAdd}>ADD</button>
          )}
        </div>
        {food.prepTimeMin && (
          <div className="prep"><Clock size={11} /> {food.prepTimeMin} min prep</div>
        )}
      </div>
    </motion.div>
  )
}
