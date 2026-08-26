import { Link, useLocation } from 'react-router-dom'
import { ShoppingCart, ArrowRight } from 'lucide-react'
import { useCart } from '../context/CartContext'
import { useAuth } from '../context/AuthContext'

export default function FloatingCart() {
  const { count, total } = useCart()
  const { isAuthenticated, user } = useAuth()
  const location = useLocation()

  if (!isAuthenticated || user?.role !== 'STUDENT') return null
  if (location.pathname === '/cart' || location.pathname === '/checkout') return null
  if (count === 0) return null

  return (
    <Link to="/cart" className="sc-fab" aria-label="Open cart">
      <div className="sc-fab-left">
        <ShoppingCart size={17} />
        <span className="sc-fab-count">{count}</span>
        <span className="sc-fab-items">{count === 1 ? 'item' : 'items'}</span>
      </div>
      <div className="sc-fab-divider" />
      <div className="sc-fab-right">
        <span className="sc-fab-total">₹{total.toFixed(0)}</span>
        <ArrowRight size={15} />
      </div>
    </Link>
  )
}
