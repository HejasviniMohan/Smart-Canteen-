import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { ArrowRight, ChefHat } from 'lucide-react'
import { motion } from 'framer-motion'
import { foodService, categoryService } from '../services/foodService'
import { orderService } from '../services/orderService'
import { useAuth } from '../context/AuthContext'
import FoodCardPremium from '../components/FoodCardPremium'
import HeroSection from '../components/HeroSection'
import StatusStrip from '../components/StatusStrip'
import SearchBar from '../components/SearchBar'
import CategoryGrid from '../components/CategoryGrid'
import SpecialCard from '../components/SpecialCard'
import ComboCarousel from '../components/ComboCarousel'
import TokenCard from '../components/TokenCard'
import HowItWorks from '../components/HowItWorks'
import FeatureSection from '../components/FeatureSection'
import Testimonials from '../components/Testimonials'
import FAQ from '../components/FAQ'
import { useCart } from '../context/CartContext'
import { useNavigate } from 'react-router-dom'
import { toast } from 'react-toastify'

export default function Home() {
  const { isAuthenticated } = useAuth()
  const { add } = useCart()
  const navigate = useNavigate()

  const [foods, setFoods] = useState([])
  const [categories, setCategories] = useState([])
  const [recentOrders, setRecentOrders] = useState([])
  const [specialIdx, setSpecialIdx] = useState(0)
  const [search, setSearch] = useState('')
  const [activeChip, setActiveChip] = useState(null)

  useEffect(() => {
    foodService.list({ available: true }).then((r) => setFoods(r.data || [])).catch(() => {})
    categoryService.list().then((r) => setCategories(r.data || [])).catch(() => {})
    if (isAuthenticated) {
      orderService.mine()
        .then((r) => setRecentOrders((r.data || []).filter((o) => o.status === 'COMPLETED').slice(0, 3)))
        .catch(() => {})
    }
  }, [isAuthenticated])

  const itemsByCategory = useMemo(() => {
    const map = {}
    foods.forEach((f) => { if (f.categoryId) map[f.categoryId] = (map[f.categoryId] || 0) + 1 })
    return map
  }, [foods])

  const todaySpecial = useMemo(() => {
    const specials = foods.filter((f) => f.isTodaySpecial && f.availability !== false)
    if (specials.length > 0) return specials[specialIdx % specials.length]
    const discounted = foods.filter((f) => f.specialPrice && Number(f.specialPrice) < Number(f.price))
    if (discounted.length > 0) return discounted[specialIdx % discounted.length]
    if (foods.length > 0) return foods[specialIdx % foods.length]
    return null
  }, [foods, specialIdx])

  useEffect(() => {
    if (!todaySpecial) return
    const t = setInterval(() => setSpecialIdx((i) => i + 1), 7000)
    return () => clearInterval(t)
  }, [todaySpecial])

  const popular = useMemo(() => {
    let items = foods
    if (search.trim()) {
      const q = search.toLowerCase()
      items = items.filter((f) => f.name.toLowerCase().includes(q))
    }
    if (activeChip) {
      const k = activeChip.toLowerCase()
      items = items.filter((f) => (f.categoryName || '').toLowerCase().includes(k) || f.name.toLowerCase().includes(k))
    }
    return items
  }, [foods, search, activeChip])

  const handleOrderAgain = (order) => {
    order.items.forEach((it) => {
      add({
        id: it.foodItemId,
        name: it.foodItemName,
        price: Number(it.subtotal) / Math.max(it.quantity, 1),
        isVeg: true,
        imageUrl: null,
      }, it.quantity)
    })
    toast.success('Items added — proceed to checkout')
    navigate('/cart')
  }

  return (
    <>
      <HeroSection />
      <StatusStrip />

      <section className="sc-section" id="search">
        <div className="container">
          <div style={{ textAlign: 'center', marginBottom: 24 }}>
            <span className="sc-eyebrow">What are you craving today?</span>
            <h2 style={{ margin: '6px 0 6px', fontSize: 'clamp(1.6rem, 2.6vw, 2.1rem)', fontWeight: 700 }}>Search, Tap, Enjoy</h2>
            <p style={{ color: 'var(--muted)', margin: 0 }}>Hundreds of items, one search away.</p>
          </div>
          <SearchBar />
        </div>
      </section>

      <section className="sc-section" id="categories">
        <div className="container">
          <div className="head">
            <div>
              <h2>Browse Categories</h2>
              <p>Pick a category to explore</p>
            </div>
            <Link to="/menu" className="more">View all <ArrowRight size={14} /></Link>
          </div>
          <CategoryGrid categories={categories} itemsByCategory={itemsByCategory} />
        </div>
      </section>

      <section className="sc-section" id="specials">
        <div className="container">
          <div className="head">
            <div>
              <h2>🔥 Today's Special</h2>
              <p>Hand-picked by our chef — limited time only</p>
            </div>
          </div>
          {todaySpecial ? (
            <SpecialCard food={todaySpecial} />
          ) : (
            <div className="empty-state" style={{ background: '#fff', borderRadius: 18, border: '1px solid var(--line)' }}>
              <ChefHat size={42} style={{ color: 'var(--muted)' }} />
              <h5 style={{ marginTop: 12 }}>No special today</h5>
              <p style={{ margin: 0 }}>Check back tomorrow for fresh deals.</p>
            </div>
          )}
        </div>
      </section>

      <section className="sc-section">
        <div className="container">
          <div className="head">
            <div>
              <h2>⭐ Popular Items</h2>
              <p>What everyone on campus is ordering right now</p>
            </div>
            <Link to="/menu" className="more">See full menu <ArrowRight size={14} /></Link>
          </div>
          {popular.length === 0 ? (
            <div className="empty-state" style={{ background: '#fff', borderRadius: 18, border: '1px solid var(--line)' }}>No items match your search.</div>
          ) : (
            <div className="sc-card-grid">
              {popular.slice(0, 8).map((f) => (
                <FoodCardPremium key={f.id} food={f} />
              ))}
            </div>
          )}
        </div>
      </section>

      <section className="sc-section">
        <div className="container">
          <div className="head">
            <div>
              <h2>🍱 Combo Meals</h2>
              <p>Curated bundles at student-friendly prices</p>
            </div>
            <Link to="/menu" className="more">All combos <ArrowRight size={14} /></Link>
          </div>
          <ComboCarousel />
        </div>
      </section>

      <section className="sc-section">
        <div className="container">
          <div className="head">
            <div>
              <h2>🎫 Your Digital Token</h2>
              <p>See exactly where your order is — in real time</p>
            </div>
          </div>
          <TokenCard />
        </div>
      </section>

      <section className="sc-section">
        <div className="container">
          <div className="head" style={{ justifyContent: 'center', textAlign: 'center', flexDirection: 'column', alignItems: 'center' }}>
            <h2>How Smart Canteen Works</h2>
            <p>Four simple steps from craving to eating</p>
          </div>
          <HowItWorks />
        </div>
      </section>

      <section className="sc-section">
        <div className="container">
          <div className="head" style={{ justifyContent: 'center', textAlign: 'center', flexDirection: 'column', alignItems: 'center' }}>
            <h2>Why Choose Smart Canteen</h2>
            <p>Built for the way students actually eat on campus</p>
          </div>
          <FeatureSection />
        </div>
      </section>

      {isAuthenticated && recentOrders.length > 0 && (
        <section className="sc-section">
          <div className="container">
            <div className="head">
              <div>
                <h2>🔄 Order Again</h2>
                <p>Your previous favourites, one tap away</p>
              </div>
              <Link to="/orders" className="more">All orders <ArrowRight size={14} /></Link>
            </div>
            <div className="sc-card-grid" style={{ gridTemplateColumns: 'repeat(3, 1fr)' }}>
              {recentOrders.map((o) => {
                const summary = o.items.map((i) => i.foodItemName).slice(0, 3).join(' + ')
                const more = o.items.length > 3 ? ` +${o.items.length - 3}` : ''
                return (
                  <motion.div
                    key={o.id}
                    className="sc-feature"
                    whileHover={{ y: -3 }}
                    style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12 }}
                  >
                    <div>
                      <div style={{ fontSize: '0.8rem', color: 'var(--muted)' }}>Token {o.tokenNumber}</div>
                      <div style={{ fontWeight: 600, marginTop: 2 }}>{summary}{more}</div>
                      <div style={{ color: 'var(--brand)', fontWeight: 800, marginTop: 4 }}>₹{Number(o.totalAmount).toFixed(0)}</div>
                    </div>
                    <button onClick={() => handleOrderAgain(o)} className="add-btn">Reorder</button>
                  </motion.div>
                )
              })}
            </div>
          </div>
        </section>
      )}

      <section className="sc-section">
        <div className="container">
          <div className="head" style={{ justifyContent: 'center', textAlign: 'center', flexDirection: 'column', alignItems: 'center' }}>
            <h2>Loved by Students</h2>
            <p>What our 1,200+ daily users have to say</p>
          </div>
          <Testimonials />
        </div>
      </section>

      <section className="sc-section">
        <div className="container">
          <div className="head" style={{ justifyContent: 'center', textAlign: 'center', flexDirection: 'column', alignItems: 'center' }}>
            <h2>Frequently Asked Questions</h2>
            <p>Everything you need to know before you order</p>
          </div>
          <FAQ />
        </div>
      </section>
    </>
  )
}
