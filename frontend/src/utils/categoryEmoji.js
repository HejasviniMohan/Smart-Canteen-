const map = {
  'south indian': '🍛',
  'north indian': '🥘',
  'snacks': '🥪',
  'beverages': '☕',
  'drinks': '🥤',
  'desserts': '🍨',
  'sweets': '🍮',
  'chinese': '🥡',
  'fast food': '🍔',
  'breakfast': '🍳',
  'rice': '🍚',
  'biryani': '🍗',
  'pizza': '🍕',
  'burger': '🍔',
  'pasta': '🍝',
  'salad': '🥗',
  'soup': '🍲',
  'ice cream': '🍦',
  'bakery': '🥐',
  'default': '🍽️',
}

export function categoryEmoji(name = '') {
  const key = name.toLowerCase().trim()
  for (const k of Object.keys(map)) {
    if (key.includes(k)) return map[k]
  }
  return map.default
}

export function categoryHint(name = '') {
  const key = name.toLowerCase()
  if (key.includes('south')) return 'Dosa, Idli, Vada & more'
  if (key.includes('north')) return 'Roti, Paneer, Rice & more'
  if (key.includes('snack')) return 'Samosa, Sandwich & more'
  if (key.includes('beverage') || key.includes('drink')) return 'Tea, Coffee, Juice'
  if (key.includes('dessert') || key.includes('sweet')) return 'Ice Cream, Gulab Jamun'
  if (key.includes('chinese')) return 'Noodles, Manchurian'
  if (key.includes('breakfast')) return 'Idli, Pongal, Upma'
  return 'Explore items'
}
