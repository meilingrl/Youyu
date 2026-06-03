# Scripts

Useful local scripts:

- `start-backend-local.ps1`
  - Load the current worktree root `.env` and run the backend on `127.0.0.1:8080`.
- `start-frontend-local.ps1`
  - Load the current worktree root `.env` and run the frontend dev server on `127.0.0.1:5173` by default.
- `open-local-dev.cmd`
  - Double-click on Windows to open two terminals automatically: one backend, one frontend.
- `start-cloudflared-tunnel.ps1`
  - Start a `cloudflared` tunnel to the local backend and refresh `.env` with the new callback URL.
- `stop-cloudflared-tunnel.ps1`
  - Stop the last `cloudflared` tunnel started by the script.
- `start-local-dev.ps1`
  - Print the exact backend/frontend startup commands for the current worktree.
- `generate-perf-catalog-sql.mjs`
  - Generate `backend/src/main/resources/seed/data-perf-catalog.sql`.
  - Example: `node scripts/generate-perf-catalog-sql.mjs 4000`
