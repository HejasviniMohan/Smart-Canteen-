/** Rupees, showing paise only when there are any: 65 -> ₹65, 65.5 -> ₹65.50. */
export function money(value) {
  const n = Number(value)
  return `₹${Number.isInteger(n) ? n : n.toFixed(2)}`
}

export function dateTime(value) {
  return value ? new Date(value).toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' }) : '-'
}

export function timeOnly(value) {
  return value ? new Date(value).toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' }) : '-'
}

/** "Today, 12:45 PM" or "Tomorrow, 9:00 AM", relative to `now`. */
export function slotLabel(value, now = new Date()) {
  const slot = new Date(value)
  const dayStart = (d) => new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime()
  const days = Math.round((dayStart(slot) - dayStart(now)) / 86400000)
  const day = days === 0 ? 'Today' : days === 1 ? 'Tomorrow'
    : slot.toLocaleDateString([], { weekday: 'short', day: 'numeric', month: 'short' })
  return `${day}, ${timeOnly(value)}`
}
