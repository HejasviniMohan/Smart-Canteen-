import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { Bell } from 'lucide-react'
import { useNotifications } from '../context/NotificationsContext'
import { dateTime } from '../utils/format'

export default function NotificationBell() {
  const { items, unread, markRead, markAll } = useNotifications()
  const [open, setOpen] = useState(false)
  const root = useRef(null)

  useEffect(() => {
    if (!open) return undefined
    const closeOnOutside = (e) => { if (!root.current?.contains(e.target)) setOpen(false) }
    const closeOnEscape = (e) => { if (e.key === 'Escape') setOpen(false) }
    document.addEventListener('mousedown', closeOnOutside)
    document.addEventListener('keydown', closeOnEscape)
    return () => {
      document.removeEventListener('mousedown', closeOnOutside)
      document.removeEventListener('keydown', closeOnEscape)
    }
  }, [open])

  return (
    <div className="notif-root" ref={root}>
      <button
        className="icon-btn"
        aria-label={unread > 0 ? `Notifications, ${unread} unread` : 'Notifications'}
        aria-expanded={open}
        onClick={() => setOpen((v) => !v)}
      >
        <Bell size={18} />
        {unread > 0 && <span className="dot" data-testid="unread-count">{unread}</span>}
      </button>
      {open && (
        <div className="notif-panel" role="dialog" aria-label="Notifications">
          <div className="notif-head">
            <strong>Notifications</strong>
            {unread > 0 && <button className="btn-text" onClick={markAll}>Mark all read</button>}
          </div>
          {items.length === 0 && <p className="notif-empty">Nothing yet. Updates on your orders will appear here.</p>}
          <ul>
            {items.map((n) => (
              <li key={n.id} className={n.isRead ? '' : 'unread'}>
                <button onClick={() => { if (!n.isRead) markRead(n.id) }}>
                  <span className="notif-title">{n.title}</span>
                  <span className="notif-message">{n.message}</span>
                  <span className="notif-time">{dateTime(n.createdAt)}</span>
                </button>
              </li>
            ))}
          </ul>
          <Link to="/profile" className="notif-foot" onClick={() => setOpen(false)}>Go to profile</Link>
        </div>
      )}
    </div>
  )
}
