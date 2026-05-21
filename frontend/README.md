# CampusMarket Frontend

`frontend/` contains the Vue 3 application scaffold for `CampusMarket`.

## Current scaffold coverage

- User-facing app layout
- Admin layout
- Login page shell
- Home page shell
- Reusable list and form page shells
- Router guards
- API client wrapper
- Pinia store scaffold

## Local commands

```bash
npm ci
npm run dev
```

## Build verification

```bash
npm run build
```

## Directory conventions

- `src/router/`: routes and guards
- `src/layouts/`: app and admin layouts
- `src/views/app/`: user-facing pages
- `src/views/admin/`: admin pages
- `src/views/auth/`: authentication pages
- `src/components/`: reusable layout and shell components
- `src/stores/`: Pinia stores
- `src/api/`: API modules and client wrapper
- `src/utils/`: auth and storage helpers
- `src/styles/`: global styles and design variables
