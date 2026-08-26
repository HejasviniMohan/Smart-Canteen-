# Smart Canteen — Local Development with Real PostgreSQL

## One-time setup

1. Install Docker Desktop (https://www.docker.com/products/docker-desktop/)
2. Make sure Docker is running (whale icon in system tray)

## Start database

```powershell
cd "D:\Canteen management"
docker compose up -d postgres
```

The schema + seed data load automatically on first run.

## Start backend (against real DB)

```powershell
cd "D:\Canteen management\backend"
mvn -DskipTests spring-boot:run -Dspring-boot.run.profiles=prod
```

(or `java -jar target\smart-canteen-backend-1.0.0.jar --spring.profiles.active=prod`)

## Start frontend

```powershell
cd "D:\Canteen management\frontend"
npm install
npm run dev
```

Open http://localhost:5173

## Tear down

```powershell
docker compose down -v   # remove container + data volume
```

## Connection details

- Host: `localhost:5432`
- DB: `smart_canteen`
- User: `postgres`
- Password: `Canteen@2026`