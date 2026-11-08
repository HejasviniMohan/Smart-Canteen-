import { useSyncExternalStore } from 'react'

// The first paint is themed by the inline script in index.html. This module takes over
// afterwards; both use the same storage key and the same two attributes.
const KEY = 'sc-theme'
const META_COLOR = { light: '#ECEFF3', dark: '#0A0A0A' }
const listeners = new Set()

export function getTheme() {
  return document.documentElement.getAttribute('data-theme') === 'dark' ? 'dark' : 'light'
}

function apply(theme) {
  const root = document.documentElement
  // Without this, every colour transition on the page runs at once and the switch smears.
  root.classList.add('theme-switching')
  root.setAttribute('data-theme', theme)
  root.setAttribute('data-bs-theme', theme)
  root.style.colorScheme = theme
  document.querySelector('meta[name="theme-color"]')?.setAttribute('content', META_COLOR[theme])
  void root.offsetHeight // force the new colours to apply before transitions return
  requestAnimationFrame(() => root.classList.remove('theme-switching'))
  listeners.forEach((fn) => fn())
}

const reducedMotion = () => window.matchMedia?.('(prefers-reduced-motion: reduce)').matches

/**
 * Switches theme. Given the point that was clicked, the new theme grows out of it as a
 * circle, using the View Transitions API where the browser supports it; elsewhere the
 * switch is instant.
 */
export function setTheme(theme, origin) {
  try { localStorage.setItem(KEY, theme) } catch { /* private mode: still switch for this visit */ }
  if (!origin || !document.startViewTransition || reducedMotion()) {
    apply(theme)
    return
  }
  const { x, y } = origin
  const radius = Math.hypot(Math.max(x, innerWidth - x), Math.max(y, innerHeight - y))
  const transition = document.startViewTransition(() => apply(theme))
  transition.ready.then(() => {
    document.documentElement.animate(
      { clipPath: [`circle(0px at ${x}px ${y}px)`, `circle(${radius}px at ${x}px ${y}px)`] },
      { duration: 700, easing: 'cubic-bezier(0.22, 1, 0.36, 1)', pseudoElement: '::view-transition-new(root)' },
    )
  }).catch(() => {})
}

export function toggleTheme(origin) {
  setTheme(getTheme() === 'dark' ? 'light' : 'dark', origin)
}

// Follow the system setting until the visitor picks a theme themselves.
if (typeof window !== 'undefined' && window.matchMedia) {
  window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', (e) => {
    let saved = null
    try { saved = localStorage.getItem(KEY) } catch { /* ignore */ }
    if (!saved) apply(e.matches ? 'dark' : 'light')
  })
}

function subscribe(fn) {
  listeners.add(fn)
  return () => listeners.delete(fn)
}

/** The current theme, re-rendering when it changes. */
export function useTheme() {
  return useSyncExternalStore(subscribe, getTheme, () => 'light')
}

/** Reads a colour token, for libraries such as charts that need a literal colour value. */
export function token(name) {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim()
}
