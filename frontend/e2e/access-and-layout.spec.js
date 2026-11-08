import { test, expect, devices } from '@playwright/test'
import { ADMIN, login, placeOrder, registerStudent, STAFF } from './helpers'

test('sign-up offers no way to choose a role', async ({ page }) => {
  await page.goto('/register')
  await expect(page.getByLabel('Role')).toHaveCount(0)
})

test('students cannot open staff or admin pages', async ({ page }) => {
  await registerStudent(page)
  for (const path of ['/staff', '/staff/scan', '/admin', '/admin/users', '/admin/payments']) {
    await page.goto(path)
    await expect(page).toHaveURL(/\/$/)
  }
})

test('logged-out visitors are sent to login', async ({ page }) => {
  await page.goto('/orders')
  await expect(page).toHaveURL(/\/login/)
})

test('refreshing a deep link works', async ({ page }) => {
  await page.goto('/menu')
  await page.reload()
  await expect(page.getByPlaceholder(/Search for dosa/)).toBeVisible()
})

test('search from the home page carries through to the menu', async ({ page }) => {
  await page.goto('/')
  await page.getByLabel('Search food').fill('dosa')
  await page.getByRole('search').getByRole('button', { name: 'Search' }).click()
  await expect(page).toHaveURL(/q=dosa/)
  await expect(page.locator('.sc-food-name', { hasText: 'Masala Dosa' })).toBeVisible()
  await expect(page.locator('.sc-food-name', { hasText: 'Filter Coffee' })).toHaveCount(0)
})

test('admin can reach every section, with no errors on any page', async ({ page }) => {
  const errors = []
  page.on('pageerror', (e) => errors.push(e.message))
  await login(page, ADMIN)
  await expect(page.getByRole('heading', { name: 'Admin Dashboard' })).toBeVisible()
  await expect(page.getByText('Total Revenue')).toBeVisible()
  for (const [link, heading] of [['Orders', 'Orders'], ['Payments', 'Payments'], ['Foods', 'Foods'],
    ['Categories', 'Categories'], ['Users', 'Users'], ['Reviews', 'Reviews']]) {
    await page.getByRole('navigation', { name: 'Admin sections' }).getByRole('link', { name: link }).click()
    await expect(page.getByRole('heading', { name: heading, exact: true })).toBeVisible()
  }
  expect(errors).toEqual([])
})

test('admin can add a food, and it appears on the menu', async ({ page }) => {
  const name = `E2E Thali ${Date.now()}`
  await login(page, ADMIN)
  await page.goto('/admin/foods')
  await page.getByRole('button', { name: '+ Add Food' }).click()
  await page.getByLabel('Name').fill(name)
  await page.getByLabel('Price (₹)', { exact: true }).fill('99')
  await page.getByRole('button', { name: 'Save' }).click()
  await expect(page.getByRole('cell', { name })).toBeVisible()

  await page.goto(`/menu?q=${encodeURIComponent(name)}`)
  await expect(page.getByText(name)).toBeVisible()

  await page.goto('/admin/foods')
  page.once('dialog', (d) => d.accept())
  await page.getByRole('row', { name: new RegExp(name) }).getByRole('button', { name: 'Remove' }).click()
  await expect(page.getByRole('cell', { name })).toHaveCount(0)
})

test('admin can create a staff account that can log in', async ({ page, browser }) => {
  const email = `staff-${Date.now()}@test.com`
  await login(page, ADMIN)
  await page.goto('/admin/users')
  await page.getByRole('button', { name: '+ Add Staff' }).click()
  await page.getByLabel('Name').fill('New Staff')
  await page.getByLabel('Email').fill(email)
  await page.getByLabel('Temporary password').fill('staffpass1')
  await page.getByRole('button', { name: 'Create' }).click()
  await expect(page.getByRole('cell', { name: email })).toBeVisible()

  const other = await (await browser.newContext()).newPage()
  await login(other, { email, password: 'staffpass1' })
  await expect(other).toHaveURL(/\/staff/)
})

test.describe('phone width', () => {
  test.use({ viewport: devices['Pixel 7'].viewport, hasTouch: true, isMobile: true })

  async function expectNoSideScroll(page, path) {
    await page.goto(path)
    await page.waitForTimeout(400)
    const overflow = await page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth)
    expect(overflow, `${path} scrolls sideways by ${overflow}px`).toBeLessThanOrEqual(1)
  }

  test('student pages fit without sideways scrolling', async ({ page }) => {
    for (const path of ['/', '/menu', '/specials', '/login', '/register']) await expectNoSideScroll(page, path)
    await registerStudent(page)
    await page.goto('/menu')
    await page.getByRole('button', { name: /^Add .* to cart$/ }).first().click()
    for (const path of ['/cart', '/checkout', '/orders', '/profile']) await expectNoSideScroll(page, path)
    await placeOrder(page, 'cash')
    await expectNoSideScroll(page, page.url().replace(/^.*\/\/[^/]+/, ''))
  })

  test('staff pages fit without sideways scrolling', async ({ page }) => {
    await login(page, STAFF)
    for (const path of ['/staff', '/staff/scan']) await expectNoSideScroll(page, path)
  })

  test('admin pages fit without sideways scrolling', async ({ page }) => {
    await login(page, ADMIN)
    for (const path of ['/admin', '/admin/orders', '/admin/payments', '/admin/foods', '/admin/users', '/admin/reviews']) {
      await expectNoSideScroll(page, path)
    }
  })
})

test('dark mode follows the system, can be toggled, and is remembered', async ({ browser }) => {
  const context = await browser.newContext({ colorScheme: 'dark' })
  const page = await context.newPage()
  const theme = () => page.evaluate(() => document.documentElement.dataset.theme)
  const background = () => page.evaluate(() => getComputedStyle(document.body).backgroundColor)

  await page.goto('/')
  expect(await theme()).toBe('dark')
  expect(await background()).toBe('rgb(10, 10, 10)')

  // The switch animates through a view transition, so wait for it rather than reading at once.
  await page.getByRole('button', { name: 'Switch to light theme' }).click()
  await expect.poll(theme).toBe('light')
  await expect.poll(background).toBe('rgb(236, 239, 243)')

  // The choice outlives a reload, even though the system still prefers dark.
  await page.reload()
  expect(await theme()).toBe('light')
  await expect(page.getByRole('button', { name: 'Switch to dark theme' })).toBeVisible()
  await context.close()
})
