# ADR 5: Pickup slots with atomic capacity limits

**Context.** Online ordering alone just moves the queue to the pickup counter if everyone orders for 1:00 PM. The real limit is how fast the kitchen can work.

**Decision.** Students choose a 15-minute slot, and each slot accepts a limited number of orders. Booking is a single statement:
```sql
UPDATE pickup_slots SET booked = booked + 1 WHERE slot_start = ? AND booked < capacity
```
One affected row means a place was taken; zero means the slot is full. A `CHECK (booked <= capacity)` constraint is the last line of defence.

**Alternatives rejected.**
- *Read, compare, write:* two requests can both read 9 of 10 and both book.
- *`SELECT FOR UPDATE`:* correct, but needs two statements for the same result.
- *Optimistic locking:* correct, but contention on a popular slot would cause many failed retries.

**Behaviour.** The booking shares a transaction with the order, so a rejected order never uses up a place. Cancellation and payment expiry give the place back. A test sends 50 concurrent checkouts to a 10-place slot and asserts exactly 10 succeed.

**Trade-offs.** Capacity counts orders, not items, so a large order counts the same as a small one. Counting prep minutes would be more accurate, and is a natural next step.
