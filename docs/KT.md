# Knowledge transfer: running the live deployment

This covers how the live site is put together, where to look when something breaks, and how to change it safely. No secret values are written here. They're kept in Render's environment settings and in your password manager.

## What runs where

| Part | Service | Location | Address |
|---|---|---|---|
| Website (React) | Vercel, project `smart-canteen` (team `hejasvini`) | Global CDN | https://smart-canteen-ruby.vercel.app |
| API (Spring Boot, Docker) | Render web service `smart-canteen-backend` (`srv-dbo5p1tg1s2s73e9ej2g`), free plan | Singapore | https://smart-canteen-backend-jqcm.onrender.com |
| Database (Postgres 18) | Neon project `Smart-canteen-system` (`fragrant-dawn-37223080`), branch `production`, database `neondb` | Singapore (`aws-ap-southeast-1`) | Only the API connects to it |
| Payments | Razorpay, **Test Mode** | | Dummy money only |
| Code and CI | GitHub `HejasviniMohan/Smart-Canteen-`, GitHub Actions | | |

Every account above belongs to `hejasvinimohan@gmail.com`.

```
Browser ──► Vercel (static site) ──HTTPS + JWT──► Render (Spring Boot API) ──JDBC/TLS──► Neon Postgres
                                                     ▲
                              Razorpay webhook ──────┘  (payment.captured / payment.failed / order.paid)
```

The browser calls the API directly, so the API only accepts browser requests from the Vercel address. That's the `CORS_ALLOWED_ORIGINS` setting.

## Dashboards

| What | Where |
|---|---|
| API deploys, logs, settings | https://dashboard.render.com/web/srv-dbo5p1tg1s2s73e9ej2g (tabs: Events, Logs, Environment) |
| Website deploys | https://vercel.com/hejasvini/smart-canteen |
| Database | https://console.neon.tech, then project `Smart-canteen-system` (Monitoring, Tables, SQL Editor, Backup & Restore) |
| Payments, refunds, webhooks | https://dashboard.razorpay.com in **Test Mode**: Transactions, then Webhooks |
| CI runs | https://github.com/HejasviniMohan/Smart-Canteen-/actions |
| API health | https://smart-canteen-backend-jqcm.onrender.com/actuator/health should say `UP` |

## How a change goes live

1. Commit and push to `main`.
2. **GitHub Actions** runs lint, unit tests, backend tests on real Postgres, and browser tests. A red CI run doesn't stop deploys, so check it.
3. **Render** rebuilds the API from `backend/Dockerfile` and switches over when it's healthy. This takes about 5 to 8 minutes, and the old version keeps serving until then.
4. **Vercel** rebuilds the website from `frontend/`. Automatic deploys need Vercel's GitHub app to have access to the repo (see [Still to do](#still-to-do)). Until then, deploy by hand from the repo root:
   ```bash
   vercel deploy --prod
   ```

**Database changes** use Flyway. The API applies any new `backend/src/main/resources/db/migration/V<n>__name.sql` when it starts.
- Never edit a migration that has already run. Add a new one instead.
- Test the migration locally first with `docker compose up --build`.
- At startup the log shows a warning: `Flyway upgrade recommended: PostgreSQL 18.6 is newer than this version of Flyway`. It's harmless.

## Checking a deployment

```bash
# Website
vercel ls smart-canteen                   # recent deploys and their state
vercel inspect <deployment-url> --logs    # build log for one deploy
vercel env ls                             # variable names (values stay hidden)

# CI
gh run list -L 5
gh run view <run-id> --log-failed

# API
curl https://smart-canteen-backend-jqcm.onrender.com/actuator/health
```

For API logs, open Render → **Logs** and filter by text, for example `ERROR`, `Razorpay`, `Flyway`.

## Environment variables

### Render (API)

