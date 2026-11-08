import { describe, expect, it } from 'vitest'
import { addItem, cartCount, cartTotal, setQuantity, syncWithMenu } from './cart'

const dosa = { id: 1, name: 'Dosa', price: '80.00', effectivePrice: '65.00', isVeg: true }

describe('addItem', () => {
  it('uses the price the server will charge, not the list price', () => {
    expect(addItem([], dosa)[0].price).toBe(65)
  })

  it('increases the quantity of an item already in the cart', () => {
    const cart = addItem(addItem([], dosa), dosa, 2)
    expect(cart).toHaveLength(1)
    expect(cart[0].quantity).toBe(3)
  })
})

describe('setQuantity', () => {
  it('removes an item when its quantity reaches zero', () => {
    expect(setQuantity(addItem([], dosa), 1, 0)).toEqual([])
  })
})

describe('totals', () => {
  it('adds up price times quantity', () => {
    const cart = [{ price: 65, quantity: 2 }, { price: 10.5, quantity: 1 }]
    expect(cartTotal(cart)).toBe(140.5)
    expect(cartCount(cart)).toBe(3)
  })
})

describe('syncWithMenu', () => {
  const cart = [
    { id: 1, name: 'Dosa', price: 65, quantity: 1 },
    { id: 2, name: 'Tea', price: 10, quantity: 1 },
    { id: 3, name: 'Soup', price: 50, quantity: 1 },
  ]

  it('updates changed prices and removes unavailable or deleted items', () => {
    const menu = [
      { id: 1, name: 'Dosa', effectivePrice: '70.00', availability: true },
      { id: 2, name: 'Tea', effectivePrice: '10.00', availability: false },
    ]
    const result = syncWithMenu(cart, menu)
    expect(result.items).toEqual([{ id: 1, name: 'Dosa', price: 70, quantity: 1 }])
    expect(result.repriced).toEqual(['Dosa'])
    expect(result.removed).toEqual(['Tea', 'Soup'])
  })

  it('reports no changes when the cart is already correct', () => {
    const menu = [{ id: 1, name: 'Dosa', effectivePrice: '65.00', availability: true }]
    const result = syncWithMenu([cart[0]], menu)
    expect(result.removed).toEqual([])
    expect(result.repriced).toEqual([])
  })
})
