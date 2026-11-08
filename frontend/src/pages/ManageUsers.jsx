import { useCallback, useEffect, useState } from 'react'
import { toast } from 'react-toastify'
import { adminService } from '../services/orderService'
import { errorMessage } from '../services/api'
import { useAuth } from '../context/AuthContext'
import AdminNav from '../components/AdminNav'
import { ErrorState, Skeleton } from '../components/Feedback'

const EMPTY = { name: '', email: '', password: '', phone: '', role: 'STAFF' }

export default function ManageUsers() {
  const { user: me } = useAuth()
  const [users, setUsers] = useState(null)
  const [error, setError] = useState(null)
  const [form, setForm] = useState(null)

  const load = useCallback(() => {
    setError(null)
    adminService.users().then((r) => setUsers(r.data)).catch((e) => setError(errorMessage(e)))
  }, [])

  useEffect(load, [load])

  const create = async (e) => {
    e.preventDefault()
    try {
      await adminService.createUser(form)
      toast.success('Account created')
      setForm(null)
      load()
    } catch (err) {
      toast.error(errorMessage(err))
    }
  }

  const toggle = async (u) => {
    try {
      await adminService.setEnabled(u.id, !u.enabled)
      toast.success(`${u.name} ${u.enabled ? 'disabled' : 'enabled'}`)
      load()
    } catch (err) {
      toast.error(errorMessage(err))
    }
  }

  const field = (key) => ({ value: form[key], onChange: (e) => setForm({ ...form, [key]: e.target.value }) })

  return (
    <div className="page-pad">
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h1 className="h2 mb-0">Users</h1>
        <button className="btn brand-btn" onClick={() => setForm(EMPTY)}>+ Add Staff</button>
      </div>
      <AdminNav />

      {error && <ErrorState message={error} onRetry={load} />}
      {!error && !users && <Skeleton rows={4} height={48} />}

      {users && (
        <div className="table-responsive">
          <table className="table">
            <thead><tr><th>Name</th><th>Email</th><th>Phone</th><th>Role</th><th>Status</th><th></th></tr></thead>
            <tbody>
              {users.map((u) => (
                <tr key={u.id}>
                  <td>{u.name}</td><td>{u.email}</td><td>{u.phone || '-'}</td><td>{u.role}</td>
                  <td><span className={`tag ${u.enabled ? 'tag-success' : 'tag-neutral'}`}>{u.enabled ? 'Active' : 'Disabled'}</span></td>
                  <td>
                    {u.email !== me?.email && (
                      <button className={`btn btn-sm ${u.enabled ? 'btn-outline-danger' : 'btn-outline-success'}`} onClick={() => toggle(u)}>
                        {u.enabled ? 'Disable' : 'Enable'}
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {form && (
        <div className="modal show d-block" style={{ background: 'var(--scrim)' }} role="dialog" aria-modal="true" aria-labelledby="add-user-title">
          <div className="modal-dialog">
            <div className="modal-content">
              <form onSubmit={create}>
                <div className="modal-header">
                  <h2 className="modal-title h5" id="add-user-title">Add staff or admin</h2>
                  <button type="button" className="btn-close" aria-label="Close" onClick={() => setForm(null)}></button>
                </div>
                <div className="modal-body">
                  <div className="mb-2"><label className="form-label" htmlFor="u-name">Name</label>
                    <input id="u-name" className="form-control" required {...field('name')} /></div>
                  <div className="mb-2"><label className="form-label" htmlFor="u-email">Email</label>
                    <input id="u-email" type="email" className="form-control" required {...field('email')} /></div>
                  <div className="mb-2"><label className="form-label" htmlFor="u-password">Temporary password</label>
                    <input id="u-password" type="password" autoComplete="new-password" className="form-control" required minLength={8}
                           pattern="(?=.*[A-Za-z])(?=.*\d).{8,100}" {...field('password')} />
                    <div className="form-text">At least 8 characters, with a letter and a number.</div></div>
                  <div className="mb-2"><label className="form-label" htmlFor="u-phone">Phone (optional)</label>
                    <input id="u-phone" type="tel" className="form-control" {...field('phone')} /></div>
                  <div className="mb-2"><label className="form-label" htmlFor="u-role">Role</label>
                    <select id="u-role" className="form-select" {...field('role')}>
                      <option value="STAFF">Staff</option>
                      <option value="ADMIN">Admin</option>
                    </select></div>
                </div>
                <div className="modal-footer">
                  <button type="button" className="btn btn-secondary" onClick={() => setForm(null)}>Cancel</button>
                  <button type="submit" className="btn brand-btn">Create</button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
