import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ChefHat } from 'lucide-react'
import { foodService } from '../services/foodService'
import { errorMessage } from '../services/api'
import { ErrorState, SkeletonGrid } from '../components/Feedback'
import SpecialCard from '../components/SpecialCard'
import FoodCard from '../components/FoodCard'

export default function TodaySpecials() {
  const [foods, setFoods] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const load = useCallback(() => {
    setLoading(true)
    setError(null)
    foodService.list({ available: true, todaySpecial: true })
      .then((r) => setFoods(r.data || []))
      .catch((e) => setError(errorMessage(e)))
      .finally(() => setLoading(false))
  }, [])

  useEffect(load, [load])

  return (
    <div className="menu-page">
      <div className="specials-hero">
        <h1>Today's Specials</h1>
        <p>Picked by the canteen for today</p>
      </div>

      {loading && (
        <SkeletonGrid count={4} />
      )}

      {!loading && error && <ErrorState message={error} onRetry={load} />}

      {!loading && !error && foods.length === 0 && (
        <div className="menu-empty">
          <div className="menu-empty-icon" aria-hidden="true"><ChefHat size={40} strokeWidth={1.5} /></div>
          <h5>No specials today</h5>
          <p>Check back tomorrow for fresh deals!</p>
          <Link to="/menu" className="btn brand-btn mt-3">Browse Full Menu</Link>
        </div>
      )}

      {!loading && !error && foods.length > 0 && (
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
