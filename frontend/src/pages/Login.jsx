import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { toast } from 'react-toastify'
import { errorMessage } from '../services/api'

// Shown only in builds made with VITE_DEMO_MODE=true, next to a server seeded with these
// accounts. A normal deployment has neither the accounts nor this panel.
const DEMO_MODE = import.meta.env.VITE_DEMO_MODE === 'true'
const DEMO_ACCOUNTS = [
  { label: 'Student', email: 'student@canteen.com', password: 'Student@123' },
  { label: 'Staff', email: 'staff@canteen.com', password: 'Staff@123' },
  { label: 'Admin', email: 'admin@canteen.com', password: 'Admin@123' },
]

export default function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ email: '', password: '' })
  const [busy, setBusy] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true)
    try {
      const u = await login(form.email, form.password)
      toast.success(`Welcome back, ${u.name}!`)
      if (u.role === 'ADMIN') navigate('/admin')
      else if (u.role === 'STAFF') navigate('/staff')
      else navigate('/menu')
    } catch (err) {
      toast.error(errorMessage(err, 'Login failed'))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="page-pad auth-page">
      <div className="card shadow-sm">
        <div className="card-body p-4">
          <h1 className="h3 mb-3 brand-text">Login</h1>
          <form onSubmit={submit}>
            <div className="mb-3">
              <label className="form-label" htmlFor="email">Email</label>
              <input id="email" type="email" required autoComplete="username" className="form-control" value={form.email}
                     onChange={(e) => setForm({ ...form, email: e.target.value })} />
            </div>
            <div className="mb-3">
              <label className="form-label" htmlFor="password">Password</label>
              <input id="password" type="password" required autoComplete="current-password" className="form-control" value={form.password}
                     onChange={(e) => setForm({ ...form, password: e.target.value })} />
            </div>
            <button type="submit" className="btn brand-btn w-100" disabled={busy}>
              {busy ? 'Logging in...' : 'Login'}
            </button>
          </form>
          <hr />
          <div className="small text-muted">
            Don't have an account? <Link to="/register">Sign up</Link>
          </div>
          {DEMO_MODE && (
            <div className="demo-box">
              <strong>Demo: try a role</strong>
              <div className="d-flex gap-2 flex-wrap mt-2">
                {DEMO_ACCOUNTS.map((a) => (
                  <button key={a.label} type="button" className="btn btn-outline-secondary btn-sm"
                          onClick={() => setForm({ email: a.email, password: a.password })}>
                    {a.label}
                  </button>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
