import { Search, Mic, SlidersHorizontal, ArrowRight } from 'lucide-react'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'

const CHIPS = ['South Indian', 'North Indian', 'Snacks', 'Beverages', 'Desserts', 'Combos', 'Healthy']

export default function SearchBar({ categories = [] }) {
  const [q, setQ] = useState('')
  const [active, setActive] = useState(null)
  const navigate = useNavigate()

  const submit = (e) => {
    e?.preventDefault?.()
    const params = new URLSearchParams()
    if (q) params.set('q', q)
    if (active) params.set('cat', active)
    navigate(`/menu${params.toString() ? `?${params}` : ''}`)
  }

  return (
    <div>
      <form className="sc-search" onSubmit={submit}>
        <Search size={18} style={{ marginLeft: 14, color: 'var(--muted)' }} />
        <input
          type="text"
          placeholder="Search dosa, coffee, sandwich, juice..."
          value={q}
          onChange={(e) => setQ(e.target.value)}
          aria-label="Search food"
        />
        <button type="button" className="ic" aria-label="Voice search"><Mic size={18} /></button>
        <button type="button" className="ic" aria-label="Filters"><SlidersHorizontal size={18} /></button>
        <button type="submit" className="submit">Search</button>
      </form>
      <div className="sc-chips">
        {CHIPS.map((c) => (
          <button
            key={c}
            className={`sc-chip ${active === c ? 'active' : ''}`}
            onClick={() => setActive(active === c ? null : c)}
          >
            {c}
          </button>
        ))}
      </div>
    </div>
  )
}
