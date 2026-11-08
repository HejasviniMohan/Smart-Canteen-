# ADR 2: Payment gateway interface, with a sandbox that can't reach production

**Context.** Checkout sent a hardcoded `demo-signature`, and the server accepted it in every environment. The demo still has to work without a payment account.

**Decision.** Business logic depends on a `PaymentGateway` interface with two implementations, selected by `app.payment.gateway`:
- `RazorpayGateway`: real test-mode orders, HMAC verification and refunds, using hosted checkout so card data never touches this app.
- `SandboxGateway`: simulates the provider but still signs and verifies with real HMACs. It throws at startup under the `prod` profile.

Two paths can confirm a payment: the browser callback and a signed webhook, which covers a student closing the tab. Both reach one idempotent `settle()`. A row lock makes them take turns, and a `payment_events` table ignores redelivered webhooks.

**Trade-offs.** The refund and order-creation calls happen inside a database transaction. This is simple, and a failed refund rolls the cancellation back, but a slow provider holds a connection (bounded by a 15-second timeout). At higher volume, an outbox table and a background worker would be better.

Money arriving for an order that has already been cancelled is refunded automatically.
