# Interview guide

Written for Hejasvini. Live site: **https://smart-canteen-ruby.vercel.app**. For how the deployment works and where to look when something breaks, see [KT.md](KT.md).

**Before any interview: run the demo yourself, read the key files below, and make sure you can explain each decision in your own words.** Interviewers follow up, and "why" matters more than "what".

If you're asked how it was built, be straightforward about any help you had, including AI tools. Owning and defending the decisions is what impresses people.

## 2-minute pitch

> Campus canteens get long queues at peak times. Smart Canteen lets students order ahead, pick a pickup slot and collect with a QR code. It's a React and Spring Boot app on Postgres, with student, staff and admin roles.
>
> The interesting part is what happens under load and failure. Slots use an atomic update, so 50 people racing for 10 places gives exactly 10 orders, and there's a test proving it. QR codes are signed with HMAC and can only be collected once, even if scanned twice at the same moment. Payments are confirmed by signature, both from the browser and by a webhook, so an order still gets paid if the student closes the tab. And the kitchen board updates live over Server-Sent Events rather than polling.
>
> It's tested at three levels, including browser tests against the full stack, and it starts with one Docker command.

## 5-minute demo

`docker compose up --build`, then open http://localhost:5173 in two windows. Use a private window for the second.

| # | Do this | Say this |
|---|---|---|
| 1 | Window 1: log in as **Staff** using the demo button | "This board has a live connection to the server." |
| 2 | Window 2: log in as **Student**, add food, go to checkout | "The cart shows the price the server will charge, and it's re-checked at checkout." |
| 3 | Pick a slot, choose Pay online, continue | "Slots have real capacity. The order is created now, but the kitchen can't see it until it's paid." |
| 4 | Point at Window 1: the order is **not** there | "An unpaid order never reaches the kitchen." |
| 5 | Click **Simulate success** | "This sandbox stands in for Razorpay, with real signature checks. It refuses to start in production." |
| 6 | Window 1: the order **appears with no refresh** | "That was pushed over Server-Sent Events after the transaction committed." |
| 7 | Mark accepted, preparing, ready. Watch Window 2 | "The student sees each step live, plus a notification." |
| 8 | Window 1: Hand over, then Hand Over Food | "One atomic update, so a double scan can't hand it over twice." The camera scan works here too, if you have a second device. |
| 9 | Log in as **Admin** and show the dashboard and Payments | "Real analytics from real queries." |
| 10 | Show the tests: `SlotApiTest` and `PickupApiTest` | "Here's the 50-request test that proves there's no overbooking." |

## Demo on the live site

Use this when you can't run Docker, or when the interviewer wants a link: **https://smart-canteen-ruby.vercel.app**

**Before the call**
- Open the site **5 minutes early**. The free backend sleeps after 15 idle minutes, and the first request takes about a minute.
- Have the three logins ready from your password manager. The live site has **no demo buttons**, on purpose.
- Use two browser windows (one private), or a laptop and a phone.

| # | Do this | Say this |
|---|---|---|
| 1 | Window 1: log in as staff | "Same live board, now running on Render, with the data in Neon Postgres." |
| 2 | Window 2: log in as student, order, choose **Pay online** | "This is Razorpay's real checkout, in test mode." |
| 3 | Pay with UPI `success@razorpay` | "The browser sends back a signature, and the server checks it with HMAC before marking the order paid." |
| 4 | Window 1: the order appears without a refresh | "Server-Sent Events, published after the database commit." |
| 5 | Optional: place another paid order and cancel it as the student | "Cancelling before cooking starts refunds through Razorpay. You can see it in their dashboard." |
| 6 | Optional: scan the student's QR code with a phone as staff | "The camera needs HTTPS, which is one reason this had to be deployed rather than only run locally." |

If Razorpay or the Wi-Fi misbehaves, switch to the local Docker demo above. The sandbox needs no internet.

## Code tour

