import { Link } from 'react-router-dom'
import { ArrowRight, CalendarClock, QrCode, Radio } from 'lucide-react'
import StatusStrip from './StatusStrip'

export default function HeroSection() {
  return (
    <section className="sc-hero" aria-labelledby="hero-title">
      <div className="hero-media" aria-hidden="true">
        <img
          src="/images/hero.webp"
          srcSet="/images/hero-1200.webp 1200w, /images/hero.webp 2400w"
          sizes="100vw"
          alt=""
          fetchPriority="high"
          decoding="async"
        />
      </div>
      <div className="hero-scrim" aria-hidden="true" />

      <div className="container">
        <div>
          <p className="hero-kicker enter">The campus canteen, on your phone</p>
          <h1 id="hero-title" className="hero-title enter enter-2">Lunch, without the line.</h1>
          <p className="hero-lead enter enter-3">
            Order from anywhere on campus, choose a pickup slot, and collect your food
            with a QR code the moment it's ready.
          </p>
          <div className="hero-ctas enter enter-4">
            <Link to="/menu" className="hero-btn hero-btn-primary">Browse the menu <ArrowRight size={18} /></Link>
            <Link to="/specials" className="hero-btn hero-btn-ghost">Today's specials</Link>
          </div>
          <ul className="hero-points enter enter-5">
            <li><CalendarClock size={16} aria-hidden="true" /> Pickup slots</li>
            <li><Radio size={16} aria-hidden="true" /> Live order tracking</li>
            <li><QrCode size={16} aria-hidden="true" /> QR pickup</li>
          </ul>
        </div>
        <div className="enter enter-5">
          <StatusStrip />
        </div>
      </div>
    </section>
  )
}
