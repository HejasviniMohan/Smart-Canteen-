import { useState } from 'react'
import { useCart } from '../context/CartContext'
import { toast } from 'react-toastify'

export default function FoodCard({ food, showCategory = true }) {
  const { items, add, updateQty } = useCart()
  const cartItem = items.find((i) => i.id === food.id)
  const qty = cartItem?.quantity || 0
  const unavailable = !food.availability
  const [imgError, setImgError] = useState(false)

  const handleAdd = () => {
    if (unavailable) return
    add(food, 1)
    toast.success(`${food.name} added to cart`)
  }

  const rating = food.rating ?? food.avgRating ?? 4.5
  const hasDiscount = food.specialPrice && Number(food.specialPrice) < Number(food.price)

  const displayPrice = hasDiscount ? Number(food.specialPrice) : Number(food.price)
  const originalPrice = hasDiscount ? Number(food.price) : null

  return (
    <div className={`sc-food-card ${unavailable ? 'unavailable' : ''}`}>
      <div className="sc-food-img">
        {food.imageUrl && !imgError ? (
          <img src={food.imageUrl} alt={food.name} onError={() => setImgError(true)} />
        ) : (
          <div className="sc-food-placeholder">{food.isVeg ? '🥗' : '🍗'}</div>
        )}
        {hasDiscount && (
          <span className="sc-food-discount">
            {Math.round((1 - Number(food.specialPrice) / Number(food.price)) * 100)}% OFF
          </span>
        )}
        <span className="sc-food-rating">⭐ {Number(rating).toFixed(1)}</span>
        {unavailable && <div className="sc-food-soldout">Sold Out</div>}
      </div>

      <div className="sc-food-body">
        <div className="sc-food-title-row">
          <h6 className="sc-food-name">{food.name}</h6>
          <span className={`sc-food-veg ${food.isVeg ? 'veg' : 'nonveg'}`} />
        </div>
        {showCategory && <div className="sc-food-category">{food.categoryName}</div>}
        <p className="sc-food-desc">{food.description || 'Freshly prepared'}</p>

        <div className="sc-food-footer">
          <div className="sc-food-price">
            {originalPrice && <span className="sc-food-price-old">₹{originalPrice.toFixed(0)}</span>}
            <span className="sc-food-price-current">₹{displayPrice.toFixed(0)}</span>
          </div>
          {unavailable ? (
            <span className="sc-food-sold-badge">Sold out</span>
          ) : qty > 0 ? (
            <div className="sc-food-qty">
              <button onClick={() => updateQty(food.id, qty - 1)}>−</button>
              <span>{qty}</span>
              <button onClick={() => updateQty(food.id, qty + 1)}>+</button>
            </div>
          ) : (
            <button className="sc-food-add-btn" onClick={handleAdd}>+ Add</button>
          )}
        </div>

        {food.prepTimeMin && (
          <div className="sc-food-prep">⏱ {food.prepTimeMin} min</div>
        )}
      </div>
    </div>
  )
}