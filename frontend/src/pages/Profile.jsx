import { useEffect, useState } from 'react'
import { toast } from 'react-toastify'
import { useAuth } from '../context/AuthContext'
import { authService } from '../services/authService'
import { errorMessage } from '../services/api'

export default function Profile() {
  const { user, logout, updateUser } = useAuth()
  const [form, setForm] = useState({ name: user?.name || '', phone: user?.phone || '' })
  const [email, setEmail] = useState(user?.email)
  const [passwords, setPasswords] = useState({ currentPassword: '', newPassword: '' })

  useEffect(() => {
    authService.me().then(({ data }) => {
      setForm({ name: data.name, phone: data.phone || '' })
      setEmail(data.email)
    }).catch((e) => toast.error(errorMessage(e, 'Could not load your profile')))
  }, [])

  const saveProfile = async (e) => {
    e.preventDefault()
    try {
      const { data } = await authService.update(form)
      updateUser({ name: data.name, phone: data.phone })
      toast.success('Profile updated')
    } catch (err) {
      toast.error(errorMessage(err))
    }
  }

  const savePassword = async (e) => {
    e.preventDefault()
    try {
      await authService.changePassword(passwords)
      setPasswords({ currentPassword: '', newPassword: '' })
      toast.success('Password changed')
    } catch (err) {
      toast.error(errorMessage(err))
    }
  }

  return (
    <div className="page-pad" style={{ maxWidth: 560, margin: '0 auto' }}>
      <h1 className="h2 mb-3">My Profile</h1>

      <div className="card shadow-sm p-4 mb-3">
        <p className="mb-3"><strong>{email}</strong> <span className="tag tag-neutral ms-1">{user?.role}</span></p>
        <form onSubmit={saveProfile}>
          <div className="mb-3">
            <label className="form-label" htmlFor="profile-name">Name</label>
            <input id="profile-name" className="form-control" required maxLength={100} value={form.name}
                   onChange={(e) => setForm({ ...form, name: e.target.value })} />
          </div>
          <div className="mb-3">
            <label className="form-label" htmlFor="profile-phone">Phone</label>
            <input id="profile-phone" type="tel" className="form-control" value={form.phone}
                   onChange={(e) => setForm({ ...form, phone: e.target.value })} />
          </div>
          <button className="btn brand-btn">Save changes</button>
        </form>
      </div>

      <div className="card shadow-sm p-4 mb-3">
        <h2 className="h5 mb-3">Change password</h2>
        <form onSubmit={savePassword}>
          <div className="mb-3">
            <label className="form-label" htmlFor="current-password">Current password</label>
            <input id="current-password" type="password" required autoComplete="current-password" className="form-control"
                   value={passwords.currentPassword}
                   onChange={(e) => setPasswords({ ...passwords, currentPassword: e.target.value })} />
          </div>
          <div className="mb-3">
            <label className="form-label" htmlFor="new-password">New password</label>
            <input id="new-password" type="password" required minLength={8} autoComplete="new-password" className="form-control"
                   pattern="(?=.*[A-Za-z])(?=.*\d).{8,100}" aria-describedby="new-password-help"
                   value={passwords.newPassword}
                   onChange={(e) => setPasswords({ ...passwords, newPassword: e.target.value })} />
            <div id="new-password-help" className="form-text">At least 8 characters, with a letter and a number.</div>
          </div>
          <button className="btn btn-outline-secondary">Change password</button>
        </form>
      </div>

      <button className="btn btn-outline-danger" onClick={logout}>Logout</button>
    </div>
  )
}
