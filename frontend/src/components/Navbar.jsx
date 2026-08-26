import { Link, useNavigate, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useCart } from '../context/CartContext'
import { useEffect, useState } from 'react'
import { notificationService } from '../services/notificationService'

export default function Navbar() {
  const { user, isAuthenticated, logout } = useAuth()
  const { count } = useCart()
  const navigate = useNavigate()
  const location = useLocation()
  const [unread, setUnread] = useState(0)
  const [notifs, setNotifs] = useState([])
  const [showNotifs, setShowNotifs] = useState(false)

  useEffect(() => {
    if (!isAuthenticated) return
    const fetch = () => notificationService.mine().then((r) => {
      setNotifs(r.data || [])
      setUnread((r.data || []).filter((n) => !n.isRead).length)
    }).catch(() => {})
    fetch()
    const t = setInterval(fetch, 15000)
    return () => clearInterval(t)
  }, [isAuthenticated])

  const handleLogout = async () => { await logout(); navigate('/') }

  const handleSearchClick = () => {
    if (location.pathname === '/') {
      const el = document.querySelector('.search-bar input')
      if (el) { el.focus(); el.scrollIntoView({ behavior: 'smooth', block: 'center' }) }
    } else {
      navigate('/')
      setTimeout(() => {
        const el = document.querySelector('.search-bar input')
        if (el) el.focus()
      }, 200)
    }
  }

  return (
    <nav className="navbar navbar-expand-lg bg-white border-bottom shadow-sm sticky-top">
      <div className="container">
        <Link className="navbar-brand d-flex align-items-center" to="/">
          <span style={{fontSize:'1.6rem'}}>🍱</span>
          <span className="brand-text ms-1">Smart Canteen</span>
        </Link>
        <button className="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#nav">
          <span className="navbar-toggler-icon"></span>
        </button>
        <div className="collapse navbar-collapse" id="nav">
          <ul className="navbar-nav me-auto">
            <li className="nav-item"><Link className="nav-link" to="/">Home</Link></li>
            <li className="nav-item"><Link className="nav-link" to="/menu">Menu</Link></li>
            {user?.role === 'STUDENT' && <li className="nav-item"><Link className="nav-link" to="/orders">My Orders</Link></li>}
            {['STAFF','ADMIN'].includes(user?.role) && <li className="nav-item"><Link className="nav-link" to="/staff">Staff</Link></li>}
            {user?.role === 'ADMIN' && <li className="nav-item"><Link className="nav-link" to="/admin">Admin</Link></li>}
          </ul>
          <ul className="navbar-nav ms-auto align-items-lg-center">
            <li className="nav-item">
              <button className="nav-search-btn" onClick={handleSearchClick} title="Search">🔍</button>
            </li>
            {isAuthenticated && user?.role === 'STUDENT' && (
              <li className="nav-item">
                <Link className="nav-link position-relative" to="/cart" title="Cart">
                  🛒
                  {count > 0 && <span className="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger" style={{fontSize: '0.65rem'}}>{count}</span>}
                </Link>
              </li>
            )}
            {isAuthenticated && (
              <li className="nav-item position-relative">
                <button className="nav-link btn btn-link position-relative" onClick={() => setShowNotifs((v) => !v)}>
                  🔔
                  {unread > 0 && <span className="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger">{unread}</span>}
                </button>
                {showNotifs && (
                  <div className="dropdown-menu show p-2" style={{ right: 0, left: 'auto', minWidth: 280, maxHeight: 400, overflow: 'auto', position: 'absolute' }}>
                    <h6 className="dropdown-header">Notifications</h6>
                    {notifs.length === 0 && <div className="text-muted small p-2">No notifications</div>}
                    {notifs.map((n) => (
                      <div key={n.id} className={`p-2 small border-bottom ${n.isRead ? 'text-muted' : 'fw-semibold'}`}>
                        <div>{n.title}</div>
                        <div className="text-muted" style={{fontSize:'0.75rem'}}>{n.message}</div>
                      </div>
                    ))}
                  </div>
                )}
              </li>
            )}
            {isAuthenticated ? (
              <>
                <li className="nav-item"><Link className="nav-link" to="/profile">👤 {user.name}</Link></li>
                <li className="nav-item"><button className="btn btn-link nav-link" onClick={handleLogout}>Logout</button></li>
              </>
            ) : (
              <>
                <li className="nav-item"><Link className="nav-link" to="/login">Login</Link></li>
                <li className="nav-item"><Link className="btn brand-btn text-white ms-2" to="/register">Sign up</Link></li>
              </>
            )}
          </ul>
        </div>
      </div>
    </nav>
  )
}