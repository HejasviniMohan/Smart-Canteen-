import { useEffect, useRef, useState } from 'react'
import { BrowserQRCodeReader } from '@zxing/browser'

const SCAN_INTERVAL_MS = 200
// ZXing's binarizer sometimes fails on a large, sharp image that it reads easily when
// smaller. Each frame is tried at a few sizes; each attempt takes milliseconds.
const SCALES = [1, 0.5, 0.75, 0.35]

/**
 * Reads QR codes from the camera. Browsers only allow camera access on HTTPS or localhost.
 *
 * This component runs the camera and the scan loop itself and uses ZXing only to decode
 * single frames. ZXing's own video helper can wait forever: if the video starts playing
 * before it attaches its 'canplay' listener, its promise never resolves and scanning
 * silently never begins.
 */
export default function QrScanner({ onScan, onClose }) {
  const video = useRef(null)
  const latestOnScan = useRef(onScan)
  const [error, setError] = useState(null)

  useEffect(() => {
    latestOnScan.current = onScan
  })

  useEffect(() => {
    let stream = null
    let timer = null
    let stopped = false
    const reader = new BrowserQRCodeReader()
    const frame = document.createElement('canvas')
    const context = frame.getContext('2d', { willReadFrequently: true })

    const decode = (v) => {
      for (const scale of SCALES) {
        frame.width = Math.round(v.videoWidth * scale)
        frame.height = Math.round(v.videoHeight * scale)
        context.drawImage(v, 0, 0, frame.width, frame.height)
        try {
          return reader.decodeFromCanvas(frame).getText()
        } catch {
          // Not found at this size. Try the next one.
        }
      }
      return null
    }

    const stop = () => {
      stopped = true
      clearTimeout(timer)
      stream?.getTracks().forEach((track) => track.stop())
    }

    const scan = () => {
      if (stopped) return
      const v = video.current
      const text = v && v.videoWidth > 0 ? decode(v) : null
      if (text) {
        stop()
        latestOnScan.current(text)
        return
      }
      timer = setTimeout(scan, SCAN_INTERVAL_MS)
    }

    navigator.mediaDevices.getUserMedia({ video: { facingMode: { ideal: 'environment' } } })
      .then((s) => {
        stream = s
        if (stopped) {
          stop()
          return
        }
        video.current.srcObject = s
        // Starting playback can be refused (for example by autoplay rules), and the
        // scan loop only needs frames, so it starts regardless of play() succeeding.
        video.current.play().catch(() => {})
        scan()
      })
      .catch(() => {
        if (!stopped) setError('The camera could not be opened. Allow camera access (HTTPS or localhost is required), or enter the token below.')
      })

    return stop
  }, [])

  return (
    <div className="scanner-box">
      {error
        ? <p className="text-danger mb-2" role="alert">{error}</p>
        : <video ref={video} muted playsInline aria-label="Camera preview for scanning a QR code" />}
      <button type="button" className="btn btn-outline-secondary btn-sm mt-2" onClick={onClose}>Close camera</button>
    </div>
  )
}
