import { createContext, useContext, useEffect, useState } from 'react'

const CartContext = createContext(null)

export function CartProvider({ children }) {
  const [items, setItems] = useState(() => {
    const raw = localStorage.getItem('cart')
    return raw ? JSON.parse(raw) : []
  })

  useEffect(() => {
    localStorage.setItem('cart', JSON.stringify(items))
  }, [items])

  const add = (food, qty = 1) => {
    setItems((prev) => {
      const found = prev.find((i) => i.id === food.id)
      if (found) {
        return prev.map((i) => i.id === food.id ? { ...i, quantity: i.quantity + qty } : i)
      }
      return [...prev, {
        id: food.id,
        name: food.name,
        price: Number(food.price),
        imageUrl: food.imageUrl,
        isVeg: food.isVeg,
        quantity: qty,
      }]
    })
  }

  const updateQty = (id, quantity) => {
    setItems((prev) => prev
          .map((i) => i.id === id ? { ...i, quantity } : i)
          .filter((i) => i.quantity > 0))
  }

  const remove = (id) => setItems((prev) => prev.filter((i) => i.id !== id))

  const clear = () => setItems([])

  const total = items.reduce((sum, i) => sum + i.price * i.quantity, 0)
  const count = items.reduce((sum, i) => sum + i.quantity, 0)

  return (
    <CartContext.Provider value={{ items, add, updateQty, remove, clear, total, count }}>
      {children}
    </CartContext.Provider>
  )
}

export const useCart = () => {
  const ctx = useContext(CartContext)
  if (!ctx) throw new Error('useCart must be used inside CartProvider')
  return ctx
}