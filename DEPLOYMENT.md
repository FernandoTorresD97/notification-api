# Deploy Guide — Notification API

This guide walks through deploying the full stack (PostgreSQL + Spring Boot API + static frontend) using free-tier services, so the project can be shown live in your portfolio, CV or LinkedIn — not just run locally.

Suggested combo: **Render** (backend + PostgreSQL) + **Netlify** (frontend). Both have generous free tiers and deploy straight from a GitHub repository — no credit card required for this setup.

---

## 0. Push the project to GitHub first

```bash
cd notification-api
git init
git add .
git commit -m "Notification API - backend, frontend, docker, CI"
git branch -M main
git remote add origin https://github.com/<your-username>/notification-api.git
git push -u origin main
```

---

## 1. Deploy PostgreSQL on Render

1. Go to [render.com](https://render.com) and sign up / log in (GitHub login is fastest).
2. **New +** → **PostgreSQL**.
3. Give it a name (e.g. `notification-db`), pick the **Free** plan, choose a region close to you.
4. Once created, open the database page and copy:
   - **Internal Database URL** (used by the API when both run on Render)
   - **Hostname**, **Port**, **Database**, **Username**, **Password** (in case you need them individually)

## 2. Deploy the Spring Boot API on Render

1. **New +** → **Web Service** → connect your GitHub repo (`notification-api`).
2. Configure:
   - **Runtime**: Docker (Render will detect the `Dockerfile` at the repo root automatically)
   - **Region**: same as the database, to keep latency low
   - **Instance type**: Free
3. Add environment variables (**Environment** tab) — use the individual values from your Render Postgres instance:
   ```
   DB_HOST=<hostname from step 1>
   DB_PORT=5432
   DB_NAME=<database name>
   DB_USER=<username>
   DB_PASSWORD=<password>
   ```
4. Deploy. Render will build the Docker image and start the container. First boot takes a few minutes; watch the **Logs** tab — the same startup sequence you saw locally (Hibernate creating tables, `DataLoader` seeding channels, Tomcat starting) should appear here too.
5. Once live, Render gives you a public URL like:
   ```
   https://notification-api-xxxx.onrender.com
   ```
   Test it: `https://notification-api-xxxx.onrender.com/api/v1/channels` should return the seeded channels. Swagger UI is at `/swagger-ui.html` on that same URL.

> **Free tier note:** Render's free web services spin down after periods of inactivity and take ~30–60s to wake up on the next request. That's expected — mention it if you demo this live, or upgrade to a paid instance later if it matters for your use case.

## 3. Point the frontend at the deployed API

Open `frontend/app.js` and you technically don't need to change anything — the API base URL is configurable directly in the UI (the input field at the top of the page). But for convenience, you can set the default:

```js
const state = {
  apiBaseUrl: localStorage.getItem("notif_api_base_url") || "https://notification-api-xxxx.onrender.com",
  ...
};
```

## 4. Deploy the frontend on Netlify

1. Go to [netlify.com](https://netlify.com), sign up / log in with GitHub.
2. **Add new site** → **Import an existing project** → pick your `notification-api` repo.
3. Configure:
   - **Base directory**: `frontend`
   - **Build command**: *(leave empty — it's plain HTML/CSS/JS, no build step)*
   - **Publish directory**: `frontend`
4. Deploy. Netlify gives you a public URL like:
   ```
   https://notification-api-frontend.netlify.app
   ```
5. Open it, confirm the API base URL field points to your Render URL, and test creating a channel/notification from the live UI.

*(Vercel or GitHub Pages work the same way — any static host is fine, since the frontend has no build step.)*

## 5. Update your CORS origin (optional, tighter security)

Right now `WebConfig.java` allows all origins (`allowedOriginPatterns("*")`) to keep local development friction-free. For a more correct production setup, once you know your Netlify URL, tighten it:

```java
registry.addMapping("/api/**")
        .allowedOrigins("https://notification-api-frontend.netlify.app")
        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        .allowedHeaders("*");
```

Commit, push, Render redeploys automatically.

---

## Alternative: everything in one place with Docker Compose on a VPS

If you'd rather run everything yourself on a small VPS (DigitalOcean, Hetzner, an old Raspberry Pi, whatever):

```bash
git clone https://github.com/<your-username>/notification-api.git
cd notification-api
docker compose up --build -d
```

This starts PostgreSQL, the API (port 8080) and the frontend (port 3000) together, using the `docker-compose.yml` already in the repo. Point a domain/reverse proxy (Caddy, Nginx, Traefik) at ports 3000/8080 if you want it public with HTTPS.

---

## What to put in your README / LinkedIn once deployed

```
🔗 Live demo: https://notification-api-frontend.netlify.app
🔗 API docs (Swagger): https://notification-api-xxxx.onrender.com/swagger-ui.html
🔗 Source: https://github.com/<your-username>/notification-api
```

A working, publicly reachable link is worth far more to a recruiter than a screenshot — it's proof the thing actually runs.
