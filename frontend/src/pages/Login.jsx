import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { toast } from 'react-toastify'

export default function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ email: '', password: '' })

  const submit = async (e) => {
    e.preventDefault()
    try {
      const u = await login(form.email, form.password)
      toast.success(`Welcome back, ${u.name}!`)
      if (u.role === 'ADMIN') navigate('/admin')
      else if (u.role === 'STAFF') navigate('/staff')
      else navigate('/menu')
    } catch (err) {
      toast.error(err.response?.data?.message || 'Login failed')
    }
  }

  return (
    <div className="page-pad" style={{ maxWidth: 460 }}>
      <div className="card shadow-sm">
        <div className="card-body p-4">
          <h3 className="mb-3 brand-text">Login</h3>
          <form onSubmit={submit}>
            <div className="mb-3">
              <label className="form-label">Email</label>
              <input type="email" required className="form-control" value={form.email}
                     onChange={(e) => setForm({ ...form, email: e.target.value })} />
            </div>
            <div className="mb-3">
              <label className="form-label">Password</label>
              <input type="password" required className="form-control" value={form.password}
                     onChange={(e) => setForm({ ...form, password: e.target.value })} />
            </div>
            <button type="submit" className="btn brand-btn text-white w-100">Login</button>
          </form>
          <hr />
          <div className="small text-muted">
            Don't have an account? <Link to="/register">Sign up</Link>
          </div>
          <div className="mt-2 small text-muted">
            Demo: admin@canteen.com / Admin@123 — student@canteen.com / Student@123
          </div>
        </div>
      </div>
    </div>
  )
}