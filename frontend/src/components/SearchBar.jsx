import { Search } from 'lucide-react'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'

export default function SearchBar() {
  const [q, setQ] = useState('')
  const navigate = useNavigate()

  const submit = (e) => {
    e.preventDefault()
    navigate(q.trim() ? `/menu?q=${encodeURIComponent(q.trim())}` : '/menu')
  }

  return (
    <form className="sc-search" onSubmit={submit} role="search">
      <Search size={18} style={{ marginLeft: 14, color: 'var(--muted)' }} />
      <input
        type="text"
        placeholder="Search dosa, coffee, sandwich, juice..."
        value={q}
        onChange={(e) => setQ(e.target.value)}
        aria-label="Search food"
      />
      <button type="submit" className="submit">Search</button>
    </form>
  )
}
