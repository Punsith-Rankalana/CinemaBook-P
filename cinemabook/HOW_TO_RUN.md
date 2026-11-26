# How to run CinemaBook in IntelliJ IDEA

## What you need installed first

| Tool | Version | Download |
|------|---------|----------|
| IntelliJ IDEA | 2023.1+ (Community or Ultimate) | https://www.jetbrains.com/idea/ |
| Java JDK | 21 | https://adoptium.net (pick Temurin 21) |
| Docker Desktop | Latest | https://www.docker.com/products/docker-desktop/ |
| Node.js | 20+ | https://nodejs.org |

---

## Step 1 — Extract and open the project

1. Extract the zip anywhere on your computer (e.g. `C:\Projects\CinemaBook\`)
2. Open IntelliJ IDEA
3. Click **Open** → navigate to the `cinemabook/backend/` folder → click **OK**
4. IntelliJ will detect the `pom.xml` and ask *"Trust Maven project?"* → click **Trust Project**
5. Wait for Maven to download all dependencies (first time takes 2–5 minutes)

---

## Step 2 — Set Java 21 SDK

1. Go to **File → Project Structure** (Ctrl+Alt+Shift+S)
2. Under **Project**, set **SDK** to Java 21
3. Set **Language level** to 21
4. Click **OK**

---

## Step 3 — Enable Lombok annotation processing

IntelliJ needs this or you'll get hundreds of red errors.

1. Go to **File → Settings** (Ctrl+Alt+S)
2. Navigate to **Build, Execution, Deployment → Compiler → Annotation Processors**
3. Check ✅ **Enable annotation processing**
4. Click **OK**

> The `.idea/compiler.xml` file in the project already configures this — if IntelliJ loaded it, you may skip this step.

---

## Step 4 — Start Docker (PostgreSQL + Redis)

Open a terminal in the `cinemabook/` root folder and run:

```
docker compose up -d
```

This starts:
- **PostgreSQL** on port `5432` (database: `cinemabook_db`, user: `cinemabook`, password: `cinemabook_secret`)
- **Redis** on port `6379`

Verify they are running:
```
docker compose ps
```

Both should show **Up**.

---

## Step 5 — Run the Spring Boot backend

The run configuration is already included in the project.

1. In IntelliJ, look at the top-right toolbar — you should see **CinemaBookApplication** in the dropdown
2. Click the green ▶ **Run** button

IntelliJ will:
- Compile the project
- Run Flyway migrations (creates all database tables automatically)
- Start the server on **http://localhost:8080**

You should see in the console:
```
Started CinemaBookApplication in X.XXX seconds
```

> **If the run configuration is not visible:** Go to **Run → Edit Configurations → + → Spring Boot** and set:
> - Main class: `com.cinemabook.CinemaBookApplication`
> - Module: `cinemabook-backend`
> - Environment variables: copy from the `.env` file in the `backend/` folder

---

## Step 6 — Run the React frontend

Open a **separate terminal** in the `cinemabook/frontend/` folder:

```
npm install
npm run dev
```

The frontend starts on **http://localhost:3000**

---

## Step 7 — Open in browser

Go to: **http://localhost:3000**

You should see the CinemaBook login page.

Try registering:
- Click **Create a watcher account** → fill in email, username, password → submit
- Click **Sign in** → log in with your new account
- You should land on the Watcher Dashboard

---

## Verify the API is working

Open a browser or use Postman:

```
GET http://localhost:8080/api/v1/actuator/health
```

Should return:
```json
{ "status": "UP" }
```

---

## Common errors and fixes

### Red errors all over the code after opening
→ Go to **File → Settings → Build → Compiler → Annotation Processors** → enable it → rebuild (Ctrl+F9)

### "Cannot resolve symbol" on Lombok annotations (@Getter, @Builder etc.)
→ Install the Lombok plugin: **File → Settings → Plugins** → search "Lombok" → Install → Restart IntelliJ

### Port 5432 already in use
→ Something else is using PostgreSQL. Either stop it, or change the port in `docker-compose.yml` and `application.properties`.

### "Connection refused" to database on startup
→ Docker is not running. Open Docker Desktop and wait for it to fully start, then re-run `docker compose up -d`.

### Flyway error on startup
→ The database wasn't created. Run this in Docker terminal:
```
docker exec -it cinemabook_postgres psql -U cinemabook -c "CREATE DATABASE cinemabook_db;"
```
Then restart the Spring Boot app.

### Frontend shows blank page
→ Make sure the backend is running first. Check browser console for errors.

---

## Project structure at a glance

```
cinemabook/
├── docker-compose.yml        ← start this first
├── backend/                  ← open THIS folder in IntelliJ
│   ├── pom.xml
│   ├── mvnw / mvnw.cmd       ← Maven wrapper (no Maven install needed)
│   ├── .env                  ← dev environment variables
│   ├── .idea/
│   │   └── runConfigurations/
│   │       └── CinemaBookApplication.xml  ← auto-loads in IntelliJ
│   └── src/
└── frontend/                 ← run with npm in a separate terminal
    ├── package.json
    └── src/
```

---

## Ports used

| Service | Port |
|---------|------|
| Spring Boot API | 8080 |
| React frontend | 3000 |
| PostgreSQL | 5432 |
| Redis | 6379 |
