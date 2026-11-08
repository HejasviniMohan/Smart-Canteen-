import { Link } from 'react-router-dom'
import { UtensilsCrossed } from 'lucide-react'

const YEAR = new Date().getFullYear()

const LINKS = [
  { label: 'Full menu', to: '/menu' },
  { label: "Today's specials", to: '/specials' },
  { label: 'My orders', to: '/orders' },
  { label: 'Create an account', to: '/register' },
]

export default function Footer() {
  return (
    <footer className="sc-footer">
      <div className="container">
        <div className="grid">
          <div>
            <div className="brand">
              <span className="logo-mark" aria-hidden="true"><UtensilsCrossed size={18} /></span>
              Smart Canteen
            </div>
            <p className="about">Order ahead, choose a pickup slot, and collect with a QR code.</p>
          </div>
          <nav aria-label="Footer">
            <h2>Explore</h2>
            <ul>
              {LINKS.map((l) => <li key={l.label}><Link to={l.to}>{l.label}</Link></li>)}
            </ul>
          </nav>
        </div>
        <div className="bottom">© {YEAR} Smart Canteen</div>
      </div>
    </footer>
  )
}
