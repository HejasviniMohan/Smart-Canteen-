import { Navigate } from 'react-router-dom'

/** Target of a QR poster at the canteen: scanning it opens the menu directly. */
export default function QrLanding() {
  return <Navigate to="/menu" replace />
}
