import { useEffect, useState } from 'react'
import { foodService, categoryService } from '../services/foodService'
import { toast } from 'react-toastify'

export default function ManageFoods() {
  const [foods, setFoods] = useState([])
  const [cats, setCats] = useState([])
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState({ categoryId: '', name: '', description: '', price: '', prepTimeMin: 10, isVeg: true, imageUrl: '' })

  const load = () => {
    foodService.list().then((r) => setFoods(r.data))
    categoryService.list().then((r) => setCats(r.data))
  }
  useEffect(load, [])

  const open = (f) => {
    setEditing(f.id)
    setForm(f ? {
      categoryId: f.categoryId, name: f.name, description: f.description || '',
      price: f.price, prepTimeMin: f.prepTimeMin, isVeg: f.isVeg, imageUrl: f.imageUrl || '',
    } : { categoryId: cats[0]?.id || '', name: '', description: '', price: '', prepTimeMin: 10, isVeg: true, imageUrl: '' })
  }

  const save = async (e) => {
    e.preventDefault()
    try {
      if (editing) await foodService.update(editing, form)
      else await foodService.create(form)
      toast.success('Saved'); setEditing(null); load()
    } catch (err) { toast.error(err.response?.data?.message || 'Save failed') }
  }

  const remove = async (id) => {
    if (!confirm('Delete this food?')) return
    try { await foodService.remove(id); toast.success('Deleted'); load() } catch (e) { toast.error(e.response?.data?.message || 'Failed') }
  }

  return (
    <div className="page-pad">
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h2>Manage Foods</h2>
        <button className="btn brand-btn text-white" onClick={() => open(null)}>+ Add Food</button>
      </div>
      <table className="table bg-white shadow-sm">
        <thead><tr><th>ID</th><th>Name</th><th>Category</th><th>Price</th><th>Veg</th><th>Avail.</th><th></th></tr></thead>
        <tbody>
          {foods.map((f) => (
            <tr key={f.id}>
              <td>{f.id}</td><td>{f.name}</td><td>{f.categoryName}</td>
              <td>₹{Number(f.price).toFixed(0)}</td>
              <td>{f.isVeg ? '🟢' : '🔴'}</td>
              <td>{f.availability ? '✅' : '❌'}</td>
              <td>
                <button className="btn btn-sm btn-outline-primary me-1" onClick={() => open(f)}>Edit</button>
                <button className="btn btn-sm btn-outline-danger" onClick={() => remove(f.id)}>Delete</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {editing !== null && (
        <div className="modal show d-block" style={{ background: 'rgba(0,0,0,0.5)' }}>
          <div className="modal-dialog">
            <div className="modal-content">
              <form onSubmit={save}>
                <div className="modal-header">
                  <h5 className="modal-title">{editing ? 'Edit' : 'Add'} Food</h5>
                  <button type="button" className="btn-close" onClick={() => setEditing(null)}></button>
                </div>
                <div className="modal-body">
                  <div className="mb-2">
                    <label className="form-label">Category</label>
                    <select className="form-select" required value={form.categoryId} onChange={(e) => setForm({ ...form, categoryId: Number(e.target.value) })}>
                      {cats.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
                    </select>
                  </div>
                  <div className="mb-2"><label className="form-label">Name</label>
                    <input className="form-control" required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></div>
                  <div className="mb-2"><label className="form-label">Description</label>
                    <textarea className="form-control" rows="2" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} /></div>
                  <div className="mb-2"><label className="form-label">Price (₹)</label>
                    <input type="number" step="0.01" min="0" className="form-control" required value={form.price} onChange={(e) => setForm({ ...form, price: Number(e.target.value) })} /></div>
                  <div className="mb-2"><label className="form-label">Image URL (optional)</label>
                    <input className="form-control" value={form.imageUrl} onChange={(e) => setForm({ ...form, imageUrl: e.target.value })} /></div>
                  <div className="row g-2">
                    <div className="col-6"><label className="form-label">Prep time (min)</label>
                      <input type="number" min="1" className="form-control" value={form.prepTimeMin} onChange={(e) => setForm({ ...form, prepTimeMin: Number(e.target.value) })} /></div>
                    <div className="col-6 d-flex align-items-end">
                      <div className="form-check form-switch">
                        <input className="form-check-input" type="checkbox" id="veg" checked={form.isVeg} onChange={(e) => setForm({ ...form, isVeg: e.target.checked })} />
                        <label className="form-check-label ms-2" htmlFor="veg">Vegetarian</label>
                      </div>
                    </div>
                  </div>
                </div>
                <div className="modal-footer">
                  <button type="button" className="btn btn-secondary" onClick={() => setEditing(null)}>Cancel</button>
                  <button type="submit" className="btn brand-btn text-white">Save</button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}