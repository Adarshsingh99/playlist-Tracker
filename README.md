# Playlist Tracker

> Track your YouTube playlist learning progress — no login required.

A full-stack multi-user web application where each browser gets an isolated anonymous identity backed by a secure HttpOnly cookie. Every user sees only their own playlists and progress.

---

## Project Structure

```
playlist-tracker/
├── backend/          # Spring Boot (Java 17) — REST API + MongoDB
├── frontend/         # React + Vite + Tailwind CSS
├── .gitignore
└── README.md
```

---

## Tech Stack

| Layer    | Technology                            |
|----------|---------------------------------------|
| Frontend | React 18, Vite, Tailwind CSS, Axios   |
| Backend  | Java 17, Spring Boot 3.3, Maven       |
| Database | MongoDB (local or Atlas)              |
| API      | YouTube Data API v3                   |

---

## Features

1. **Import YouTube Playlist** — paste any public playlist URL
2. **Video Checklist** — check off each video as you watch it
3. **Progress Tracking** — live progress bar + stats (total / completed / remaining)
4. **Persistent Progress** — survives browser restarts via MongoDB
5. **Multi-user Isolation** — each browser is a separate anonymous user

---

## Multi-User Architecture

```
Browser
  │
  ▼
HttpOnly Secure Cookie (auid=<uuid>)
  │
  ▼
Spring Boot AnonymousUserFilter
  │  reads cookie → sets request attribute
  ▼
PlaylistController
  │  reads from request attribute ONLY (never from request body)
  ▼
PlaylistService
  │  all queries scoped to anonymousUserId
  ▼
MongoDB
  │  compound unique index: (anonymousUserId + playlistId)
  ▼
Isolated per-user data
```

**Security guarantee:** The `anonymousUserId` is never accepted from the request body or query parameters. It is read exclusively from the server-set HttpOnly cookie.

---

## Prerequisites

- Java 17+
- Maven 3.8+
- Node.js 18+
- MongoDB (local) or MongoDB Atlas account
- YouTube Data API v3 key

---

## Local Setup

### 1. Clone

```bash
git clone https://github.com/your-username/playlist-tracker.git
cd playlist-tracker
```

### 2. Get a YouTube API Key

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a project → Enable **YouTube Data API v3**
3. Create credentials → **API Key**
4. Copy the key

### 3. Start MongoDB

```bash
# Local MongoDB
mongod --dbpath /data/db

# Or use MongoDB Atlas (free tier):
# https://www.mongodb.com/atlas/database
```

### 4. Configure the Backend

```bash
cd backend
cp .env.example .env
```

Edit `.env`:
```env
YOUTUBE_API_KEY=your_actual_api_key
MONGODB_URI=mongodb://localhost:27017/playlisttracker
COOKIE_SECURE=false
COOKIE_SAME_SITE=Lax
CORS_ALLOWED_ORIGINS=http://localhost:5173
```

### 5. Run the Backend

```bash
# Windows
cd backend
set YOUTUBE_API_KEY=your_key_here
set MONGODB_URI=mongodb://localhost:27017/playlisttracker
mvn spring-boot:run

# Linux/Mac
cd backend
export YOUTUBE_API_KEY=your_key_here
export MONGODB_URI=mongodb://localhost:27017/playlisttracker
mvn spring-boot:run
```

Backend starts at: `http://localhost:8080`

### 6. Run the Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend starts at: `http://localhost:5173`

The Vite dev server proxies `/api/*` → `http://localhost:8080`, so cookies work seamlessly.

---

---

## API Reference

### V1 — YouTube Playlist Import APIs

#### Import Playlist
```
POST /api/playlists/import
Content-Type: application/json

{ "playlistUrl": "https://www.youtube.com/playlist?list=PLxxxx" }
```

#### Get User's Imported Playlists
```
GET /api/playlists
```

#### Get Single Imported Playlist
```
GET /api/playlists/{id}
```
Returns `404` if the playlist belongs to another user — no data leakage.

#### Update Video Progress (Imported Playlist)
```
PATCH /api/playlists/{playlistId}/videos/{videoId}
Content-Type: application/json

{ "completed": true }
```

---

### V2 — Custom Learning Playlist APIs

#### Create Custom Playlist
```
POST /api/custom-playlists
Content-Type: application/json

{ "name": "Web Development" }
```

#### Get User's Custom Playlists
```
GET /api/custom-playlists
```

#### Get Single Custom Playlist
```
GET /api/custom-playlists/{id}
```
Returns `404` if the custom playlist belongs to another user.

#### Add Content (Single Video OR Entire Playlist)
```
POST /api/custom-playlists/{id}/content
Content-Type: application/json

{ "url": "https://www.youtube.com/watch?v=XXXXX" }
```
OR
```
POST /api/custom-playlists/{id}/content
Content-Type: application/json

{ "url": "https://www.youtube.com/playlist?list=PLXXXXX" }
```
- Automatically identifies URL type.
- Prevents duplicates (single video: returns 400 with `"Video already exists in this playlist"` if duplicate).
- For entire playlist: skips duplicates, adds only new videos, returns counts: `{ totalFound, addedCount, alreadyExistedCount, message }`.

