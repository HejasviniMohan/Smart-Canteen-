import { useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'

export default function QrLanding() {
  const { canteenId } = useParams()
  const navigate = useNavigate()
  useEffect(() => {
    navigate(`/menu?canteen=${canteenId || 'main'}`, { replace: true })
  }, [canteenId, navigate])
  return <div className="page-pad">Redirecting to menu...</div>
}