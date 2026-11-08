import { Moon, Sun } from 'lucide-react'
import { toggleTheme, useTheme } from '../theme'

export default function ThemeToggle({ className = 'icon-btn' }) {
  const theme = useTheme()
  const next = theme === 'dark' ? 'light' : 'dark'
  return (
    <button type="button" className={`${className} theme-toggle`} onClick={(e) => {
        const r = e.currentTarget.getBoundingClientRect()
        toggleTheme({ x: r.left + r.width / 2, y: r.top + r.height / 2 })
      }}
      aria-label={`Switch to ${next} theme`} title={`Switch to ${next} theme`}>
      {/* Both icons stay mounted so the swap can cross-fade instead of popping. */}
      <span className="theme-icon" data-on={theme === 'light'}><Moon size={18} /></span>
      <span className="theme-icon" data-on={theme === 'dark'}><Sun size={18} /></span>
    </button>
  )
}