| Topic | File |
|---|---|
| Order rules and lifecycle | `backend/.../service/OrderService.java`, `entity/OrderStatus.java` |
| Atomic slot booking | `repository/PickupSlotRepository.java`, `slots/SlotService.java` |
| Atomic pickup | `repository/OrderRepository.java` (`markCollected`) |
| QR signing | `pickup/PickupCodeService.java` |
| Payments, webhook, idempotency | `service/PaymentService.java`, `payment/` |
| Live updates | `realtime/LiveUpdateService.java`, `frontend/src/services/liveStream.js` |
| Security | `config/SecurityConfig.java`, `security/` |
| Concurrency tests | `src/test/.../api/SlotApiTest.java`, `PickupApiTest.java`, `OrderApiTest.java` |
| Decisions and trade-offs | `docs/adr/` |

## Likely questions

**How do you prevent overbooking?**
A single SQL statement: `UPDATE ... SET booked = booked + 1 WHERE booked < capacity`. The database locks the row for that statement, so check and increment can't be separated. Reading first and then writing leaves a gap where two requests can both see a free place. There's also a CHECK constraint as a backstop.

**What's the difference between optimistic and pessimistic locking, and where do you use each?**
Optimistic assumes conflicts are rare and detects them at write time, using `@Version` on orders, so two staff can't silently overwrite each other. Pessimistic takes a lock up front, which I use on payment rows because the browser callback and webhook usually arrive together, and they should simply take turns.

**What if the double-click happens at checkout?**
The client sends an `Idempotency-Key`, which has a unique database index per user. A repeat returns the original order. If two arrive at once, the database rejects one, and I return the winner's order.

**A webhook arrives twice, or before the browser callback?**
Each event id is stored in `payment_events`, so duplicates are ignored. Webhook and callback both call one `settle()` function, which does nothing if already paid. Order doesn't matter.

**What if payment succeeds after the order expired?**
`settle()` sees the order is cancelled and refunds automatically.

**Why verify a signature at all?**
The browser is controlled by the user, so its claim that "payment succeeded" can't be trusted. Only the provider and server know the secret, so a valid HMAC proves the provider sent it. Comparison is constant-time, so timing reveals nothing.

**Why SSE and not WebSockets?**
Updates flow one way, and normal requests handle the other. SSE is plain HTTP, so it's simpler and works through proxies. If I needed two-way traffic, like chat, I'd choose WebSockets.

**Why send only an id in the event and refetch?**
One source of truth. A missed event can't leave wrong data on screen, and the client refetches on reconnect.

**Why publish after commit?**
Otherwise a client could refetch before the data is visible, or hear about a change that then rolls back.

