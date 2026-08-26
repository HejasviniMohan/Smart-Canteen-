import { useEffect, useState } from 'react'
import { Link, useNavigate, useLocation } from 'react-router-dom'
import { Search, ShoppingCart, Bell, User, LogOut, UtensilsCrossed, Menu as MenuIcon, X } from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import { useCart } from '../context/CartContext'
import { notificationService } from '../services/notificationService'

const links = [
  { to: '/',       label: 'Home' },
  { to: '/menu',   label: 'Menu' },
  { to: '/specials', label: "Today's Specials" },
]

export default function PremiumNavbar() {
  const { user, isAuthenticated, logout } = useAuth()
  const { count } = useCart()
  const navigate = useNavigate()
  const location = useLocation()
  const [unread, setUnread] = useState(0)
  const [scrolled, setScrolled] = useState(false)
  const [mobileOpen, setMobileOpen] = useState(false)

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 12)
    window.addEventListener('scroll', onScroll, { passive: true })
    onScroll()
    return () => window.removeEventListener('scroll', onScroll)
  }, [])

  useEffect(() => {
    if (!isAuthenticated) return
    const fetch = () => notificationService.mine().then((r) => {
      setUnread((r.data || []).filter((n) => !n.isRead).length)
    }).catch(() => {})
    fetch()
    const t = setInterval(fetch, 20000)
    return () => clearInterval(t)
  }, [isAuthenticated])

  useEffect(() => { setMobileOpen(false) }, [location.pathname])

  const isActive = (to) => {
    if (to === '/') return location.pathname === '/'
    return location.pathname.startsWith(to)
  }

  const handleLogout = async () => { await logout(); navigate('/') }

  return (
    <nav className="sc-nav" style={scrolled ? { background: 'rgba(255,253,251,0.92)' } : undefined}>
      <div className="nav-inner">
        <Link to="/" className="brand" aria-label="Smart Canteen home">
          <span className="logo-mark"><UtensilsCrossed size={20} /></span>
          <span>Smart Canteen</span>
        </Link>

        <ul className="nav-links" role="menubar">
          {links.map((l, i) => (
            <li key={i} role="none">
              <Link
                to={l.to + (l.hash || '')}
                className={`nav-link-p ${isActive(l.to) && l.to === '/' ? 'active' : ''}`}
                role="menuitem"
              >
                {l.label}
              </Link>
            </li>
          ))}
          {isAuthenticated && user?.role === 'STUDENT' && (
            <li role="none">
              <Link to="/orders" className={`nav-link-p ${isActive('/orders') ? 'active' : ''}`} role="menuitem">My Orders</Link>
            </li>
          )}
          {['STAFF', 'ADMIN'].includes(user?.role) && (
            <li role="none"><Link to="/staff"  className="nav-link-p">Staff</Link></li>
          )}
          {user?.role === 'ADMIN' && (
            <li role="none"><Link to="/admin"  className="nav-link-p">Admin</Link></li>
          )}
        </ul>

        <div className="nav-actions">
          <button className="icon-btn hide-mobile" aria-label="Search" onClick={() => navigate('/menu')}>
            <Search size={18} />
          </button>
          {isAuthenticated && user?.role === 'STUDENT' && (
            <Link to="/cart" className="icon-btn hide-mobile" aria-label="Cart">
              <ShoppingCart size={18} />
              {count > 0 && <span className="dot">{count}</span>}
            </Link>
          )}
          {isAuthenticated && (
            <button className="icon-btn hide-mobile" aria-label="Notifications">
              <Bell size={18} />
              {unread > 0 && <span className="dot">{unread}</span>}
            </button>
          )}
          {isAuthenticated ? (
            <>
              <Link to="/profile" className="icon-btn hide-mobile" aria-label="Profile">
                <User size={18} />
              </Link>
              <button onClick={handleLogout} className="icon-btn hide-mobile" aria-label="Logout" title="Logout">
                <LogOut size={18} />
              </button>
            </>
          ) : (
            <>
              <Link to="/login"    className="btn-outline-brand hide-mobile">Login</Link>
              <Link to="/register" className="btn-filled-brand hide-mobile">Sign Up</Link>
            </>
          )}
          <button className="icon-btn burger" onClick={() => setMobileOpen(v => !v)} aria-label="Menu">
            {mobileOpen ? <X size={20} /> : <MenuIcon size={20} />}
          </button>
        </div>
      </div>

      {mobileOpen && (
        <div className="nav-mobile">
          {links.map((l, i) => (
            <Link key={i} to={l.to + (l.hash || '')} className="nav-link-p" onClick={() => setMobileOpen(false)}>
              {l.label}
            </Link>
          ))}
          {isAuthenticated && user?.role === 'STUDENT' && <Link to="/orders" className="nav-link-p">My Orders</Link>}
          {isAuthenticated ? (
            <>
              <Link to="/profile" className="nav-link-p">Profile</Link>
              <button className="nav-link-p" style={{ background: 'transparent', border: 0, textAlign: 'left' }} onClick={handleLogout}>Logout</button>
            </>
          ) : (
            <>
              <Link to="/login"    className="btn-outline-brand" style={{ textAlign: 'center' }}>Login</Link>
              <Link to="/register" className="btn-filled-brand"  style={{ textAlign: 'center' }}>Sign Up</Link>
            </>
          )}
        </div>
      )}
    </nav>
  )
}
