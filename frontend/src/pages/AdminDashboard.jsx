import { useCallback, useEffect, useMemo, useState } from 'react'
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, LineChart, Line, CartesianGrid } from 'recharts'
import { adminService } from '../services/orderService'
import { errorMessage } from '../services/api'
import { money } from '../utils/format'
import AdminNav from '../components/AdminNav'
import { ErrorState, Skeleton } from '../components/Feedback'
import { token, useTheme } from '../theme'

const RANGES = [7, 30, 90]

export default function AdminDashboard() {
  const [days, setDays] = useState(30)
  const [data, setData] = useState(null)
  const [error, setError] = useState(null)

  const load = useCallback(() => {
    setError(null)
    Promise.all([
      adminService.dashboard(), adminService.topFoods(days),
      adminService.peakHours(days), adminService.revenue(days),
    ]).then(([stats, top, hours, revenue]) => {
      setData({ stats: stats.data, top: top.data, hours: hours.data, revenue: revenue.data })
    }).catch((e) => setError(errorMessage(e)))
  }, [days])

  useEffect(load, [load])

  // Recharts needs literal colours, so they are read from the theme and re-read on toggle.
  const theme = useTheme()
  const c = useMemo(() => ({
    series: token('--chart-1'), grid: token('--chart-grid'), axis: token('--chart-axis'),
    surface: token('--surface'), surface2: token('--surface-2'), ink: token('--ink'), line: token('--line'),
  }), [theme]) // eslint-disable-line react-hooks/exhaustive-deps
  const axis = { stroke: 'none', tick: { fill: c.axis, fontSize: 12 }, tickLine: false, axisLine: false }
  const tooltip = {
    contentStyle: { background: c.surface, border: `1px solid ${c.line}`, borderRadius: 12, color: c.ink, boxShadow: 'none' },
    labelStyle: { color: c.ink, fontWeight: 600 }, itemStyle: { color: c.ink },
  }

  return (
    <div className="page-pad">
      <h1 className="h2 mb-3">Admin Dashboard</h1>
      <AdminNav />

      {error && <ErrorState message={error} onRetry={load} />}
      {!error && !data && <Skeleton rows={3} height={200} />}

      {data && (
        <>
          <div className="row g-3 mb-4">
            <Kpi label="Today's Orders" value={data.stats.todayOrders} />
            <Kpi label="Today's Revenue" value={money(data.stats.todayRevenue)} />
            <Kpi label="Active Orders" value={data.stats.pendingOrders} />
            <Kpi label="Total Revenue" value={money(data.stats.totalRevenue)} />
            <Kpi label="Users" value={data.stats.totalUsers} />
            <Kpi label="Food Items" value={data.stats.totalFoodItems} />
          </div>

          <div className="d-flex gap-2 mb-3" role="group" aria-label="Date range for charts">
            {RANGES.map((r) => (
              <button key={r} aria-pressed={days === r} onClick={() => setDays(r)}
                      className={`filter-chip ${days === r ? 'active' : ''}`}>
                Last {r} days
              </button>
            ))}
          </div>

          <div className="row g-3">
            <div className="col-12">
              <ChartCard title="Revenue trend">
                <LineChart data={data.revenue} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
                  <CartesianGrid vertical={false} stroke={c.grid} />
                  <XAxis dataKey="date" tickFormatter={(d) => d.slice(5)} minTickGap={24} {...axis} />
                  <YAxis width={56} {...axis} />
                  <Tooltip formatter={(v) => money(v)} cursor={{ stroke: c.axis, strokeWidth: 1 }} {...tooltip} />
                  <Line type="monotone" dataKey="revenue" name="Revenue" stroke={c.series} strokeWidth={2} dot={false}
                        activeDot={{ r: 5, fill: c.series, stroke: c.surface, strokeWidth: 2 }} />
                </LineChart>
              </ChartCard>
            </div>
            <div className="col-12 col-lg-6">
              <ChartCard title="Top selling foods">
                <BarChart data={data.top} layout="vertical" margin={{ top: 0, right: 8, left: 8, bottom: 0 }} barCategoryGap={4}>
                  <CartesianGrid horizontal={false} stroke={c.grid} />
                  <XAxis type="number" allowDecimals={false} {...axis} />
                  <YAxis type="category" dataKey="foodItemName" width={120} {...axis} />
                  <Tooltip cursor={{ fill: c.surface2 }} {...tooltip} />
                  <Bar dataKey="totalQuantity" name="Sold" fill={c.series} radius={[0, 4, 4, 0]} />
                </BarChart>
              </ChartCard>
            </div>
            <div className="col-12 col-lg-6">
              <ChartCard title="Orders by hour of day">
                <BarChart data={data.hours} margin={{ top: 8, right: 8, left: 0, bottom: 0 }} barCategoryGap={2}>
                  <CartesianGrid vertical={false} stroke={c.grid} />
                  <XAxis dataKey="hour" tickFormatter={(h) => `${h}:00`} {...axis} />
                  <YAxis allowDecimals={false} width={40} {...axis} />
                  <Tooltip labelFormatter={(h) => `${h}:00`} cursor={{ fill: c.surface2 }} {...tooltip} />
                  <Bar dataKey="orderCount" name="Orders" fill={c.series} radius={[4, 4, 0, 0]} />
                </BarChart>
              </ChartCard>
            </div>
          </div>
        </>
      )}
    </div>
  )
}

function ChartCard({ title, children }) {
  return (
    <div className="chart-card">
      <h2 className="h6 mb-3">{title}</h2>
      <div style={{ width: '100%', height: 300 }}>
        <ResponsiveContainer>{children}</ResponsiveContainer>
      </div>
    </div>
  )
}

function Kpi({ label, value }) {
  return (
    <div className="col-6 col-md-4 col-lg-2">
      <div className="stat-card">
        <div className="stat-label">{label}</div>
        <div className="stat-value">{value}</div>
      </div>
    </div>
  )
}
