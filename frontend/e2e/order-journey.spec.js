import { test, expect } from '@playwright/test'
import { advanceToReady, login, payInSandbox, placeOrder, registerStudent, STAFF } from './helpers'

test('online order: pay, kitchen, live updates, pickup, then review', async ({ browser }) => {
  const studentContext = await browser.newContext()
  const staffContext = await browser.newContext()
  const student = await studentContext.newPage()
  const staff = await staffContext.newPage()

  await login(staff, STAFF)
  await staff.goto('/staff')
  await expect(staff.getByTestId('live-indicator')).toContainText('Live')

  await registerStudent(student)
  const { token, id } = await placeOrder(student, 'online')

  // Unpaid: the kitchen must not see it yet.
  await expect(student.getByRole('dialog').getByText(/Sandbox/)).toBeVisible()
  await expect(staff.getByTestId(`order-card-${id}`)).toHaveCount(0)

  await payInSandbox(student)

  // LIVE: appears for staff with no reload.
  await expect(staff.getByTestId(`order-card-${id}`)).toBeVisible({ timeout: 5000 })

  await advanceToReady(staff, id)

  // LIVE: the student's page updates with no reload.
  await expect(student.getByTestId('order-status')).toContainText('Ready for pickup', { timeout: 5000 })
  await expect(student.getByTestId('unread-count')).toBeVisible()

  await staff.getByTestId(`order-card-${id}`).getByRole('link', { name: 'Hand over' }).click()
  await expect(staff.getByTestId('scan-result')).toContainText('Paid and ready')
  await staff.getByRole('button', { name: 'Hand Over Food' }).click()
  await expect(staff.getByTestId('scan-result')).toContainText('Already collected')

  await expect(student.getByTestId('order-status')).toContainText('Collected', { timeout: 5000 })

  // Reviews are available only now.
  await student.getByRole('radio', { name: '5 stars' }).click()
  await student.getByRole('button', { name: 'Submit review' }).click()
  await expect(student.getByLabel('You rated this 5 out of 5')).toBeVisible()

  await studentContext.close()
  await staffContext.close()
})

test('cash order: blocked until cash is recorded', async ({ browser }) => {
  const student = await (await browser.newContext()).newPage()
  const staff = await (await browser.newContext()).newPage()
  await registerStudent(student)
  const { token, id } = await placeOrder(student, 'cash')
  await expect(student.getByText(/in cash ready/)).toBeVisible()

  await login(staff, STAFF)
  await staff.goto('/staff')
  await advanceToReady(staff, id)

  await staff.goto(`/staff/scan?token=${token}`)
  await expect(staff.getByRole('button', { name: 'Hand Over Food' })).toHaveCount(0)
  await staff.getByRole('button', { name: 'Cash received' }).click()
  await staff.getByRole('button', { name: 'Hand Over Food' }).click()
  await expect(staff.getByTestId('scan-result')).toContainText('Already collected')
})

test('failed payment can be retried, and the order survives', async ({ page }) => {
  await registerStudent(page)
  await placeOrder(page, 'online')
  await page.getByRole('dialog').getByRole('button', { name: 'Simulate failure' }).click()
  await expect(page.getByTestId('order-status')).toContainText('Awaiting payment')

  await page.getByRole('button', { name: /^Pay / }).click()
  await payInSandbox(page)
})

test('cancelling a paid order refunds it', async ({ page }) => {
  await registerStudent(page)
  await placeOrder(page, 'online')
  await payInSandbox(page)

  page.once('dialog', (d) => d.accept())
  await page.getByRole('button', { name: 'Cancel Order' }).click()
  await expect(page.getByTestId('order-status')).toContainText('Cancelled')
  await expect(page.getByText('Refunded')).toBeVisible()
})

test('a full slot cannot be chosen', async ({ page, request }) => {
  await registerStudent(page)
  const token = await page.evaluate(() => localStorage.getItem('token'))
  const headers = { Authorization: `Bearer ${token}` }

  const slots = await (await request.get('/api/canteen/slots')).json()
  const slot = slots[slots.length - 1]
  const foods = await (await request.get('/api/foods?available=true')).json()
  for (let i = 0; i < slot.remaining; i += 1) {
    const res = await request.post('/api/orders', {
      headers,
      data: { items: [{ foodItemId: foods[0].id, quantity: 1 }], paymentMethod: 'CASH_ON_PICKUP', pickupSlot: slot.start },
    })
    expect(res.status()).toBe(201)
  }

  await page.goto('/menu')
  await page.getByRole('button', { name: /^Add .* to cart$/ }).first().click()
  await page.goto('/checkout')
  await expect(page.locator('.slot-option', { hasText: 'Full' }).first()).toBeDisabled()

  const overbook = await request.post('/api/orders', {
    headers,
    data: { items: [{ foodItemId: foods[0].id, quantity: 1 }], paymentMethod: 'CASH_ON_PICKUP', pickupSlot: slot.start },
  })
  expect(overbook.status()).toBe(409)
})
