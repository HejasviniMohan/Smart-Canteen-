import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import { notificationService } from '../services/notificationService'
import { useAuth } from './AuthContext'
import { useLiveRefresh } from './LiveUpdatesContext'

const NotificationsContext = createContext(null)

export function NotificationsProvider({ children }) {
  const { isAuthenticated } = useAuth()
  const [items, setItems] = useState([])

  const reload = useCallback(() => {
    if (!isAuthenticated) return
    notificationService.mine().then((r) => setItems(r.data || [])).catch(() => {
      // The bell is secondary, so it keeps its last known state rather than interrupting the page.
    })
  }, [isAuthenticated])

  useEffect(() => {
    if (isAuthenticated) reload()
    else setItems([])
  }, [isAuthenticated, reload])

  useLiveRefresh(reload)

  const markRead = async (id) => {
    await notificationService.markRead(id)
    setItems((list) => list.map((n) => (n.id === id ? { ...n, isRead: true } : n)))
  }

  const markAll = async () => {
    await notificationService.markAll()
    setItems((list) => list.map((n) => ({ ...n, isRead: true })))
  }

  const unread = items.filter((n) => !n.isRead).length

  return (
    <NotificationsContext.Provider value={{ items, unread, markRead, markAll }}>
      {children}
    </NotificationsContext.Provider>
  )
}

export const useNotifications = () => useContext(NotificationsContext)
