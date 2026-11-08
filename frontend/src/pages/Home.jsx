import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ArrowRight, ChefHat } from 'lucide-react'
import { toast } from 'react-toastify'
import { foodService, categoryService } from '../services/foodService'
import { orderService } from '../services/orderService'
import { errorMessage } from '../services/api'
import { useAuth } from '../context/AuthContext'
import { useCart } from '../context/CartContext'
import { money } from '../utils/format'
import FoodCard from '../components/FoodCard'
import HeroSection from '../components/HeroSection'
import SearchBar from '../components/SearchBar'
import CategoryGrid from '../components/CategoryGrid'
import SpecialCard from '../components/SpecialCard'
import ComboCarousel from '../components/ComboCarousel'
import HowItWorks from '../components/HowItWorks'
import FeatureSection from '../components/FeatureSection'
import FAQ from '../components/FAQ'
import { ErrorState, SkeletonGrid } from '../components/Feedback'
import Reveal from '../components/Reveal'

export default function Home() {
  const { user } = useAuth()
  const { add } = useCart()
  const navigate = useNavigate()

  const [foods, setFoods] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [categories, setCategories] = useState([])
  const [recentOrders, setRecentOrders] = useState([])
  const [specialIdx, setSpecialIdx] = useState(0)

  const isStudent = user?.role === 'STUDENT'

  const load = () => {
    setLoading(true)
    setError(null)
    foodService.list({ available: true })
      .then((r) => setFoods(r.data || []))
      .catch((e) => setError(errorMessage(e)))
      .finally(() => setLoading(false))
    categoryService.list().then((r) => setCategories(r.data || [])).catch(() => {})
  }

  useEffect(load, [])

  useEffect(() => {
    if (!isStudent) return
    // "Order again" is a convenience, so the page works fine without it.
    orderService.mine()
      .then((r) => setRecentOrders((r.data || []).filter((o) => o.status === 'COMPLETED').slice(0, 3)))
      .catch(() => {})
  }, [isStudent])

  const itemsByCategory = useMemo(() => {
    const map = {}
    foods.forEach((f) => { map[f.categoryId] = (map[f.categoryId] || 0) + 1 })
    return map
  }, [foods])

  // One photo per category for the category tiles: its first dish that has one.
  const coverByCategory = useMemo(() => {
    const map = {}
    foods.forEach((f) => { if (f.imageUrl && !map[f.categoryId]) map[f.categoryId] = f.imageUrl })
    return map
  }, [foods])

  const specials = useMemo(() => foods.filter((f) => f.isTodaySpecial), [foods])
  const combos = useMemo(() => foods.filter((f) => f.categoryName === 'Combos'), [foods])

  // Sorted by real ratings. Items with no reviews yet come last.
  const topRated = useMemo(() => [...foods]
    .filter((f) => f.categoryName !== 'Combos')
    .sort((a, b) => (b.averageRating ?? 0) - (a.averageRating ?? 0) || (b.reviewCount ?? 0) - (a.reviewCount ?? 0))
    .slice(0, 8), [foods])

  useEffect(() => {
    if (specials.length < 2) return undefined
    const t = setInterval(() => setSpecialIdx((i) => i + 1), 7000)
    return () => clearInterval(t)
  }, [specials.length])

  const handleOrderAgain = (order) => {
    const menu = new Map(foods.map((f) => [f.id, f]))
    const missing = []
    order.items.forEach((it) => {
      const food = menu.get(it.foodItemId)
      if (food) add(food, it.quantity)
      else missing.push(it.foodItemName)
    })
    if (missing.length > 0) toast.warn(`Not available right now: ${missing.join(', ')}`)
    else toast.success('Items added to your cart')
    navigate('/cart')
  }

  return (
    <>
      <HeroSection />

      <section className="sc-section" id="search">
        <Reveal className="container">
          <div className="head centered">
            <h2>What are you hungry for?</h2>
          </div>
          <SearchBar />
        </Reveal>
      </section>

      <section className="sc-section" id="categories">
        <div className="container">
          <Reveal className="head">
            <div>
              <h2>Categories</h2>
              <p>Pick a category to explore.</p>
            </div>
            <Link to="/menu" className="more">View all <ArrowRight size={14} /></Link>
          </Reveal>
          <CategoryGrid categories={categories} itemsByCategory={itemsByCategory} coverByCategory={coverByCategory} />
        </div>
      </section>

      <section className="sc-section" id="specials">
        <Reveal className="container">
          {specials.length > 0 ? (
            <SpecialCard food={specials[specialIdx % specials.length]} />
          ) : !loading && (
            <div className="empty-state empty-panel">
              <ChefHat size={40} strokeWidth={1.5} aria-hidden="true" />
              <h5>No special today</h5>
              <p className="mb-0">The kitchen hasn't picked one yet. Check the full menu instead.</p>
            </div>
          )}
        </Reveal>
      </section>

      <section className="sc-section">
        <div className="container">
          <Reveal className="head">
            <div>
              <h2>Top rated</h2>
              <p>Rated by students who ordered them.</p>
            </div>
            <Link to="/menu" className="more">See the full menu <ArrowRight size={14} /></Link>
          </Reveal>
          {loading && <SkeletonGrid count={4} />}
          {!loading && error && <ErrorState message={error} onRetry={load} />}
          {!loading && !error && (
            <Reveal stagger className="menu-grid">
              {topRated.map((f) => <FoodCard key={f.id} food={f} />)}
            </Reveal>
          )}
        </div>
      </section>

      {combos.length > 0 && (
        <section className="sc-section">
          <div className="container">
            <Reveal className="head">
              <div>
                <h2>Combo meals</h2>
                <p>Value meals from the menu.</p>
              </div>
            </Reveal>
            <Reveal>
              <ComboCarousel combos={combos} />
            </Reveal>
          </div>
        </section>
      )}

      {recentOrders.length > 0 && (
        <section className="sc-section">
          <div className="container">
            <Reveal className="head">
              <div>
                <h2>Order again</h2>
                <p>Your recent orders, one tap away.</p>
              </div>
              <Link to="/orders" className="more">All orders <ArrowRight size={14} /></Link>
            </Reveal>
            <Reveal stagger className="order-again-grid">
              {recentOrders.map((o) => (
                <div key={o.id} className="sc-feature order-again">
                  <div>
                    <div className="oa-token">Token <span className="token">{o.tokenNumber}</span></div>
                    <div className="oa-items">{o.items.map((i) => i.foodItemName).slice(0, 3).join(' + ')}</div>
                    <div className="oa-total">{money(o.totalAmount)}</div>
                  </div>
                  <button type="button" onClick={() => handleOrderAgain(o)} className="add-btn">Reorder</button>
                </div>
              ))}
            </Reveal>
          </div>
        </section>
      )}

      <section className="sc-section">
        <div className="container">
          <Reveal className="head">
            <div>
              <h2>How it works</h2>
              <p>Four steps from craving to eating.</p>
            </div>
          </Reveal>
          <Reveal>
            <HowItWorks />
          </Reveal>
        </div>
      </section>

      <section className="sc-section">
        <div className="container">
          <Reveal className="head">
            <div>
              <h2>Why order ahead</h2>
            </div>
          </Reveal>
          <Reveal>
            <FeatureSection />
          </Reveal>
        </div>
      </section>

      <section className="sc-section">
        <Reveal className="container">
          <div className="head centered">
            <h2>Questions, answered</h2>
          </div>
          <FAQ />
        </Reveal>
      </section>
    </>
  )
}
