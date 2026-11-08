import { API_BASE } from './api'

/**
 * Parses text/event-stream chunks. Returns complete events plus the unfinished remainder,
 * because a network chunk can end in the middle of an event.
 */
export function parseSseChunk(buffer) {
  const events = []
  const blocks = buffer.split('\n\n')
  const remainder = blocks.pop()
  for (const block of blocks) {
    let name = 'message'
    const data = []
    for (const line of block.split('\n')) {
      if (line.startsWith('event:')) name = line.slice(6).trim()
      else if (line.startsWith('data:')) data.push(line.slice(5).trim())
    }
    if (data.length > 0) events.push({ name, data: data.join('\n') })
  }
  return { events, remainder }
}

/**
 * Connects to the live-updates stream and reconnects with increasing delays. It uses fetch
 * rather than the browser's EventSource because EventSource can't send an Authorization
 * header, and putting the JWT in the URL would leak it into server logs.
 */
export function openLiveStream({ onEvent, onConnected }) {
  const controller = new AbortController()
  let attempt = 0

  const run = async () => {
    while (!controller.signal.aborted) {
      try {
        const response = await fetch(`${API_BASE}/events/stream`, {
          headers: { Authorization: `Bearer ${localStorage.getItem('token')}` },
          signal: controller.signal,
        })
        if (response.status === 401) return
        if (!response.ok) throw new Error(`Stream failed: ${response.status}`)

        attempt = 0
        onConnected?.()

        const reader = response.body.pipeThrough(new TextDecoderStream()).getReader()
        let buffer = ''
        while (true) {
          const { value, done } = await reader.read()
          if (done) break
          const parsed = parseSseChunk(buffer + value)
          buffer = parsed.remainder
          parsed.events.filter((e) => e.name === 'order').forEach((e) => onEvent(JSON.parse(e.data)))
        }
      } catch {
        if (controller.signal.aborted) return
      }
      attempt += 1
      const delay = Math.min(30000, 1000 * 2 ** attempt)
      await new Promise((resolve) => setTimeout(resolve, delay))
    }
  }
  run()

  return () => controller.abort()
}
