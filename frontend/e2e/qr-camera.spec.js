import { test, expect, chromium } from '@playwright/test'
import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import { advanceToReady, login, placeOrder, registerStudent, STAFF } from './helpers'

const WIDTH = 640
const HEIGHT = 480

/** Draws the page's QR code as a video file for Chromium's fake camera (Y4M, grayscale). */
async function qrAsFakeCameraVideo(page) {
  // The QR is painted after the canvas mounts. Copying it too early records a blank
  // white frame that can never be decoded, so wait until it really has dark modules.
  await expect.poll(() => page.evaluate(() => {
    const qr = document.querySelector('.od-qr-box canvas')
    const { data } = qr.getContext('2d').getImageData(0, 0, qr.width, qr.height)
    let dark = 0
    for (let i = 0; i < data.length; i += 4) if (data[i] < 128) dark += 1
    return dark / (data.length / 4)
  })).toBeGreaterThan(0.2)

  // A real camera never sends the same frame twice. A single frozen frame would turn the
  // decoder's rare misses on one particular image into permanent failures, so the feed
  // varies the size and position a little, as a hand-held phone would.
  const variations = [[2, 0, 0], [1.8, 9, -7], [2.2, -11, 6], [1.9, 5, 12]]
  const frames = await page.evaluate(([w, h, vars]) => {
    const qr = document.querySelector('.od-qr-box canvas')
    return vars.map(([scale, dx, dy]) => {
      const canvas = document.createElement('canvas')
      canvas.width = w
      canvas.height = h
      const ctx = canvas.getContext('2d')
      ctx.fillStyle = '#fff'
      ctx.fillRect(0, 0, w, h)
      ctx.imageSmoothingEnabled = false
      const size = Math.round(qr.width * scale)
      ctx.drawImage(qr, (w - size) / 2 + dx, (h - size) / 2 + dy, size, size)
      const { data } = ctx.getImageData(0, 0, w, h)
      const luma = []
      for (let i = 0; i < data.length; i += 4) luma.push(data[i])
      return luma
    })
  }, [WIDTH, HEIGHT, variations])

  const uv = Buffer.alloc((WIDTH * HEIGHT) / 2, 128)
  const file = path.join(fs.mkdtempSync(path.join(os.tmpdir(), 'qr-')), 'qr.y4m')
  const encoded = frames.map((luma) => Buffer.concat([Buffer.from('FRAME\n'), Buffer.from(luma), uv]))
  fs.writeFileSync(file, Buffer.concat([
    Buffer.from(`YUV4MPEG2 W${WIDTH} H${HEIGHT} F4:1 Ip A1:1 C420\n`),
    ...encoded,
  ]))
  return file
}

test('staff scan the student\'s real QR code with the camera, and it works only once', async ({ browser, baseURL }) => {
  const student = await (await browser.newContext()).newPage()
  await registerStudent(student)
  const { token, id } = await placeOrder(student, 'cash')
  await expect(student.locator('.od-qr-box canvas')).toBeVisible()
  const video = await qrAsFakeCameraVideo(student)

  const cameraBrowser = await chromium.launch({
    args: ['--use-fake-ui-for-media-stream', '--use-fake-device-for-media-stream', `--use-file-for-fake-video-capture=${video}`],
  })
  const staff = await (await cameraBrowser.newContext({ baseURL, permissions: ['camera'] })).newPage()
  await login(staff, STAFF)
  await staff.goto('/staff')
  await advanceToReady(staff, id)

  await staff.goto('/staff/scan')
  await staff.getByRole('button', { name: 'Scan QR with camera' }).click()
  const result = staff.getByTestId('scan-result')
  await expect(result).toContainText(`Token ${token}`, { timeout: 20000 })

  await staff.getByRole('button', { name: 'Cash received' }).click()
  await staff.getByRole('button', { name: 'Hand Over Food' }).click()
  await expect(result).toContainText('Already collected')

  // Scanning the same code again shows it was used, and offers no way to hand over.
  await staff.getByRole('button', { name: 'Next customer' }).click()
  await staff.getByRole('button', { name: 'Scan QR with camera' }).click()
  await expect(result).toContainText('Already collected', { timeout: 20000 })
  await expect(staff.getByRole('button', { name: 'Hand Over Food' })).toHaveCount(0)

  await cameraBrowser.close()
})
