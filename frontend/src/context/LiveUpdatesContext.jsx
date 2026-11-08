import { createContext, useContext, useEffect, useRef, useState } from 'react'
import { openLiveStream } from '../services/liveStream'
import { useAuth } from './AuthContext'

const LiveUpdatesContext = createContext(null)

/** One stream per browser tab, shared by every component that wants live data. */
export function LiveUpdatesProvider({ children }) {
  const { isAuthenticated } = useAuth()
  const [listeners] = useState(() => new Set())
  const [connected, setConnected] = useState(false)

  useEffect(() => {
    if (!isAuthenticated) return undefined
    const notify = (event) => listeners.forEach((listener) => listener(event))
    const close = openLiveStream({
      onEvent: notify,
      onConnected: () => {
        setConnected(true)
        // Anything that changed while disconnected was missed, so everyone refetches.
        notify({ type: 'reconnected' })
      },
    })
    return () => {
      close()
      setConnected(false)
    }
  }, [isAuthenticated, listeners])

  return (
    <LiveUpdatesContext.Provider value={{ listeners, connected }}>
      {children}
    </LiveUpdatesContext.Provider>
  )
}

export const useLiveConnected = () => useContext(LiveUpdatesContext)?.connected ?? false

/**
 * Calls reload whenever a matching live event arrives. A slow timer stays as a safety net
 * in case the stream is blocked, for example by a restrictive network.
 */
export function useLiveRefresh(reload, { filter, fallbackMs = 60000 } = {}) {
  const context = useContext(LiveUpdatesContext)
  const latestReload = useRef(reload)
  const latestFilter = useRef(filter)

  useEffect(() => {
    latestReload.current = reload
    latestFilter.current = filter
  })

  useEffect(() => {
    if (!context) return undefined
    const listener = (event) => {
      if (event.type === 'reconnected' || !latestFilter.current || latestFilter.current(event)) {
        latestReload.current()
      }
    }
    context.listeners.add(listener)
    const timer = setInterval(() => latestReload.current(), fallbackMs)
    return () => {
      context.listeners.delete(listener)
      clearInterval(timer)
    }
  }, [context, fallbackMs])
}
