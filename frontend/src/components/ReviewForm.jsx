import { useState } from 'react'
import { toast } from 'react-toastify'
import { Star } from 'lucide-react'
import { reviewService } from '../services/orderService'
import { errorMessage } from '../services/api'

function ReviewRow({ orderId, item, existing, onSaved }) {
  const [rating, setRating] = useState(0)
  const [comment, setComment] = useState('')
  const [saving, setSaving] = useState(false)

  if (existing) {
    return (
      <div className="review-row">
        <strong>{item.foodItemName}</strong>
        <span className="review-stars" aria-label={`You rated this ${existing.rating} out of 5`}>
          {'★'.repeat(existing.rating)}{'☆'.repeat(5 - existing.rating)}
        </span>
        {existing.comment && <p className="text-muted mb-0">{existing.comment}</p>}
      </div>
    )
  }

  const submit = async (e) => {
    e.preventDefault()
    setSaving(true)
    try {
      await reviewService.create({ orderId, foodItemId: item.foodItemId, rating, comment: comment.trim() || undefined })
      toast.success('Thanks for your review')
      onSaved()
    } catch (err) {
      toast.error(errorMessage(err))
    } finally {
      setSaving(false)
    }
  }

  return (
    <form className="review-row" onSubmit={submit}>
      <strong>{item.foodItemName}</strong>
      <div role="radiogroup" aria-label={`Rate ${item.foodItemName}`} className="review-picker">
        {[1, 2, 3, 4, 5].map((n) => (
          <button
            type="button" key={n} role="radio" className={n <= rating ? 'on' : ''} aria-checked={rating === n}
            aria-label={`${n} star${n > 1 ? 's' : ''}`} onClick={() => setRating(n)}
          >
            <Star size={22} />
          </button>
        ))}
      </div>
      <input
        className="form-control" maxLength={500} placeholder="Add a comment (optional)"
        aria-label={`Comment on ${item.foodItemName}`} value={comment} onChange={(e) => setComment(e.target.value)}
      />
      <button className="btn btn-outline-secondary btn-sm" disabled={rating === 0 || saving}>Submit review</button>
    </form>
  )
}

export default function ReviewForm({ order, reviews, onSaved }) {
  return (
    <div className="od-card">
      <div className="od-section-title">Rate your food</div>
      {order.items.map((item) => (
        <ReviewRow
          key={item.id} orderId={order.id} item={item} onSaved={onSaved}
          existing={reviews.find((r) => r.orderId === order.id && r.foodItemId === item.foodItemId)}
        />
      ))}
    </div>
  )
}
