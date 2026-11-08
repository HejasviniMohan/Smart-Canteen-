import { useEffect, useRef, useState } from 'react'
import { Drumstick, Leaf } from 'lucide-react'

/** A food photo that fades in once loaded, with an icon fallback if it can't load. */
export default function FoodImage({ food, placeholderClass }) {
  const [failed, setFailed] = useState(false)
  const [loaded, setLoaded] = useState(false)
  const img = useRef(null)

  // A cached image can finish loading before React attaches onLoad, so check once on mount.
  useEffect(() => {
    if (img.current?.complete && img.current.naturalWidth > 0) setLoaded(true)
  }, [])

  if (!food.imageUrl || failed) {
    const Icon = food.isVeg ? Leaf : Drumstick
    return <div className={placeholderClass} aria-hidden="true"><Icon size={36} strokeWidth={1.25} /></div>
  }
  return (
    <img
      ref={img}
      src={food.imageUrl}
      alt={food.name}
      loading="lazy"
      decoding="async"
      className="img-fade"
      data-loaded={loaded}
      onLoad={() => setLoaded(true)}
      onError={() => setFailed(true)}
    />
  )
}
