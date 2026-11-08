import { useEffect, useState } from 'react'
import { Link, useNavigate, useLocation } from 'react-router-dom'
import { Search, ShoppingCart, User, LogOut, UtensilsCrossed, Menu as MenuIcon, X } from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import { useCart } from '../context/CartContext'
import NotificationBell from './NotificationBell'
import ThemeToggle from './ThemeToggle'

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
  const [scrolled, setScrolled] = useState(false)
  const [mobileOpen, setMobileOpen] = useState(false)

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 12)
    window.addEventListener('scroll', onScroll, { passive: true })
    onScroll()
    return () => window.removeEventListener('scroll', onScroll)
  }, [])

  useEffect(() => { setMobileOpen(false) }, [location.pathname])

  const isActive = (to) => {
    if (to === '/') return location.pathname === '/'
    return location.pathname.startsWith(to)
  }

  const handleLogout = async () => { await logout(); navigate('/') }

  // On the home page the bar starts transparent over the hero photo, then turns solid on scroll.
  const overHero = location.pathname === '/' && !scrolled && !mobileOpen

  return (
    <nav className={`sc-nav ${scrolled ? 'scrolled' : ''} ${overHero ? 'over-hero' : ''}`} aria-label="Main">
      <div className="nav-inner">
        <Link to="/" className="brand" aria-label="Smart Canteen home">
          <span className="logo-mark" aria-hidden="true"><UtensilsCrossed size={18} /></span>
          <span>Smart Canteen</span>
        </Link>

        <ul className="nav-links">
          {links.map((l, i) => (
            <li key={i}>
              <Link
                to={l.to + (l.hash || '')}
                className={`nav-link-p ${isActive(l.to) ? 'active' : ''}`}
              >
                {l.label}
              </Link>
            </li>
          ))}
          {isAuthenticated && user?.role === 'STUDENT' && (
            <li>
              <Link to="/orders" className={`nav-link-p ${isActive('/orders') ? 'active' : ''}`}>My Orders</Link>
            </li>
          )}
          {['STAFF', 'ADMIN'].includes(user?.role) && (
            <li><Link to="/staff"  className="nav-link-p">Staff</Link></li>
          )}
          {user?.role === 'ADMIN' && (
            <li><Link to="/admin"  className="nav-link-p">Admin</Link></li>
          )}
        </ul>

        <div className="nav-actions">
          <button className="icon-btn hide-mobile" aria-label="Search" onClick={() => navigate('/menu')}>
            <Search size={18} />
          </button>
          {(!isAuthenticated || user?.role === 'STUDENT') && (
            <Link to="/cart" className="icon-btn hide-mobile" aria-label={`Cart, ${count} items`}>
              <ShoppingCart size={18} />
              {count > 0 && <span className="dot">{count}</span>}
            </Link>
          )}
          {isAuthenticated && (
            <NotificationBell />
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
              <Link to="/register" className="btn-filled-brand hide-mobile">Sign up</Link>
            </>
          )}
          <ThemeToggle />
          <button className="icon-btn burger" onClick={() => setMobileOpen(v => !v)} aria-label="Menu" aria-expanded={mobileOpen}>
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
          {['STAFF', 'ADMIN'].includes(user?.role) && <Link to="/staff" className="nav-link-p">Staff</Link>}
          {user?.role === 'ADMIN' && <Link to="/admin" className="nav-link-p">Admin</Link>}
          {isAuthenticated ? (
            <>
              <Link to="/profile" className="nav-link-p">Profile</Link>
              <button className="nav-link-p border-0 bg-transparent text-start" onClick={handleLogout}>Logout</button>
            </>
          ) : (
            <>
              <Link to="/login"    className="btn-outline-brand">Login</Link>
              <Link to="/register" className="btn-filled-brand">Sign up</Link>
            </>
          )}
        </div>
      )}
    </nav>
  )
}
