import { useCallback, useEffect, useState } from 'react'
import { canteenService } from '../services/orderService'
import { useLiveRefresh } from '../context/LiveUpdatesContext'
import { slotLabel } from '../utils/format'

/**
 * The canteen's live state, shown as a pickup ticket in the hero.
 * Every number comes from the /canteen/status endpoint.
 */
export default function StatusStrip() {
  const [status, setStatus] = useState(null)

  const load = useCallback(() => {
    // Purely informational: if it fails, the ticket just stays hidden.
    canteenService.status().then((r) => setStatus(r.data)).catch(() => {})
  }, [])

  useEffect(load, [load])
  useLiveRefresh(load, { fallbackMs: 30000 })

  if (!status) return null

  return (
    <section className="sc-status" aria-label="Canteen right now">
      <div className="ticket-head">
        <div>
          <div className="ticket-state">
            <span className={`live-dot ${status.open ? '' : 'closed'}`} aria-hidden="true" />
            {status.open ? 'Open now' : 'Closed'}
          </div>
          <div className="ticket-sub">{status.open ? `Until ${status.closesAt}` : `Opens at ${status.opensAt}`}</div>
        </div>
      </div>
      <dl>
        <div><dt>Orders in the kitchen</dt><dd>{status.ordersInKitchen}</dd></div>
        <div><dt>Kitchen queue</dt><dd>~{status.estimatedWaitMinutes} min</dd></div>
        <div><dt>Next pickup slot</dt><dd>{status.nextSlot ? slotLabel(status.nextSlot) : 'None free'}</dd></div>
      </dl>
    </section>
  )
}
