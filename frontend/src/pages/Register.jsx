import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { toast } from 'react-toastify'

export default function Register() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ name: '', email: '', password: '', phone: '', role: 'STUDENT' })

  const submit = async (e) => {
    e.preventDefault()
    try {
      await register(form)
      toast.success('Account created')
      navigate('/menu')
    } catch (err) {
      toast.error(err.response?.data?.message || 'Registration failed')
    }
  }

  return (
    <div className="page-pad" style={{ maxWidth: 460 }}>
      <div className="card shadow-sm">
        <div className="card-body p-4">
          <h3 className="mb-3 brand-text">Sign up</h3>
          <form onSubmit={submit}>
            <div className="mb-3">
              <label className="form-label">Full name</label>
              <input required className="form-control" value={form.name}
                     onChange={(e) => setForm({ ...form, name: e.target.value })} />
            </div>
            <div className="mb-3">
              <label className="form-label">Email</label>
              <input type="email" required className="form-control" value={form.email}
                     onChange={(e) => setForm({ ...form, email: e.target.value })} />
            </div>
            <div className="mb-3">
              <label className="form-label">Password</label>
              <input type="password" required minLength={6} className="form-control" value={form.password}
                     onChange={(e) => setForm({ ...form, password: e.target.value })} />
            </div>
            <div className="mb-3">
              <label className="form-label">Phone</label>
              <input className="form-control" value={form.phone}
                     onChange={(e) => setForm({ ...form, phone: e.target.value })} />
            </div>
            <div className="mb-3">
              <label className="form-label">Role</label>
              <select className="form-select" value={form.role}
                      onChange={(e) => setForm({ ...form, role: e.target.value })}>
                <option value="STUDENT">Student</option>
                <option value="STAFF">Canteen Staff</option>
                <option value="ADMIN">Admin</option>
              </select>
            </div>
            <button type="submit" className="btn brand-btn text-white w-100">Create account</button>
          </form>
          <hr />
          <div className="small text-muted">
            Already have an account? <Link to="/login">Login</Link>
          </div>
        </div>
      </div>
    </div>
  )
}