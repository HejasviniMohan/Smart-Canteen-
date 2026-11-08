import { useCallback, useEffect, useState } from 'react'
import { toast } from 'react-toastify'
import { foodService, categoryService } from '../services/foodService'
import { errorMessage } from '../services/api'
import { money } from '../utils/format'
import AdminNav from '../components/AdminNav'
import { ErrorState, Skeleton } from '../components/Feedback'

const blank = (categoryId) => ({
  categoryId, name: '', description: '', price: '', specialPrice: '',
  isTodaySpecial: false, prepTimeMin: 10, isVeg: true, imageUrl: '',
})

export default function ManageFoods() {
  const [foods, setFoods] = useState(null)
  const [cats, setCats] = useState([])
  const [error, setError] = useState(null)
  const [editingId, setEditingId] = useState(null)
  const [form, setForm] = useState(null)

  const load = useCallback(() => {
    setError(null)
    Promise.all([foodService.list(), categoryService.list()])
      .then(([f, c]) => { setFoods(f.data); setCats(c.data) })
      .catch((e) => setError(errorMessage(e)))
  }, [])

  useEffect(load, [load])

  const openNew = () => {
    setEditingId(null)
    setForm(blank(cats[0]?.id ?? ''))
  }

  const openEdit = (f) => {
    setEditingId(f.id)
    setForm({
      categoryId: f.categoryId, name: f.name, description: f.description || '', price: f.price,
      specialPrice: f.specialPrice ?? '', isTodaySpecial: f.isTodaySpecial, prepTimeMin: f.prepTimeMin,
      isVeg: f.isVeg, imageUrl: f.imageUrl || '',
    })
  }

  const save = async (e) => {
    e.preventDefault()
    const hasSpecial = form.specialPrice !== ''
    const payload = {
      ...form,
      specialPrice: hasSpecial ? form.specialPrice : undefined,
      imageUrl: form.imageUrl.trim() || undefined,
      ...(editingId && !hasSpecial ? { clearSpecialPrice: true } : {}),
    }
    try {
      if (editingId) await foodService.update(editingId, payload)
      else await foodService.create(payload)
      toast.success('Saved')
      setForm(null)
      load()
    } catch (err) {
      toast.error(errorMessage(err, 'Save failed'))
    }
  }

  const remove = async (f) => {
    if (!window.confirm(`Remove ${f.name} from the menu? Past orders keep their history.`)) return
    try {
      await foodService.remove(f.id)
      toast.success('Removed from the menu')
      load()
    } catch (err) {
      toast.error(errorMessage(err))
    }
  }

  const set = (key, value) => setForm({ ...form, [key]: value })

  return (
    <div className="page-pad">
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h1 className="h2 mb-0">Foods</h1>
        <button className="btn brand-btn" onClick={openNew} disabled={!foods}>+ Add Food</button>
      </div>
      <AdminNav />

      {error && <ErrorState message={error} onRetry={load} />}
      {!error && !foods && <Skeleton rows={6} height={48} />}

      {foods && (
        <div className="table-responsive">
          <table className="table">
            <thead><tr><th>Name</th><th>Category</th><th>Price</th><th>Special</th><th>Today</th><th>Available</th><th></th></tr></thead>
            <tbody>
              {foods.map((f) => (
                <tr key={f.id}>
                  <td>{f.name}</td><td>{f.categoryName}</td>
                  <td>{money(f.price)}</td>
                  <td>{f.specialPrice ? money(f.specialPrice) : '-'}</td>
                  <td>{f.isTodaySpecial ? 'Yes' : '-'}</td>
                  <td>{f.availability ? 'Yes' : 'No'}</td>
                  <td className="text-nowrap">
                    <button className="btn btn-sm btn-outline-primary me-1" onClick={() => openEdit(f)}>Edit</button>
                    <button className="btn btn-sm btn-outline-danger" onClick={() => remove(f)}>Remove</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {form && (
        <div className="modal show d-block" style={{ background: 'var(--scrim)' }} role="dialog" aria-modal="true" aria-labelledby="food-title">
          <div className="modal-dialog modal-dialog-scrollable">
            <div className="modal-content">
              <form onSubmit={save} style={{ display: 'contents' }}>
                <div className="modal-header">
                  <h2 className="modal-title h5" id="food-title">{editingId ? 'Edit' : 'Add'} Food</h2>
                  <button type="button" className="btn-close" aria-label="Close" onClick={() => setForm(null)}></button>
                </div>
                <div className="modal-body">
                  <div className="mb-2">
                    <label className="form-label" htmlFor="f-cat">Category</label>
                    <select id="f-cat" className="form-select" required value={form.categoryId} onChange={(e) => set('categoryId', Number(e.target.value))}>
                      {cats.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
                    </select>
                  </div>
                  <div className="mb-2"><label className="form-label" htmlFor="f-name">Name</label>
                    <input id="f-name" className="form-control" required maxLength={120} value={form.name} onChange={(e) => set('name', e.target.value)} /></div>
                  <div className="mb-2"><label className="form-label" htmlFor="f-desc">Description</label>
                    <textarea id="f-desc" className="form-control" rows="2" maxLength={500} value={form.description} onChange={(e) => set('description', e.target.value)} /></div>
                  <div className="row g-2 mb-2">
                    <div className="col-6"><label className="form-label" htmlFor="f-price">Price (₹)</label>
                      <input id="f-price" type="number" step="0.01" min="0.01" className="form-control" required value={form.price} onChange={(e) => set('price', e.target.value)} /></div>
                    <div className="col-6"><label className="form-label" htmlFor="f-special">Special price (₹)</label>
                      <input id="f-special" type="number" step="0.01" min="0.01" className="form-control" placeholder="None" value={form.specialPrice} onChange={(e) => set('specialPrice', e.target.value)} /></div>
                  </div>
                  <div className="mb-2"><label className="form-label" htmlFor="f-image">Image URL (optional)</label>
                    <input id="f-image" type="url" className="form-control" maxLength={500} value={form.imageUrl} onChange={(e) => set('imageUrl', e.target.value)} /></div>
                  <div className="mb-2"><label className="form-label" htmlFor="f-prep">Prep time (min)</label>
                    <input id="f-prep" type="number" min="1" max="120" className="form-control" value={form.prepTimeMin} onChange={(e) => set('prepTimeMin', Number(e.target.value))} /></div>
                  <div className="form-check form-switch">
                    <input className="form-check-input" type="checkbox" id="f-veg" checked={form.isVeg} onChange={(e) => set('isVeg', e.target.checked)} />
                    <label className="form-check-label" htmlFor="f-veg">Vegetarian</label>
                  </div>
                  <div className="form-check form-switch">
                    <input className="form-check-input" type="checkbox" id="f-today" checked={form.isTodaySpecial} onChange={(e) => set('isTodaySpecial', e.target.checked)} />
                    <label className="form-check-label" htmlFor="f-today">Today's special</label>
                  </div>
                </div>
                <div className="modal-footer">
                  <button type="button" className="btn btn-secondary" onClick={() => setForm(null)}>Cancel</button>
                  <button type="submit" className="btn brand-btn">Save</button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