**How would you scale this?**
Stateless JWTs mean more instances are easy. Then: Redis pub/sub for SSE across instances, Redis for rate limits (they're per instance today), a job lock for the expiry task, and read replicas for analytics. I'd measure before doing any of it.

**How is auth done?**
BCrypt-hashed passwords and a signed JWT. The filter loads the user on every request, so disabling someone or changing their role applies immediately. That costs one indexed query per request, which is a deliberate trade-off against pure stateless JWT.

**What was the hardest bug?**
Live updates that sometimes didn't reach staff. It passed 12 times in a row and then failed on a fresh database. The trace showed staff never refetched, while students did, and the logs showed why: calling `complete()` on a dead connection threw, which aborted the loop before reaching the live connections. The fix isolates each send, and a regression test covers it. A related one: nginx prefers a regex `location` over a plain prefix, so the stream was being buffered. Only a browser test through the real stack could catch it.

A second one: the camera test failed about 1 run in 25. The obvious suspect, a dense QR code, turned out to be wrong; shortening the code didn't help. Saving the failing frames showed ZXing couldn't decode them at full size but could after scaling them down, so the scanner now tries each frame at several sizes. Lesson: measure before and after, rather than trusting the first plausible fix.

**How does dark mode work?**
Every colour is a named token (`--surface`, `--ink`, `--accent`) in `styles.css`, defined once per theme; components never use raw colours. Switching sets one attribute on `<html>`, and Bootstrap's own components follow through `data-bs-theme`. A small inline script applies the saved or system theme before the first paint, so there's no white flash. Every text pair was measured for WCAG contrast in both themes, and the QR code deliberately stays black on white because scanners need it.

**How is it deployed?**
Three free managed services:
- **Vercel** serves the React build from a CDN.
- **Render** runs the Spring Boot API as a Docker container, in Singapore.
- **Neon** hosts Postgres, also in Singapore, so database calls stay close to the API.

A push to `main` runs CI on GitHub Actions and redeploys automatically. The API refuses to start in production if a secret is missing, or if someone points it at the payment sandbox. A bad config fails loudly at startup rather than at the first payment.

**Why not put everything on Vercel?**
Vercel runs short-lived serverless functions. This API holds long-lived SSE connections open to every staff board and runs a scheduled job every minute to expire unpaid orders. Both need a process that stays running, so the API goes in a container. Separating static hosting, the API and the database is a normal production layout anyway.

**How do database changes reach production?**
Flyway migrations are versioned SQL files in the repo. The API applies any new ones when it starts, inside a transaction, and never edits one that's already run. The same migrations run in tests on real Postgres through Testcontainers, so production isn't the first place a migration runs on Postgres.

**How did you handle secrets?**
- None are in the repo; `.env` is git-ignored, and `.env.example` lists names only.
- In production they live in Render's environment settings.
- The JWT and QR-signing secrets are random 48-byte values.
- The demo accounts' default passwords are public in the repo, so I replaced them with random ones before the site went live, and confirmed the old ones are rejected.
- Razorpay stays in test mode, so no real money can move.

**Why a direct database connection rather than Neon's pooler?**
The pooler (PgBouncer in transaction mode) suits serverless code that opens a connection per request. This is one long-running process with its own HikariCP pool of 5 connections. Flyway's migrations also need a normal session, which the transaction-mode pooler doesn't provide.

**What happens on a cold start?**
The free Render plan sleeps after 15 idle minutes, so the first request takes about a minute while the JVM starts. A paid instance, or a lighter runtime like GraalVM native image, would remove that. For a demo I just open it early.

**What did H2 hide?**
A `HOUR()` query that works on H2 but not Postgres. That's why tests now run on real Postgres.

## Known limitations (say these before you're asked)

- **JWT in `localStorage`** can be read by a successful XSS attack. An `httpOnly` cookie with CSRF protection would be stronger. React's escaping and having no `dangerouslySetInnerHTML` reduce the risk.
- **Changing a password doesn't revoke existing tokens**, which last up to 12 hours. Disabling a user does take effect immediately.
- **No password reset**, since there's no email service.
- **Single instance:** SSE connections and rate limits live in one instance's memory.
- **Slot capacity counts orders, not prep time.**
- **Refund calls run inside a transaction**, which holds a connection during the call (see ADR 2).
- **The wait estimate is simple:** orders in the kitchen × a configured number of minutes.
- **Free hosting:** the API sleeps when idle, so the first request after a quiet spell is slow.

## What I'd build next

Prep-time-based capacity, email or push notifications, an outbox for provider calls, stock counts per item, and load testing to find the real limits.

## Numbers you can quote

Measured on this code, and worth re-running before you quote them:
- 125 backend tests, on real Postgres (`./mvnw verify`), with 93% line coverage on the service layer
- 19 frontend unit tests, and 18 browser tests including a camera scan using a fake video feed and the light/dark theme switch
- 50 concurrent checkouts on a 10-place slot: exactly 10 succeed
- 10 concurrent scans of one order: exactly 1 succeeds
- Main JavaScript bundle reduced from 986 KB to 314 KB by lazy-loading routes

Don't quote user numbers or real-world time savings: this hasn't been used by a real canteen.
