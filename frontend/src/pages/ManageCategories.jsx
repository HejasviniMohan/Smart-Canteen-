import { useEffect, useState } from 'react'
import { categoryService } from '../services/foodService'
import { toast } from 'react-toastify'

export default function ManageCategories() {
  const [items, setItems] = useState([])
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState({ name: '', description: '', displayOrder: 0 })

  const load = () => categoryService.list().then((r) => setItems(r.data))
  useEffect(load, [])

  const open = (c) => {
    setEditing(c?.id ?? null)
    setForm(c ? { name: c.name, description: c.description || '', displayOrder: c.displayOrder } : { name: '', description: '', displayOrder: 0 })
  }
  const save = async (e) => {
    e.preventDefault()
    try {
      if (editing) await categoryService.update(editing, form)
      else await categoryService.create(form)
      toast.success('Saved'); setEditing(null); load()
    } catch (err) { toast.error(err.response?.data?.message || 'Save failed') }
  }
  const remove = async (id) => {
    if (!confirm('Delete category?')) return
    try { await categoryService.remove(id); toast.success('Deleted'); load() } catch (e) { toast.error(e.response?.data?.message || 'Failed') }
  }

  return (
    <div className="page-pad">
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h2>Manage Categories</h2>
        <button className="btn brand-btn text-white" onClick={() => open(null)}>+ Add Category</button>
      </div>
      <table className="table bg-white shadow-sm">
        <thead><tr><th>ID</th><th>Name</th><th>Description</th><th>Order</th><th></th></tr></thead>
        <tbody>
          {items.map((c) => (
            <tr key={c.id}>
              <td>{c.id}</td><td>{c.name}</td><td>{c.description}</td><td>{c.displayOrder}</td>
              <td>
                <button className="btn btn-sm btn-outline-primary me-1" onClick={() => open(c)}>Edit</button>
                <button className="btn btn-sm btn-outline-danger" onClick={() => remove(c.id)}>Delete</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {editing !== null && (
        <div className="modal show d-block" style={{ background: 'rgba(0,0,0,0.5)' }}>
          <div className="modal-dialog">
            <form className="modal-content" onSubmit={save}>
              <div className="modal-header">
                <h5 className="modal-title">{editing ? 'Edit' : 'Add'} Category</h5>
                <button type="button" className="btn-close" onClick={() => setEditing(null)}></button>
              </div>
              <div className="modal-body">
                <div className="mb-2"><label className="form-label">Name</label>
                  <input className="form-control" required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></div>
                <div className="mb-2"><label className="form-label">Description</label>
                  <input className="form-control" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} /></div>
                <div className="mb-2"><label className="form-label">Display order</label>
                  <input type="number" className="form-control" value={form.displayOrder} onChange={(e) => setForm({ ...form, displayOrder: Number(e.target.value) })} /></div>
              </div>
              <div className="modal-footer">
                <button type="button" className="btn btn-secondary" onClick={() => setEditing(null)}>Cancel</button>
                <button type="submit" className="btn brand-btn text-white">Save</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}