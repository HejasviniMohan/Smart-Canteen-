import { lazy, Suspense, useEffect } from 'react'
import { Routes, Route, Navigate, useLocation } from 'react-router-dom'
import { AuthProvider, useAuth } from './context/AuthContext'
import { CartProvider } from './context/CartContext'
import { LiveUpdatesProvider } from './context/LiveUpdatesContext'
import { NotificationsProvider } from './context/NotificationsContext'
import PremiumNavbar from './components/PremiumNavbar'
import Footer from './components/Footer'
import FloatingCart from './components/FloatingCart'
import MobileBottomNav from './components/MobileBottomNav'
import ErrorBoundary from './components/ErrorBoundary'
import { Skeleton } from './components/Feedback'

// The menu loads immediately. Heavier pages, like the charts and the camera scanner,
// download only when someone opens them.
const Login = lazy(() => import('./pages/Login'))
const Register = lazy(() => import('./pages/Register'))
const Home = lazy(() => import('./pages/Home'))
const Menu = lazy(() => import('./pages/Menu'))
const TodaySpecials = lazy(() => import('./pages/TodaySpecials'))
const CartPage = lazy(() => import('./pages/CartPage'))
const Checkout = lazy(() => import('./pages/Checkout'))
const MyOrders = lazy(() => import('./pages/MyOrders'))
const OrderDetail = lazy(() => import('./pages/OrderDetail'))
const Profile = lazy(() => import('./pages/Profile'))
const StaffDashboard = lazy(() => import('./pages/StaffDashboard'))
const StaffScan = lazy(() => import('./pages/StaffScan'))
const AdminDashboard = lazy(() => import('./pages/AdminDashboard'))
const AdminOrders = lazy(() => import('./pages/AdminOrders'))
const AdminPayments = lazy(() => import('./pages/AdminPayments'))
const AdminReviews = lazy(() => import('./pages/AdminReviews'))
const ManageFoods = lazy(() => import('./pages/ManageFoods'))
const ManageCategories = lazy(() => import('./pages/ManageCategories'))
const ManageUsers = lazy(() => import('./pages/ManageUsers'))
const QrLanding = lazy(() => import('./pages/QrLanding'))

function ProtectedRoute({ children, roles }) {
  const { user, isAuthenticated } = useAuth()
  if (!isAuthenticated) return <Navigate to="/login" replace />
  if (roles && !roles.includes(user.role)) return <Navigate to="/" replace />
  return children
}

function Shell() {
  const { pathname } = useLocation()
  // Each section of the site (home, menu, orders, admin...) fades in when you arrive.
  // Moving within a section, such as between menu categories, doesn't replay it.
  const section = pathname.split('/')[1] || 'home'

  // A new page starts at the top, the way a normal link would.
  useEffect(() => { window.scrollTo({ top: 0, left: 0, behavior: 'instant' }) }, [pathname])

  return (
    <div className="app-shell">
      <PremiumNavbar />
      <main style={{ flex: 1 }}>
        <ErrorBoundary>
        <div key={section} className="route-enter">
        <Suspense fallback={<div className="page-pad"><Skeleton rows={3} height={160} /></div>}>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/menu" element={<Menu />} />
          <Route path="/menu/:categoryId" element={<Menu />} />
          <Route path="/specials" element={<TodaySpecials />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/qr/:canteenId" element={<QrLanding />} />

          <Route path="/cart" element={<ProtectedRoute roles={['STUDENT']}><CartPage /></ProtectedRoute>} />
          <Route path="/checkout" element={<ProtectedRoute roles={['STUDENT']}><Checkout /></ProtectedRoute>} />
          <Route path="/orders" element={<ProtectedRoute roles={['STUDENT']}><MyOrders /></ProtectedRoute>} />
          <Route path="/orders/:id" element={<ProtectedRoute><OrderDetail /></ProtectedRoute>} />
          <Route path="/profile" element={<ProtectedRoute><Profile /></ProtectedRoute>} />

          <Route path="/staff" element={<ProtectedRoute roles={['STAFF','ADMIN']}><StaffDashboard /></ProtectedRoute>} />
          <Route path="/staff/scan" element={<ProtectedRoute roles={['STAFF','ADMIN']}><StaffScan /></ProtectedRoute>} />

          <Route path="/admin" element={<ProtectedRoute roles={['ADMIN']}><AdminDashboard /></ProtectedRoute>} />
          <Route path="/admin/foods" element={<ProtectedRoute roles={['ADMIN']}><ManageFoods /></ProtectedRoute>} />
          <Route path="/admin/categories" element={<ProtectedRoute roles={['ADMIN']}><ManageCategories /></ProtectedRoute>} />
          <Route path="/admin/users" element={<ProtectedRoute roles={['ADMIN']}><ManageUsers /></ProtectedRoute>} />
          <Route path="/admin/orders" element={<ProtectedRoute roles={['ADMIN']}><AdminOrders /></ProtectedRoute>} />
          <Route path="/admin/payments" element={<ProtectedRoute roles={['ADMIN']}><AdminPayments /></ProtectedRoute>} />
          <Route path="/admin/reviews" element={<ProtectedRoute roles={['ADMIN']}><AdminReviews /></ProtectedRoute>} />

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
        </Suspense>
        </div>
        </ErrorBoundary>
      </main>
      <Footer />
      <FloatingCart />
      <MobileBottomNav />
    </div>
  )
}

export default function App() {
  return (
    <AuthProvider>
      <LiveUpdatesProvider>
        <NotificationsProvider>
          <CartProvider>
            <Shell />
          </CartProvider>
        </NotificationsProvider>
      </LiveUpdatesProvider>
    </AuthProvider>
  )
}
