# Handoff

All of this work is **uncommitted** in the working tree, and nothing has been pushed.

## What's still needed

1. **Review the changes**, then commit and push. The suggested groups are below.
2. **Create a Razorpay test account.** The real flow has not been run, since it needs keys. Its signature logic is unit tested, and the webhook payload format and headers were checked against Razorpay's docs, but the first real payment should be watched closely.
3. **Deploy** using [DEPLOYMENT.md](DEPLOYMENT.md), then add the link to the README.
4. **Try the camera on a real phone.** It has been tested with Chromium's fake camera feed, not physical hardware.
5. **Watch the first CI run.** The workflow has only been checked as valid YAML, though each command in it was run locally.
6. **Rehearse** with [INTERVIEW.md](INTERVIEW.md).

Some breaking changes matter if anything else uses this API: sign-up no longer takes a role, `/orders` needs a pickup slot, and `/orders` lists are paginated. `db/schema.sql` was replaced by Flyway migrations, and an existing database created from that old script would need a Flyway baseline. No deployed database was found.

## Suggested commits

Run `git status` first. The files mix several concerns, so these are approximate, and one large commit with a good message is also fine.

1. `build: add Maven wrapper and lockfile, real lint script`
2. `feat(db): replace H2 and schema.sql with Postgres and Flyway`
3. `fix(security): stop role self-assignment, add ownership checks and rate limiting`
4. `feat(orders): add payment-pending state, idempotency, daily tokens, soft delete`
5. `feat(payments): add gateway interface, Razorpay, sandbox and webhooks`
6. `feat(pickup): add signed QR codes, camera scanner and atomic collection`
7. `feat(slots): add capacity-limited pickup slots and real canteen status`
8. `feat(live): replace polling with Server-Sent Events`
9. `feat(ui): add reviews, notifications, admin pages and profile editing`
10. `fix(ui): remove fake data and placeholder content`
11. `test: add Testcontainers, unit and Playwright suites`
12. `ci: add Dockerfiles, Compose stack and GitHub Actions`
13. `docs: rewrite README, add ADRs and guides`
