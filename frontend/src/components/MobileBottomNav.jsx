import { Link, useLocation } from 'react-router-dom'
import { Home, UtensilsCrossed, Receipt, ShoppingCart, User, ClipboardList } from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import { useCart } from '../context/CartContext'

export default function MobileBottomNav() {
  const { isAuthenticated, user } = useAuth()
  const { count } = useCart()
  const location = useLocation()

  const is = (p) => location.pathname === p
  const starts = (p) => location.pathname.startsWith(p)

  return (
    <nav className="sc-bottom-nav" aria-label="Primary">
      <Link to="/"        className={is('/') ? 'active' : ''}><Home size={20} />Home</Link>
      <Link to="/menu"    className={starts('/menu') ? 'active' : ''}><UtensilsCrossed size={20} />Menu</Link>
      {isAuthenticated && user?.role === 'STUDENT' ? (
        <Link to="/orders" className={starts('/orders') ? 'active' : ''}>
          <span style={{ position: 'relative' }}>
            <Receipt size={20} />
          </span>
          Orders
        </Link>
      ) : ['STAFF', 'ADMIN'].includes(user?.role) ? (
        <Link to="/staff" className={starts('/staff') ? 'active' : ''}><ClipboardList size={20} />Staff</Link>
      ) : null}
      {isAuthenticated && user?.role === 'STUDENT' && (
        <Link to="/cart" className={`cart-cell ${is('/cart') ? 'active' : ''}`}>
          <span style={{ position: 'relative' }}>
            <ShoppingCart size={20} />
            {count > 0 && <span className="badge">{count}</span>}
          </span>
          Cart
        </Link>
      )}
      <Link to={isAuthenticated ? '/profile' : '/login'} className={is('/profile') ? 'active' : ''}>
        <User size={20} />{isAuthenticated ? 'Profile' : 'Login'}
      </Link>
    </nav>
  )
}
