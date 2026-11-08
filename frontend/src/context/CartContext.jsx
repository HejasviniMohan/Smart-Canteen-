import { createContext, useContext, useEffect, useRef, useState } from 'react'
import { addItem, cartCount, cartTotal, setQuantity } from '../utils/cart'
import { useAuth } from './AuthContext'

const CartContext = createContext(null)

function readStoredCart() {
  try {
    return JSON.parse(localStorage.getItem('cart')) || []
  } catch {
    return []
  }
}

export function CartProvider({ children }) {
  const { user } = useAuth()
  const [items, setItems] = useState(readStoredCart)
  const wasLoggedIn = useRef(!!user)

  useEffect(() => {
    localStorage.setItem('cart', JSON.stringify(items))
  }, [items])

  // Stops the next person on a shared device from inheriting this cart.
  useEffect(() => {
    if (wasLoggedIn.current && !user) setItems([])
    wasLoggedIn.current = !!user
  }, [user])

  const value = {
    items,
    add: (food, quantity = 1) => setItems((prev) => addItem(prev, food, quantity)),
    updateQty: (id, quantity) => setItems((prev) => setQuantity(prev, id, quantity)),
    remove: (id) => setItems((prev) => prev.filter((i) => i.id !== id)),
    clear: () => setItems([]),
    replace: setItems,
    total: cartTotal(items),
    count: cartCount(items),
  }

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>
}

export const useCart = () => {
  const ctx = useContext(CartContext)
  if (!ctx) throw new Error('useCart must be used inside CartProvider')
  return ctx
}
