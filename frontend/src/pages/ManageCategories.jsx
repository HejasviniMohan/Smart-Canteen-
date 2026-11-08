import { useCallback, useEffect, useState } from 'react'
import { toast } from 'react-toastify'
import { categoryService } from '../services/foodService'
import { errorMessage } from '../services/api'
import AdminNav from '../components/AdminNav'
import { ErrorState, Skeleton } from '../components/Feedback'

export default function ManageCategories() {
  const [items, setItems] = useState(null)
  const [error, setError] = useState(null)
  const [editingId, setEditingId] = useState(null)
  const [form, setForm] = useState(null)

  const load = useCallback(() => {
    setError(null)
    categoryService.list().then((r) => setItems(r.data)).catch((e) => setError(errorMessage(e)))
  }, [])

  useEffect(load, [load])

  const open = (c) => {
    setEditingId(c?.id ?? null)
    setForm(c
      ? { name: c.name, description: c.description || '', displayOrder: c.displayOrder }
      : { name: '', description: '', displayOrder: 0 })
  }

  const save = async (e) => {
    e.preventDefault()
    try {
      if (editingId) await categoryService.update(editingId, form)
      else await categoryService.create(form)
      toast.success('Saved')
      setForm(null)
      load()
    } catch (err) {
      toast.error(errorMessage(err, 'Save failed'))
    }
  }

  const remove = async (c) => {
    if (!window.confirm(`Delete the ${c.name} category?`)) return
    try {
      await categoryService.remove(c.id)
      toast.success('Deleted')
      load()
    } catch (err) {
      toast.error(err.response?.status === 409 ? 'Move or remove its foods first.' : errorMessage(err))
    }
  }

  return (
    <div className="page-pad">
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h1 className="h2 mb-0">Categories</h1>
        <button className="btn brand-btn" onClick={() => open(null)}>+ Add Category</button>
      </div>
      <AdminNav />

      {error && <ErrorState message={error} onRetry={load} />}
      {!error && !items && <Skeleton rows={4} height={48} />}

      {items && (
        <div className="table-responsive">
          <table className="table">
            <thead><tr><th>Name</th><th>Description</th><th>Order</th><th></th></tr></thead>
            <tbody>
              {items.map((c) => (
                <tr key={c.id}>
                  <td>{c.name}</td><td>{c.description}</td><td>{c.displayOrder}</td>
                  <td className="text-nowrap">
                    <button className="btn btn-sm btn-outline-primary me-1" onClick={() => open(c)}>Edit</button>
                    <button className="btn btn-sm btn-outline-danger" onClick={() => remove(c)}>Delete</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {form && (
        <div className="modal show d-block" style={{ background: 'var(--scrim)' }} role="dialog" aria-modal="true" aria-labelledby="cat-title">
          <div className="modal-dialog">
            <form className="modal-content" onSubmit={save}>
              <div className="modal-header">
                <h2 className="modal-title h5" id="cat-title">{editingId ? 'Edit' : 'Add'} Category</h2>
                <button type="button" className="btn-close" aria-label="Close" onClick={() => setForm(null)}></button>
              </div>
              <div className="modal-body">
                <div className="mb-2"><label className="form-label" htmlFor="c-name">Name</label>
                  <input id="c-name" className="form-control" required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></div>
                <div className="mb-2"><label className="form-label" htmlFor="c-desc">Description</label>
                  <input id="c-desc" className="form-control" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} /></div>
                <div className="mb-2"><label className="form-label" htmlFor="c-order">Display order</label>
                  <input id="c-order" type="number" className="form-control" value={form.displayOrder} onChange={(e) => setForm({ ...form, displayOrder: Number(e.target.value) })} /></div>
              </div>
              <div className="modal-footer">
                <button type="button" className="btn btn-secondary" onClick={() => setForm(null)}>Cancel</button>
                <button type="submit" className="btn brand-btn">Save</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
