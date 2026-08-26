import { Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider, useAuth } from './context/AuthContext'
import { CartProvider } from './context/CartContext'
import PremiumNavbar from './components/PremiumNavbar'
import Footer from './components/Footer'
import FloatingCart from './components/FloatingCart'
import MobileBottomNav from './components/MobileBottomNav'
import Login from './pages/Login'
import Register from './pages/Register'
import Home from './pages/Home'
import Menu from './pages/Menu'
import TodaySpecials from './pages/TodaySpecials'
import CartPage from './pages/CartPage'
import Checkout from './pages/Checkout'
import MyOrders from './pages/MyOrders'
import OrderDetail from './pages/OrderDetail'
import Profile from './pages/Profile'
import StaffDashboard from './pages/StaffDashboard'
import StaffScan from './pages/StaffScan'
import AdminDashboard from './pages/AdminDashboard'
import AdminOrders from './pages/AdminOrders'
import ManageFoods from './pages/ManageFoods'
import ManageCategories from './pages/ManageCategories'
import ManageUsers from './pages/ManageUsers'
import QrLanding from './pages/QrLanding'

function ProtectedRoute({ children, roles }) {
  const { user, isAuthenticated } = useAuth()
  if (!isAuthenticated) return <Navigate to="/login" replace />
  if (roles && !roles.includes(user.role)) return <Navigate to="/" replace />
  return children
}

function Shell() {
  return (
    <div className="app-shell">
      <PremiumNavbar />
      <main style={{ flex: 1 }}>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/menu" element={<Menu />} />
          <Route path="/menu/:categoryId" element={<Menu />} />
          <Route path="/specials" element={<TodaySpecials />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/qr/:canteenId" element={<QrLanding />} />

          <Route path="/cart" element={<ProtectedRoute><CartPage /></ProtectedRoute>} />
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

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
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
      <CartProvider>
        <Shell />
      </CartProvider>
    </AuthProvider>
  )
}
