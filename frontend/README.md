# Offline UI assets (fonts + Tailwind CSS)

The app serves fonts and Tailwind CSS from `src/main/resources/static/` — no Google CDN at runtime.

## Required files in the deployed JAR

These paths **must** be present in the build artifact or icons show as text (`dashb`, `logout`, etc.):

```
static/vendor/fonts/inter/inter-latin-*.woff2          (5 files)
static/vendor/fonts/material-symbols-outlined/material-symbols-outlined-latin-*-normal.woff2  (7 files)
static/css/hrms-vendor-fonts.css
static/css/hrms-tailwind.css
```

If icons break after deploy, verify the font URL in the browser network tab:

`/vendor/fonts/material-symbols-outlined/material-symbols-outlined-latin-400-normal.woff2` → should return **200**, not 404.

## Rebuild after UI changes

```bash
cd frontend
npm install
npm run build:all
```

Commit the updated CSS and font files with your change.
