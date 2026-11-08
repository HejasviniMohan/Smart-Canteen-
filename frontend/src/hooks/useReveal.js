import { useEffect, useRef } from 'react'

// Content is only hidden for the reveal once this script has run, so a page without
// JavaScript, or one where the observer never fires, still shows everything.
if (typeof document !== 'undefined' && 'IntersectionObserver' in window) {
  document.documentElement.classList.add('js-reveal')
}

let observer = null
function getObserver() {
  if (!observer) {
    observer = new IntersectionObserver((entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.setAttribute('data-reveal', 'in')
          observer.unobserve(entry.target)
        }
      })
    }, { rootMargin: '0px 0px -8% 0px', threshold: 0.08 })
  }
  return observer
}

/** Eases an element in the first time it scrolls into view. */
export function useReveal() {
  const ref = useRef(null)
  useEffect(() => {
    const el = ref.current
    if (!el || !('IntersectionObserver' in window)) return undefined
    const io = getObserver()
    io.observe(el)
    return () => io.unobserve(el)
  }, [])
  return ref
}
