import { motion } from 'framer-motion'
import { Link } from 'react-router-dom'
import { ArrowRight, Sparkles, Ticket, Bell, Zap, ShieldCheck, Coffee, Star } from 'lucide-react'

const HERO_FOOD_IMG = 'https://images.unsplash.com/photo-1585937421612-70a008356fbe?w=600&q=80'

export default function HeroSection() {
  return (
    <section className="sc-hero">
      <div className="container">
        <motion.div
          initial={{ opacity: 0, y: 24 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, ease: 'easeOut' }}
        >
          <span className="sc-eyebrow"><Sparkles size={14} /> Built for modern campuses</span>
          <h1>
            Skip the Queue.<br />
            <span className="accent">Order Ahead.</span>
          </h1>
          <p className="lead">
            Order meals from anywhere on campus, pay securely, receive a digital token,
            and pick up your food without waiting. Smart Canteen turns lunch break
            chaos into a 5-minute pickup.
          </p>
          <div className="cta-row">
            <Link to="/menu" className="btn-primary-sc">
              Browse Menu <ArrowRight size={18} />
            </Link>
            <Link to="/specials" className="btn-secondary-sc">
              <Sparkles size={18} /> Today's Specials
            </Link>
          </div>
          <div className="pills">
            <span className="pill"><Ticket size={14} /> Digital Token</span>
            <span className="pill"><Bell size={14} /> Live Order Tracking</span>
            <span className="pill"><ShieldCheck size={14} /> Secure Payments</span>
            <span className="pill"><Zap size={14} /> Zero Waiting Time</span>
          </div>
        </motion.div>

        <motion.div
          className="visual"
          initial={{ opacity: 0, scale: 0.96 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 0.7, delay: 0.15, ease: 'easeOut' }}
        >
          <div className="blob" />
          <div className="food-card-stack">
            <motion.div
              className="hero-plate"
              animate={{ y: [0, -10, 0] }}
              transition={{ duration: 4, repeat: Infinity, ease: 'easeInOut' }}
            >
              <img
                src={HERO_FOOD_IMG}
                alt="Delicious Indian food"
                style={{ width: '100%', height: '100%', objectFit: 'cover', borderRadius: '50%' }}
                onError={(e) => { e.target.style.display = 'none' }}
              />
            </motion.div>
            <motion.div
              className="floaty t1"
              animate={{ y: [0, -8, 0] }}
              transition={{ duration: 3.5, repeat: Infinity, ease: 'easeInOut', delay: 0.4 }}
            >
              <span className="ico"><Star size={18} fill="#16A34A" /></span>
              <div>
                <div style={{ fontSize: '0.72rem', color: 'var(--muted)', fontWeight: 500 }}>Popular</div>
                <div>South Indian</div>
              </div>
            </motion.div>
            <motion.div
              className="floaty t2"
              animate={{ y: [0, 8, 0] }}
              transition={{ duration: 3.2, repeat: Infinity, ease: 'easeInOut', delay: 0.7 }}
            >
              <span className="ico"><Coffee size={18} /></span>
              <div>
                <div style={{ fontSize: '0.72rem', color: 'var(--muted)', fontWeight: 500 }}>Avg pickup</div>
                <div>5–10 min</div>
              </div>
            </motion.div>
          </div>
        </motion.div>
      </div>
    </section>
  )
}
