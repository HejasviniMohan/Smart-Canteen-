import { Link } from 'react-router-dom'
import Reveal from './Reveal'
import { Coffee, CookingPot, Croissant, Egg, IceCreamCone, Package, Salad, Sandwich, Soup, UtensilsCrossed, Wheat } from 'lucide-react'

const ICONS = [
  ['south indian', Soup], ['north indian', CookingPot], ['chinese', Wheat],
  ['snack', Sandwich], ['beverage', Coffee], ['drink', Coffee], ['dessert', IceCreamCone],
  ['combo', Package], ['healthy', Salad], ['salad', Salad], ['rice', Wheat],
  ['breakfast', Egg], ['bakery', Croissant],
]

function iconFor(name = '') {
  const k = name.toLowerCase()
  return ICONS.find(([key]) => k.includes(key))?.[1] || UtensilsCrossed
}

export default function CategoryGrid({ categories = [], itemsByCategory = {}, coverByCategory = {} }) {
  return (
    <Reveal stagger className="sc-cat-grid">
      {categories.map((c) => {
        const Icon = iconFor(c.name)
        const count = itemsByCategory[c.id] ?? 0
        // A category's own image, or else a photo of one of its dishes.
        const cover = c.imageUrl || coverByCategory[c.id]
        return (
          <Link key={c.id} to={`/menu/${c.id}`} className={`sc-cat ${cover ? 'has-cover' : ''}`}>
            {cover && <img className="cat-cover" src={cover} alt="" loading="lazy" decoding="async" />}
            {!cover && (
              <span className="cat-icon" aria-hidden="true"><Icon size={20} strokeWidth={1.75} /></span>
            )}
            <span>
              <span className="name d-block">{c.name}</span>
              <span className="count">{count} {count === 1 ? 'item' : 'items'}</span>
            </span>
          </Link>
        )
      })}
    </Reveal>
  )
}
