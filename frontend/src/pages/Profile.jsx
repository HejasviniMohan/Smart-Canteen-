import { useEffect, useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { authService } from '../services/authService'

export default function Profile() {
  const { user, logout } = useAuth()
  const [fresh, setFresh] = useState(null)

  useEffect(() => {
    authService.me().then((r) => setFresh(r.data)).catch(() => {})
  }, [])

  const u = fresh || user

  return (
    <div className="page-pad" style={{ maxWidth: 560 }}>
      <h2 className="mb-3">My Profile</h2>
      <div className="card shadow-sm p-4">
        <div className="text-center mb-3">
          <div style={{fontSize:'4rem'}}>👤</div>
          <h4 className="mb-0">{u?.name}</h4>
          <small className="text-muted">{u?.role}</small>
        </div>
        <table className="table">
          <tbody>
            <tr><th>Email</th><td>{u?.email}</td></tr>
            <tr><th>Phone</th><td>{u?.phone || '-'}</td></tr>
            <tr><th>Role</th><td>{u?.role}</td></tr>
          </tbody>
        </table>
        <button className="btn btn-outline-danger" onClick={logout}>Logout</button>
      </div>
    </div>
  )
}