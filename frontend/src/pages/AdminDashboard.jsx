import { useEffect, useState } from 'react'
import { adminService } from '../services/orderService'
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, LineChart, Line, CartesianGrid } from 'recharts'

export default function AdminDashboard() {
  const [stats, setStats] = useState(null)
  const [top, setTop] = useState([])
  const [hours, setHours] = useState([])

  useEffect(() => {
    adminService.dashboard().then((r) => setStats(r.data)).catch(() => {})
    adminService.topFoods().then((r) => setTop(r.data)).catch(() => {})
    adminService.peakHours().then((r) => setHours(r.data)).catch(() => {})
  }, [])

  return (
    <div className="page-pad">
      <h2 className="mb-3">Admin Dashboard</h2>

      {stats && (
        <div className="row g-3 mb-4">
          <Kpi label="Today's Orders" value={stats.todayOrders} icon="📦" />
          <Kpi label="Today's Revenue" value={`₹${Number(stats.todayRevenue).toFixed(0)}`} icon="💰" />
          <Kpi label="Pending Orders" value={stats.pendingOrders} icon="⏳" />
          <Kpi label="Total Revenue" value={`₹${Number(stats.totalRevenue).toFixed(0)}`} icon="💸" />
          <Kpi label="Users" value={stats.totalUsers} icon="👥" />
          <Kpi label="Food Items" value={stats.totalFoodItems} icon="🍱" />
        </div>
      )}

      <div className="row g-3">
        <div className="col-12 col-lg-6">
          <div className="card shadow-sm p-3">
            <h5>Top selling foods (last 30 days)</h5>
            <div style={{ width: '100%', height: 280 }}>
              <ResponsiveContainer>
                <BarChart data={top}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="foodItemName" hide />
                  <YAxis />
                  <Tooltip />
                  <Bar dataKey="totalQuantity" fill="#ff6b35" />
                </BarChart>
              </ResponsiveContainer>
            </div>
            <ul className="small mt-2">
              {top.slice(0, 5).map((t) => (
                <li key={t.foodItemId}>{t.foodItemName} — {t.totalQuantity} sold</li>
              ))}
            </ul>
          </div>
        </div>

        <div className="col-12 col-lg-6">
          <div className="card shadow-sm p-3">
            <h5>Peak order hours (last 7 days)</h5>
            <div style={{ width: '100%', height: 280 }}>
              <ResponsiveContainer>
                <LineChart data={hours}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="hour" />
                  <YAxis />
                  <Tooltip />
                  <Line type="monotone" dataKey="orderCount" stroke="#ff6b35" strokeWidth={2} />
                </LineChart>
              </ResponsiveContainer>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

function Kpi({ label, value, icon }) {
  return (
    <div className="col-6 col-md-4 col-lg-2">
      <div className="card shadow-sm p-3 h-100">
        <div style={{ fontSize: '1.5rem' }}>{icon}</div>
        <div className="text-muted small">{label}</div>
        <div className="fs-4 fw-bold">{value}</div>
      </div>
    </div>
  )
}