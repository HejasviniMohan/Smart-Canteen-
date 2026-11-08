import { useCart } from '../context/CartContext'
import { useAuth } from '../context/AuthContext'
import { toast } from 'react-toastify'
import { money } from '../utils/format'
import { Star, Timer } from 'lucide-react'
import FoodImage from './FoodImage'

export default function FoodCard({ food, showCategory = true }) {
  const { items, add, updateQty } = useCart()
  const { user } = useAuth()
  const qty = items.find((i) => i.id === food.id)?.quantity || 0
  const unavailable = !food.availability
  const canOrder = !user || user.role === 'STUDENT'

  const price = Number(food.effectivePrice)
  const hasDiscount = price < Number(food.price)

  const handleAdd = () => {
    add(food, 1)
    toast.success(`${food.name} added to cart`)
  }

  return (
    <div className={`sc-food-card ${unavailable ? 'unavailable' : ''}`}>
      <div className="sc-food-img">
        <FoodImage food={food} placeholderClass="sc-food-placeholder" />
        {hasDiscount && (
          <span className="sc-food-discount">{Math.round((1 - price / Number(food.price)) * 100)}% off</span>
        )}
        {food.reviewCount > 0 && (
          <span className="sc-food-rating" title={`${food.reviewCount} review${food.reviewCount === 1 ? '' : 's'}`}>
            <Star size={12} aria-hidden="true" /> {Number(food.averageRating).toFixed(1)}
          </span>
        )}
        {unavailable && <div className="sc-food-soldout">Sold Out</div>}
      </div>

      <div className="sc-food-body">
        <div className="sc-food-title-row">
          <h6 className="sc-food-name">{food.name}</h6>
          <span className={`sc-food-veg ${food.isVeg ? 'veg' : 'nonveg'}`} role="img" aria-label={food.isVeg ? 'Vegetarian' : 'Non-vegetarian'} title={food.isVeg ? 'Vegetarian' : 'Non-vegetarian'} />
        </div>
        {showCategory && <div className="sc-food-category">{food.categoryName}</div>}
        <p className="sc-food-desc">{food.description || 'Freshly prepared'}</p>

        <div className="sc-food-footer">
          <div className="sc-food-price">
            {hasDiscount && <span className="sc-food-price-old">{money(food.price)}</span>}
            <span className="sc-food-price-current">{money(price)}</span>
          </div>
          {unavailable ? (
            <span className="sc-food-sold-badge">Sold out</span>
          ) : !canOrder ? null : qty > 0 ? (
            <div className="sc-food-qty">
              <button onClick={() => updateQty(food.id, qty - 1)} aria-label={`Remove one ${food.name}`}>−</button>
              <span aria-live="polite">{qty}</span>
              <button onClick={() => updateQty(food.id, qty + 1)} aria-label={`Add one more ${food.name}`}>+</button>
            </div>
          ) : (
            <button className="sc-food-add-btn" onClick={handleAdd} aria-label={`Add ${food.name} to cart`}>Add</button>
          )}
        </div>

        {food.prepTimeMin && <div className="sc-food-prep"><Timer size={13} aria-hidden="true" /> {food.prepTimeMin} min</div>}
      </div>
    </div>
  )
}
