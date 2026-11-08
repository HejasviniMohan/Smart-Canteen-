/** Pure cart rules, kept apart from React so they are simple to test. */

export const cartTotal = (items) => items.reduce((sum, i) => sum + i.price * i.quantity, 0)
export const cartCount = (items) => items.reduce((sum, i) => sum + i.quantity, 0)

export function addItem(items, food, quantity = 1) {
  const found = items.find((i) => i.id === food.id)
  if (found) {
    return items.map((i) => (i.id === food.id ? { ...i, quantity: i.quantity + quantity } : i))
  }
  return [...items, {
    id: food.id,
    name: food.name,
    // effectivePrice comes from the server, so the cart shows exactly what will be charged.
    price: Number(food.effectivePrice ?? food.price),
    imageUrl: food.imageUrl,
    isVeg: food.isVeg,
    quantity,
  }]
}

export function setQuantity(items, id, quantity) {
  return items.map((i) => (i.id === id ? { ...i, quantity } : i)).filter((i) => i.quantity > 0)
}

/**
 * Compares a stored cart with the current menu. The cart may be days old, so prices can
 * have changed and items can have gone. Returns the corrected cart and what changed.
 */
export function syncWithMenu(items, foods) {
  const byId = new Map(foods.map((f) => [f.id, f]))
  const removed = []
  const repriced = []
  const synced = []
  for (const item of items) {
    const food = byId.get(item.id)
    if (!food || food.availability === false) {
      removed.push(item.name)
      continue
    }
    const price = Number(food.effectivePrice)
    if (price !== item.price) repriced.push(item.name)
    synced.push({ ...item, price, name: food.name })
  }
  return { items: synced, removed, repriced }
}
