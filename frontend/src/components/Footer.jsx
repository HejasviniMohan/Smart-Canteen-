import { Link } from 'react-router-dom'
import { UtensilsCrossed, Mail, Phone, MapPin, Globe, MessageCircle, Send } from 'lucide-react'

const COLS = [
  { title: 'Smart Canteen', links: [
    { label: 'About Us',    to: '/' },
    { label: 'Our Story',   to: '/' },
    { label: 'Careers',     to: '/' },
    { label: 'Press Kit',   to: '/' },
  ]},
  { title: 'Menu', links: [
    { label: 'All Items',       to: '/menu' },
    { label: 'Today\'s Specials', to: '/menu#specials' },
    { label: 'Combos',          to: '/menu' },
    { label: 'Beverages',       to: '/menu' },
  ]},
  { title: 'Support', links: [
    { label: 'Help Center', to: '/' },
    { label: 'Contact Us',  to: '/' },
    { label: 'Privacy',     to: '/' },
    { label: 'Terms',       to: '/' },
  ]},
  { title: 'Students', links: [
    { label: 'My Orders',     to: '/orders' },
    { label: 'Track Order',   to: '/orders' },
    { label: 'Refer Friends', to: '/' },
    { label: 'Student Offers', to: '/' },
  ]},
]

export default function Footer() {
  return (
    <footer className="sc-footer">
      <div className="container">
        <div className="grid">
          <div>
            <div className="brand">
              <span style={{ width: 36, height: 36, borderRadius: 10, background: 'linear-gradient(135deg, var(--brand) 0%, #FB923C 100%)', display: 'inline-flex', alignItems: 'center', justifyContent: 'center' }}>
                <UtensilsCrossed size={20} color="#fff" />
              </span>
              Smart Canteen
            </div>
            <p className="about">
              The smart way to eat on campus. Skip queues, get a digital token,
              and pick up your meal fresh — all in under 5 minutes.
            </p>
            <div className="socials">
              <a href="#" aria-label="Instagram"><Globe size={16} /></a>
              <a href="#" aria-label="Twitter"><Send size={16} /></a>
              <a href="#" aria-label="Facebook"><Globe size={16} /></a>
              <a href="#" aria-label="LinkedIn"><MessageCircle size={16} /></a>
            </div>
          </div>
          {COLS.map((col) => (
            <div key={col.title}>
              <h5>{col.title}</h5>
              <ul>
                {col.links.map((l) => (
                  <li key={l.label}><Link to={l.to}>{l.label}</Link></li>
                ))}
              </ul>
            </div>
          ))}
          <div>
            <h5>Get in Touch</h5>
            <div className="contact-item"><Mail size={16} /> hello@smartcanteen.app</div>
            <div className="contact-item"><Phone size={16} /> +91 80 1234 5678</div>
            <div className="contact-item"><MapPin size={16} /> Main Canteen, Block A, Campus</div>
          </div>
        </div>
        <div className="bottom">
          <div>© {new Date().getFullYear()} Smart Canteen. Crafted for hungry students.</div>
          <div>Made with ☕ on campus</div>
        </div>
      </div>
    </footer>
  )
}
