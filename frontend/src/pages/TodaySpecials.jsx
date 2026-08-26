import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { foodService } from '../services/foodService'
import SpecialCard from '../components/SpecialCard'
import FoodCard from '../components/FoodCard'

export default function TodaySpecials() {
  const [foods, setFoods] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    foodService.list({ available: true, todaySpecial: true })
      .then((r) => setFoods(r.data || []))
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [])

  return (
    <div className="menu-page">
      <div className="specials-hero">
        <h1>Today's Specials</h1>
        <p>Hand-picked deals — limited time only</p>
      </div>

      {loading && (
        <div className="menu-loading"><div className="menu-loading-text">Loading today's specials...</div></div>
      )}

      {!loading && foods.length === 0 && (
        <div className="menu-empty">
          <div className="menu-empty-icon">🔥</div>
          <h5>No specials today</h5>
          <p>Check back tomorrow for fresh deals!</p>
          <Link to="/menu" className="btn brand-btn text-white mt-3">Browse Full Menu</Link>
        </div>
      )}

      {!loading && foods.length > 0 && (
        <>
          <div className="specials-featured">
            <SpecialCard food={foods[0]} />
          </div>
          {foods.length > 1 && (
            <div className="specials-grid">
              {foods.slice(1).map((f) => (
                <FoodCard key={f.id} food={f} showCategory={true} />
              ))}
            </div>
          )}
        </>
      )}
    </div>
  )
}
