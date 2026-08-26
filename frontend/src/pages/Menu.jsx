import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { foodService, categoryService } from '../services/foodService'
import FoodCard from '../components/FoodCard'
import { Search, X } from 'lucide-react'

export default function Menu() {
  const { categoryId } = useParams()
  const navigate = useNavigate()
  const [foods, setFoods] = useState([])
  const [categories, setCategories] = useState([])
  const [q, setQ] = useState('')
  const [filterAvail, setFilterAvail] = useState(true)
  const [loading, setLoading] = useState(false)
  const [activeCategory, setActiveCategory] = useState(null)

  useEffect(() => {
    categoryService.list().then((r) => setCategories(r.data)).catch(() => {})
  }, [])

  useEffect(() => {
    setLoading(true)
    setActiveCategory(categoryId ? Number(categoryId) : null)
    const params = {}
    if (categoryId) params.categoryId = categoryId
    if (filterAvail) params.available = true
    foodService.list(params).then((r) => {
      let items = r.data
      if (q) items = items.filter((i) => i.name.toLowerCase().includes(q.toLowerCase()))
      setFoods(items)
    }).finally(() => setLoading(false))
  }, [categoryId, filterAvail, q])

  const selectCategory = (id) => {
    setQ('')
    if (id === null) {
      navigate('/menu')
    } else {
      navigate(`/menu/${id}`)
    }
  }

  return (
    <div className="menu-page">
      <div className="menu-header">
        <div className="menu-search-row">
          <div className="menu-search-box">
            <Search size={18} className="menu-search-icon" />
            <input
              type="text"
              className="menu-search-input"
              placeholder="Search for dosa, biryani, coffee..."
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
            <input
              type="checkbox"
              checked={filterAvail}
              onChange={(e) => setFilterAvail(e.target.checked)}
            />
            <span>Available only</span>
          </label>
        </div>

        <div className="menu-category-bar">
          <button
            className={`menu-cat-pill ${activeCategory === null ? 'active' : ''}`}
            onClick={() => selectCategory(null)}
          >
            All
          </button>
          {categories.map((c) => (
            <button
              key={c.id}
              className={`menu-cat-pill ${activeCategory === c.id ? 'active' : ''}`}
              onClick={() => selectCategory(c.id)}
            >
              {c.name}
            </button>
          ))}
        </div>
      </div>

      <div className="menu-content">
        {loading && (
          <div className="menu-loading">
            <div className="menu-loading-text">Loading menu...</div>
          </div>
        )}
        {!loading && foods.length === 0 && (
          <div className="menu-empty">
            <div className="menu-empty-icon">🔍</div>
            <h5>No items found</h5>
            <p>Try a different search or category</p>
          </div>
        )}
        {!loading && foods.length > 0 && (
          <>
            <div className="menu-results-info">
              {foods.length} item{foods.length !== 1 ? 's' : ''} found
            </div>
            <div className="menu-grid">
              {foods.map((f) => (
                <FoodCard key={f.id} food={f} showCategory={!categoryId} />
              ))}
            </div>
          </>
        )}
      </div>
    </div>
  )
}