#### Update Video Progress (Custom Playlist)
```
PATCH /api/custom-playlists/{id}/videos/{videoId}
Content-Type: application/json

{ "completed": true }
```

#### Delete Video from Custom Playlist
```
DELETE /api/custom-playlists/{id}/videos/{videoId}
```

#### Delete Custom Playlist
```
DELETE /api/custom-playlists/{id}
```

---

### Feedback API

#### Submit Feedback
```
POST /api/feedback
Content-Type: application/json

{
  "name": "Adarsh",
  "feedback": "The playlist tracker is very useful."
}
```

---

## Environment Variables

### Backend

| Variable                  | Required | Default                                      | Description                              |
|---------------------------|----------|----------------------------------------------|------------------------------------------|
| `YOUTUBE_API_KEY`         | ✅        | —                                            | YouTube Data API v3 key                  |
| `MONGODB_URI`             | ✅        | `mongodb://localhost:27017/playlisttracker`  | MongoDB connection string                |
| `PORT`                    | ❌        | `8080`                                       | Server port                              |
| `COOKIE_SECURE`           | ❌        | `false`                                      | Set `true` in production (HTTPS)         |
| `COOKIE_SAME_SITE`        | ❌        | `Lax`                                        | `Lax` or `None` (for cross-site)         |
| `COOKIE_DOMAIN`           | ❌        | _(empty)_                                    | Cookie domain for production             |
| `CORS_ALLOWED_ORIGINS`    | ❌        | `http://localhost:5173`                      | Comma-separated allowed origins          |
| `MAIL_HOST`               | ❌        | `smtp.gmail.com`                             | SMTP mail server host                    |
| `MAIL_PORT`               | ❌        | `587`                                        | SMTP mail server port                    |
| `MAIL_USERNAME`           | ❌        | _(empty)_                                    | Sender email / SMTP user                 |
| `MAIL_PASSWORD`           | ❌        | _(empty)_                                    | SMTP App password                        |
| `FEEDBACK_RECEIVER_EMAIL` | ❌        | _(empty)_                                    | Email to receive user feedback messages  |

### Frontend

| Variable            | Required | Default | Description                              |
|---------------------|----------|---------|------------------------------------------|
| `VITE_API_BASE_URL` | ❌        | _(empty)_ | Backend URL (leave empty for dev proxy) |

---

## Deployment

### Backend → Render / Railway

1. Push to GitHub
2. Create a new Web Service on Render/Railway
3. Set environment variables:
   ```
   YOUTUBE_API_KEY=...
   MONGODB_URI=mongodb+srv://user:pass@cluster.mongodb.net/playlisttracker
   COOKIE_SECURE=true
   COOKIE_SAME_SITE=None
   CORS_ALLOWED_ORIGINS=https://your-frontend.vercel.app
   ```
4. Build command: `mvn clean package -DskipTests`
5. Start command: `java -jar target/playlisttracker-0.0.1-SNAPSHOT.jar`

### Frontend → Vercel / Netlify

1. Push to GitHub
2. Connect repo to Vercel/Netlify
3. Set environment variable:
   ```
   VITE_API_BASE_URL=https://your-backend.onrender.com
   ```
4. Build command: `npm run build`
5. Output directory: `dist`

### MongoDB → Atlas

1. Create free cluster at [mongodb.com/atlas](https://www.mongodb.com/atlas)
2. Create database user + whitelist your IP (or allow all: `0.0.0.0/0`)
3. Copy connection string → set as `MONGODB_URI`

---

## Multi-User Isolation Tests

Open two different browsers (e.g. Chrome + Firefox, or Chrome normal + incognito).

### Test 1 — Data isolation
- Browser A: Import a Java DSA playlist, complete 10 videos
- Browser B: Open the app → should show **empty** "My Playlists"

### Test 2 — Independent tracking
- Browser B: Import a React playlist, complete 5 videos
- Browser A: Should still show only the Java playlist

### Test 3 — Same playlist, different progress
- Both browsers import the same playlist
- Browser A marks 10 videos complete → Browser B still shows 0

### Test 4 — Manual ID tampering
- Copy Browser A's playlist `id` from the URL
- Open it in Browser B's tab
- Expected: `404 Not Found` (no data leakage)

---

## Known Limitations

1. **Cookie-bound identity**: Your progress is tied to this browser's cookies.
   - Clearing cookies = new anonymous identity = previous progress inaccessible.
   - There is no account recovery mechanism in V1.

2. **No cross-device sync**: Logging into the app on a second device starts fresh.

3. **YouTube API quota**: The free quota is 10,000 units/day. Each import costs ~2–5 units per page. Large playlists (200+ videos) may cost more.

4. **Private/deleted videos**: These are automatically skipped during import. If a video becomes private after import, it remains in the list but cannot be watched.

5. **Playlist updates**: If new videos are added to the YouTube playlist after import, they won't appear automatically. Re-import the playlist to get updates (this returns the existing record — re-import is a future V2 feature).

---

## License

MIT
