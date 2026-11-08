export const STATUS_LABEL = {
  PENDING_PAYMENT: 'Awaiting payment',
  PLACED: 'Order placed',
  ACCEPTED: 'Accepted',
  PREPARING: 'Preparing',
  READY: 'Ready for pickup',
  COMPLETED: 'Collected',
  CANCELLED: 'Cancelled',
}

/** The kitchen's next step. READY is absent: handover happens only through the pickup screen. */
export const NEXT_KITCHEN_STATUS = { PLACED: 'ACCEPTED', ACCEPTED: 'PREPARING', PREPARING: 'READY' }

export const TIMELINE = ['PLACED', 'ACCEPTED', 'PREPARING', 'READY', 'COMPLETED']

export const canStudentCancel = (status) => ['PENDING_PAYMENT', 'PLACED', 'ACCEPTED'].includes(status)
export const canStaffCancel = (status) => ['PLACED', 'ACCEPTED', 'PREPARING'].includes(status)
export const isOngoing = (status) => ['PENDING_PAYMENT', 'PLACED', 'ACCEPTED', 'PREPARING', 'READY'].includes(status)

/** State of each timeline step for an order: 'done', 'active' or 'todo'. */
export function timelineSteps(status) {
  const index = TIMELINE.indexOf(status)
  return TIMELINE.map((step, i) => ({
    step,
    label: STATUS_LABEL[step],
    state: status === 'COMPLETED' || i < index ? 'done' : i === index ? 'active' : 'todo',
  }))
}
