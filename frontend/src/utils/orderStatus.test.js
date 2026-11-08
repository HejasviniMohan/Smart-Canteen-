import { describe, expect, it } from 'vitest'
import { canStaffCancel, canStudentCancel, NEXT_KITCHEN_STATUS, timelineSteps } from './orderStatus'

describe('cancel rules match the server', () => {
  it('students can cancel only before cooking starts', () => {
    expect(canStudentCancel('PLACED')).toBe(true)
    expect(canStudentCancel('ACCEPTED')).toBe(true)
    expect(canStudentCancel('PREPARING')).toBe(false)
    expect(canStudentCancel('READY')).toBe(false)
  })

  it('staff can cancel up to preparing, but not once ready', () => {
    expect(canStaffCancel('PREPARING')).toBe(true)
    expect(canStaffCancel('READY')).toBe(false)
  })
})

describe('kitchen progression', () => {
  it('has no next step from READY, because handover goes through pickup', () => {
    expect(NEXT_KITCHEN_STATUS.READY).toBeUndefined()
  })
})

describe('timelineSteps', () => {
  it('marks earlier steps done and the current one active', () => {
    const states = timelineSteps('PREPARING').map((s) => s.state)
    expect(states).toEqual(['done', 'done', 'active', 'todo', 'todo'])
  })

  it('marks everything done once collected', () => {
    expect(timelineSteps('COMPLETED').every((s) => s.state === 'done')).toBe(true)
  })
})
