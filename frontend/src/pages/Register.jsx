import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { toast } from 'react-toastify'
import { errorMessage } from '../services/api'

export default function Register() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ name: '', email: '', password: '', phone: '' })
  const [busy, setBusy] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true)
    try {
      await register(form)
      toast.success('Account created')
      navigate('/menu')
    } catch (err) {
      toast.error(errorMessage(err, 'Registration failed'))
    } finally {
      setBusy(false)
    }
  }

  const field = (key) => ({ value: form[key], onChange: (e) => setForm({ ...form, [key]: e.target.value }) })

  return (
    <div className="page-pad auth-page">
      <div className="card shadow-sm">
        <div className="card-body p-4">
          <h1 className="h3 mb-3 brand-text">Create your student account</h1>
          <form onSubmit={submit}>
            <div className="mb-3">
              <label className="form-label" htmlFor="name">Full name</label>
              <input id="name" required maxLength={100} autoComplete="name" className="form-control" {...field('name')} />
            </div>
            <div className="mb-3">
              <label className="form-label" htmlFor="email">Email</label>
              <input id="email" type="email" required autoComplete="email" className="form-control" {...field('email')} />
            </div>
            <div className="mb-3">
              <label className="form-label" htmlFor="password">Password</label>
              <input
                id="password" type="password" required minLength={8} autoComplete="new-password"
                pattern="(?=.*[A-Za-z])(?=.*\d).{8,100}" aria-describedby="password-help"
                className="form-control" {...field('password')}
              />
              <div id="password-help" className="form-text">At least 8 characters, with a letter and a number.</div>
            </div>
            <div className="mb-3">
              <label className="form-label" htmlFor="phone">Phone (optional)</label>
              <input id="phone" type="tel" autoComplete="tel" className="form-control" {...field('phone')} />
            </div>
            <button type="submit" className="btn brand-btn w-100" disabled={busy}>
              {busy ? 'Creating...' : 'Create account'}
            </button>
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
