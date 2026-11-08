import { Link } from 'react-router-dom'
import { useCart } from '../context/CartContext'
import { money } from '../utils/format'

export default function CartPage() {
  const { items, updateQty, remove, total } = useCart()

  if (items.length === 0) {
    return (
      <div className="page-pad">
        <div className="empty-state">
          <h4>Your cart is empty</h4>
          <Link to="/menu" className="btn brand-btn mt-3">Browse Menu</Link>
        </div>
      </div>
    )
  }

  return (
    <div className="page-pad" style={{ maxWidth: 720, margin: '0 auto' }}>
      <h1 className="h2 mb-3">Your cart</h1>
      <div className="card shadow-sm">
        <ul className="list-group list-group-flush">
          {items.map((i) => (
            <li key={i.id} className="list-group-item cart-row">
              <span className={i.isVeg ? 'veg-badge' : 'nonveg-badge'} />
              <div className="cart-row-name">
                <div className="fw-semibold">{i.name}</div>
                <small className="text-muted">{money(i.price)} each</small>
              </div>
              <div className="qty-stepper">
                <button className="btn btn-outline-secondary" onClick={() => updateQty(i.id, i.quantity - 1)} aria-label={`Remove one ${i.name}`}>−</button>
                <span className="px-2" aria-live="polite">{i.quantity}</span>
                <button className="btn btn-outline-secondary" onClick={() => updateQty(i.id, i.quantity + 1)} aria-label={`Add one more ${i.name}`} disabled={i.quantity >= 50}>+</button>
              </div>
              <div className="fw-semibold cart-row-price">{money(i.price * i.quantity)}</div>
              <button className="btn btn-sm btn-outline-danger" onClick={() => remove(i.id)} aria-label={`Remove ${i.name} from cart`}>✕</button>
            </li>
          ))}
        </ul>
        <div className="card-body d-flex justify-content-between align-items-center flex-wrap gap-2">
          <h2 className="h5 mb-0">Total: <span className="brand-text">{money(total)}</span></h2>
          <Link to="/checkout" className="btn brand-btn">Proceed to Checkout →</Link>
        </div>
      </div>
    </div>
  )
}
