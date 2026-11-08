import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { foodService, categoryService } from '../services/foodService'
import { errorMessage } from '../services/api'
import FoodCard from '../components/FoodCard'
import { ErrorState, SkeletonGrid } from '../components/Feedback'
import { Search, SearchX, X } from 'lucide-react'

export default function Menu() {
  const { categoryId } = useParams()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [foods, setFoods] = useState([])
  const [categories, setCategories] = useState([])
  const [q, setQ] = useState(searchParams.get('q') || '')
  const [filterAvail, setFilterAvail] = useState(true)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const activeCategory = categoryId ? Number(categoryId) : null

  const load = useCallback(() => {
    setLoading(true)
    setError(null)
    const params = {}
    if (categoryId) params.categoryId = categoryId
    if (filterAvail) params.available = true
    foodService.list(params)
      .then((r) => setFoods(r.data))
      .catch((e) => setError(errorMessage(e)))
      .finally(() => setLoading(false))
  }, [categoryId, filterAvail])

  useEffect(load, [load])

  useEffect(() => {
    // The menu still works without the category bar, so this failure stays quiet.
    categoryService.list().then((r) => setCategories(r.data)).catch(() => {})
  }, [])

  // Filters on the client, so typing doesn't send a request for every keystroke.
  const visible = useMemo(() => {
    const term = q.trim().toLowerCase()
    return term ? foods.filter((f) => f.name.toLowerCase().includes(term)) : foods
  }, [foods, q])

  return (
    <div className="menu-page">
      <div className="menu-header">
        <h1>The menu</h1>
        <div className="menu-search-row">
          <div className="menu-search-box">
            <Search size={18} className="menu-search-icon" />
            <input
              type="text"
              className="menu-search-input"
              placeholder="Search for dosa, biryani, coffee..."
              aria-label="Search the menu"
              value={q}
              onChange={(e) => setQ(e.target.value)}
            />
            {q && (
              <button className="menu-search-clear" onClick={() => setQ('')} aria-label="Clear search">
                <X size={16} />
              </button>
            )}
          </div>
          <label className="menu-avail-toggle">
            <input type="checkbox" checked={filterAvail} onChange={(e) => setFilterAvail(e.target.checked)} />
            <span>Available only</span>
          </label>
        </div>

        <div className="menu-category-bar">
          <button className={`menu-cat-pill ${activeCategory === null ? 'active' : ''}`} onClick={() => navigate('/menu')}>
            All
          </button>
          {categories.map((c) => (
            <button
              key={c.id}
              className={`menu-cat-pill ${activeCategory === c.id ? 'active' : ''}`}
              onClick={() => navigate(`/menu/${c.id}`)}
            >
              {c.name}
            </button>
          ))}
        </div>
      </div>

      <div className="menu-content">
        {loading && <SkeletonGrid />}
        {!loading && error && <ErrorState message={error} onRetry={load} />}
        {!loading && !error && visible.length === 0 && (
          <div className="menu-empty">
            <div className="menu-empty-icon" aria-hidden="true"><SearchX size={40} strokeWidth={1.5} /></div>
            <h5>No items found</h5>
            <p>Try a different search or category</p>
          </div>
        )}
        {!loading && !error && visible.length > 0 && (
          <>
            <div className="menu-results-info" aria-live="polite">
              {visible.length} item{visible.length !== 1 ? 's' : ''} found
            </div>
            <div className="menu-grid">
              {visible.map((f) => <FoodCard key={f.id} food={f} showCategory={!categoryId} />)}
            </div>
          </>
        )}
      </div>
    </div>
  )
}
