import { useEffect, useState } from 'react'
import api from '../services/api'
import { toast } from 'react-toastify'

export default function ManageUsers() {
  const [users, setUsers] = useState([])

  const load = () => api.get('/admin/users').then((r) => setUsers(r.data)).catch((e) => toast.error('Failed to load'))
  useEffect(load, [])

  return (
    <div className="page-pad">
      <h2 className="mb-3">Users</h2>
      <table className="table bg-white shadow-sm">
        <thead><tr><th>ID</th><th>Name</th><th>Email</th><th>Phone</th><th>Role</th><th>Enabled</th></tr></thead>
        <tbody>
          {users.map((u) => (
            <tr key={u.id}>
              <td>{u.id}</td><td>{u.name}</td><td>{u.email}</td><td>{u.phone || '-'}</td><td>{u.role}</td>
              <td>{u.enabled ? '✅' : '❌'}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}