| Name | Purpose | Secret |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod`. Turns on the startup safety checks | no |
| `DATABASE_URL` | `jdbc:postgresql://<neon-host>/neondb?sslmode=require`, using the **direct** (non-pooler) host | no |
| `DB_USERNAME` | `neondb_owner` | no |
| `DB_PASSWORD` | Neon password | **yes** |
| `JWT_SECRET` | Signs login tokens. Changing it logs everyone out | **yes** |
| `PICKUP_SECRET` | Signs QR pickup codes. Changing it invalidates any QR codes already issued | **yes** |
| `PAYMENT_GATEWAY` | `razorpay`. The sandbox refuses to start under `prod` | no |
| `RAZORPAY_KEY_ID` | `rzp_test_…`. Public, the browser sees it | no |
| `RAZORPAY_KEY_SECRET` | Razorpay API secret | **yes** |
| `RAZORPAY_WEBHOOK_SECRET` | Must match the secret on the Razorpay webhook | **yes** |
| `CORS_ALLOWED_ORIGINS` | `https://smart-canteen-ruby.vercel.app`. Comma-separate to add more, no trailing slash | no |
| `SEED_DEMO_DATA` | `false`. Leave it off (see [Security](#security)) | no |

Saving a change in Render's Environment tab redeploys the API automatically.

If a required secret is missing, the API **refuses to start** and says which one in the log. That's intended.

### Vercel (website)

| Name | Value |
|---|---|
| `VITE_API_BASE_URL` | `https://smart-canteen-backend-jqcm.onrender.com` (Production and Preview) |
| `VITE_DEMO_MODE` | Not set, so there are no demo buttons on the login page |

`VITE_*` values are baked in at build time, so redeploy after changing them.

## Logins

The live site has three accounts: `admin@canteen.com`, `staff@canteen.com` and `student@canteen.com`. Their passwords are random and stored only in the password manager. The defaults shown in the README (`Admin@123` and so on) **don't work** on the live site.

- Change a password: log in, then Profile → Change password.
- Add staff or disable a user: log in as admin, then Users.
- Students can sign up themselves. Sign-up can only ever create a student account.

## Common tasks

| Task | How |
|---|---|
| **Wake the site before a demo** | Open the site about 5 minutes early. The free API sleeps after 15 idle minutes, and the first request then takes about 1 minute |
| **Roll back the website** | Vercel → Deployments → pick an earlier one → **Instant Rollback** (or `vercel rollback`) |
| **Roll back the API** | Render → Events → an earlier deploy → **Rollback** |
| **Undo database damage** | Neon → Backup & Restore → restore `production` to a time before the problem. Restore into a new branch first if unsure |
| **Rotate a secret** | Generate one (`openssl rand -hex 48`), paste it in Render → Environment, and save. For the webhook secret, update Razorpay to the same value too |
| **Run a SQL query** | Neon → SQL Editor. Read-only queries are safe; be careful with updates |
| **See payments or refunds** | Razorpay dashboard (Test Mode) → Transactions |
| **Add a custom domain** | Vercel → Domains. Then add the new address to `CORS_ALLOWED_ORIGINS` on Render |

## Troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| First load is slow, or login spins for about a minute | The API was asleep | Wait, then retry. Open it early next time |
| Login shows a network error and the browser console mentions CORS | The site's address isn't in `CORS_ALLOWED_ORIGINS` | Add the exact address, with no trailing slash |
| API deploy fails, or the service keeps restarting | A missing variable, or the database is unreachable | Render → Logs. The startup error names the problem |
| `FATAL: password authentication failed` | `DB_PASSWORD` is wrong, or the Neon password was reset | Copy the current password from Neon → Connect |
| Connection error mentioning `channel_binding` | `&channel_binding=require` was copied into `DATABASE_URL` | Remove it. The Java driver doesn't recognise it |
| Payment popup fails to open | Wrong `RAZORPAY_KEY_ID`, or not a test key | Check the value in Render |
| Payment succeeded in Razorpay, but the order shows unpaid after closing the tab | The webhook isn't set up or its secret differs | Razorpay → Webhooks → check deliveries and the secret |
| Website shows an old version | The deploy didn't run (GitHub app missing) | Run `vercel deploy --prod` |
| QR camera doesn't start | The camera needs HTTPS and permission | Use the live HTTPS site and allow the camera |

## Free plan limits

- **Render free:** sleeps when idle; 512 MB memory; 750 instance hours a month, which is enough for one service.
- **Neon free:** the database pauses after a few idle minutes and wakes in well under a second; storage and compute hours are capped. Check usage in the Neon console.
- **Vercel Hobby:** for non-commercial use, which is fine for a portfolio demo.

## Security

- **No secrets in the repo.** `.env`, `.env.local` and `.vercel` are git-ignored. `.env.example` lists names only.
- **Demo accounts:** they were created once, and their published passwords were replaced before the site went public. `SEED_DEMO_DATA` stays `false`, so the published defaults can never come back. Never deploy with `VITE_DEMO_MODE=true` unless the database is a throwaway one.
- **Razorpay stays in Test Mode.** Don't switch this deployment to live keys: refunds would move real money.
- **Neon API key:** a key named `neon-cli-mcp-…` was created for local tooling and can reach the whole Neon account. If it isn't needed, revoke it with `neon api-keys revoke <id>`; the id is shown by `neon api-keys list`.
- **Tokens:** they last 12 hours. Rotating `JWT_SECRET` logs everyone out immediately.

## Local development

```bash
docker compose up --build             # whole app on http://localhost:5173, sandbox payments, demo logins
cd backend && ./mvnw verify           # backend tests (needs Docker for Testcontainers)
cd frontend && npm run lint && npm test && npm run build
cd frontend && npx playwright test    # browser tests, with the compose stack running
```

Local runs use their own Postgres container and never touch Neon.

## Still to do

- [ ] **Let Vercel see the repo:** go to https://github.com/apps/vercel/installations/new, choose `HejasviniMohan`, and give it access to `Smart-Canteen-`. Then run `vercel git connect https://github.com/HejasviniMohan/Smart-Canteen-` in the repo. After that, every push redeploys the website.
- [ ] **Razorpay webhook**, if it isn't added yet:
  - **URL:** `https://smart-canteen-backend-jqcm.onrender.com/api/payments/webhook`
  - **Events:** `payment.captured`, `payment.failed`, `order.paid`
  - **Secret:** the same value as `RAZORPAY_WEBHOOK_SECRET`
- [ ] **One live test payment** with UPI `success@razorpay`, then cancel a paid order and confirm the refund appears in Razorpay.
- [ ] **Camera pickup on two real phones:** one shows the student's QR code, the other scans it as staff.
