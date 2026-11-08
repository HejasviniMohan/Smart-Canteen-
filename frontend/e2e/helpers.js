import { expect } from '@playwright/test'

export const STAFF = { email: 'staff@canteen.com', password: 'Staff@123' }
export const ADMIN = { email: 'admin@canteen.com', password: 'Admin@123' }

export async function login(page, { email, password }) {
  await page.goto('/login')
  await page.getByLabel('Email').fill(email)
  await page.getByLabel('Password').fill(password)
  await page.locator('form').getByRole('button', { name: 'Login' }).click()
  await expect(page).not.toHaveURL(/\/login/)
}

let counter = 0
export async function registerStudent(page) {
  counter += 1
  const email = `e2e-${Date.now()}-${counter}@test.com`
  await page.goto('/register')
  await page.getByLabel('Full name').fill(`Student ${counter}`)
  await page.getByLabel('Email').fill(email)
  await page.getByLabel('Password').fill('password1')
  await page.getByRole('button', { name: 'Create account' }).click()
  await expect(page).toHaveURL(/\/menu/)
  return email
}

/**
 * Adds the first available item and places an order. The page ends on the order screen.
 * Returns the pickup token and the order id. Tokens restart every pickup day, so the staff
 * board can show the same token twice (today's and tomorrow's); the id is what's unique.
 */
export async function placeOrder(page, method) {
  await page.goto('/menu')
  await page.getByRole('button', { name: /^Add .* to cart$/ }).first().click()
  await page.goto('/checkout')
  await expect(page.getByRole('radio', { checked: true }).first()).toBeVisible()
  if (method === 'cash') await page.getByLabel(/Cash at pickup/).check()
  await page.getByRole('button', { name: method === 'cash' ? /Place Order/ : /Continue to Payment/ }).click()
  await expect(page).toHaveURL(/\/orders\/\d+/)
  const id = page.url().match(/\/orders\/(\d+)/)[1]
  return { token: await page.getByTestId('order-token').innerText(), id }
}

export async function payInSandbox(page) {
  const dialog = page.getByRole('dialog')
  await expect(dialog.getByText(/Sandbox/)).toBeVisible()
  await dialog.getByRole('button', { name: 'Simulate success' }).click()
  await expect(page.getByTestId('order-status')).toContainText('Order placed')
}

/** Staff moves an order through the kitchen, using the card on the dashboard. */
export async function advanceToReady(staffPage, id) {
  const card = staffPage.getByTestId(`order-card-${id}`)
  await expect(card).toBeVisible()
  for (const step of [/Mark accepted/, /Mark preparing/, /Mark ready for pickup/]) {
    await card.getByRole('button', { name: step }).click()
  }
  await expect(card.getByRole('link', { name: 'Hand over' })).toBeVisible()
}
