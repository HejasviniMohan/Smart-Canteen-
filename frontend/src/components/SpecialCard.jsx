import { Link } from 'react-router-dom'
import { Star, ShoppingBag, Timer } from 'lucide-react'
import { useCart } from '../context/CartContext'
import { useAuth } from '../context/AuthContext'
import { toast } from 'react-toastify'
import { money } from '../utils/format'
import FoodImage from './FoodImage'

export default function SpecialCard({ food }) {
  const { add } = useCart()
  const { user } = useAuth()
  if (!food) return null

  const price = Number(food.effectivePrice)
  const hasDiscount = price < Number(food.price)
  const canOrder = !user || user.role === 'STUDENT'

  const handleAdd = () => {
    add(food, 1)
    toast.success(`${food.name} added to cart`)
  }

  return (
    <article className="sc-special">
      <div className="img">
        <FoodImage food={food} placeholderClass="sc-special-placeholder" />
      </div>
      <div className="copy">
        <span className="tag-special">Today's special</span>
        <h3>{food.name}</h3>
        {food.description && <p>{food.description}</p>}
        <div className="meta">
          <span className={food.isVeg ? 'veg-mark' : 'veg-mark nonveg'} role="img" aria-label={food.isVeg ? 'Vegetarian' : 'Non-vegetarian'} />
          {food.reviewCount > 0 && (
            <span><Star size={14} aria-hidden="true" /> {Number(food.averageRating).toFixed(1)} from {food.reviewCount} {food.reviewCount === 1 ? 'review' : 'reviews'}</span>
          )}
          {food.prepTimeMin && <span><Timer size={14} aria-hidden="true" /> {food.prepTimeMin} min</span>}
        </div>
        <div className="prices">
          <span className="new">{money(price)}</span>
          {hasDiscount && (
            <>
              <span className="old">{money(food.price)}</span>
              <span className="save">{Math.round((1 - price / Number(food.price)) * 100)}% off</span>
            </>
          )}
        </div>
        <div className="actions">
          {canOrder && (
            <button type="button" className="btn-primary-sc" onClick={handleAdd}>
              <ShoppingBag size={16} /> Add to cart
            </button>
          )}
          <Link to="/menu" className="btn-secondary-sc">See the full menu</Link>
        </div>
      </div>
    </article>
  )
}
