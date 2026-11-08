import { Plus } from 'lucide-react'
import { toast } from 'react-toastify'
import { useCart } from '../context/CartContext'
import { useAuth } from '../context/AuthContext'
import { money } from '../utils/format'
import FoodImage from './FoodImage'

/** Combos are ordinary menu items in the Combos category, so they order like anything else. */
export default function ComboCarousel({ combos }) {
  const { add } = useCart()
  const { user } = useAuth()
  const canOrder = !user || user.role === 'STUDENT'

  return (
    <div className="sc-carousel">
      {combos.map((c) => (
        <article key={c.id} className="sc-combo">
          <div className="combo-img"><FoodImage food={c} placeholderClass="sc-food-placeholder" /></div>
          <div className="combo-body">
            <h3 className="name">{c.name}</h3>
            <p className="includes">{c.description}</p>
            <div className="price-row">{money(c.effectivePrice)}</div>
            {canOrder && (
              <button
                type="button"
                className="add-combo"
                onClick={() => { add(c, 1); toast.success(`${c.name} added to cart`) }}
                aria-label={`Add ${c.name} to cart`}
              >
                <Plus size={15} /> Add combo
              </button>
            )}
          </div>
        </article>
      ))}
    </div>
  )
}
