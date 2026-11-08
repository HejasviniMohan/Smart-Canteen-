import { describe, expect, it } from 'vitest'
import { parseSseChunk } from './liveStream'

describe('parseSseChunk', () => {
  it('reads a complete event', () => {
    const { events, remainder } = parseSseChunk('event:order\ndata:{"orderId":1}\n\n')
    expect(events).toEqual([{ name: 'order', data: '{"orderId":1}' }])
    expect(remainder).toBe('')
  })

  it('keeps an incomplete event for the next chunk', () => {
    const first = parseSseChunk('event:order\ndata:{"order')
    expect(first.events).toEqual([])
    const second = parseSseChunk(`${first.remainder}Id":1}\n\n`)
    expect(second.events[0].data).toBe('{"orderId":1}')
  })

  it('ignores heartbeat comments', () => {
    expect(parseSseChunk(':ping\n\n').events).toEqual([])
  })

  it('reads several events from one chunk', () => {
    const { events } = parseSseChunk('event:a\ndata:1\n\nevent:b\ndata:2\n\n')
    expect(events.map((e) => e.name)).toEqual(['a', 'b'])
  })
})
