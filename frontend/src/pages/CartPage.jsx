import { Link } from 'react-router-dom'
import { useCart } from '../context/CartContext'

export default function CartPage() {
  const { items, updateQty, remove, total } = useCart()

  if (items.length === 0) {
    return (
      <div className="page-pad">
        <div className="empty-state">
          <h4>Your cart is empty 🛒</h4>
          <Link to="/menu" className="btn brand-btn text-white mt-3">Browse Menu</Link>
        </div>
      </div>
    )
  }

  return (
    <div className="page-pad" style={{ maxWidth: 720 }}>
      <h2 className="mb-3">Your cart</h2>
      <div className="card shadow-sm">
        <ul className="list-group list-group-flush">
          {items.map((i) => (
            <li key={i.id} className="list-group-item d-flex align-items-center">
              <span className={i.isVeg ? 'veg-badge me-2' : 'nonveg-badge me-2'}></span>
              <div className="flex-grow-1">
                <div className="fw-semibold">{i.name}</div>
                <small className="text-muted">₹{i.price}</small>
              </div>
              <div className="qty-stepper me-3">
                <button className="btn btn-outline-secondary" onClick={() => updateQty(i.id, i.quantity - 1)}>−</button>
                <span className="px-2">{i.quantity}</span>
                <button className="btn btn-outline-secondary" onClick={() => updateQty(i.id, i.quantity + 1)}>+</button>
              </div>
              <div className="fw-semibold me-3">₹{(i.price * i.quantity).toFixed(0)}</div>
              <button className="btn btn-sm btn-outline-danger" onClick={() => remove(i.id)}>✕</button>
            </li>
          ))}
        </ul>
        <div className="card-body d-flex justify-content-between align-items-center">
          <h5 className="mb-0">Total: <span className="brand-text">₹{total.toFixed(2)}</span></h5>
          <Link to="/checkout" className="btn brand-btn text-white">Proceed to Checkout →</Link>
        </div>
      </div>
    </div>
  )
}