# Open Cookie Static Website

This folder contains the official static website for the Open Cookie Android application.

## Structure
- `index.html`: Main landing page.
- `privacy.html`: Privacy Policy.
- `terms.html`: Terms of Service.
- `support.html`: Support information.
- `css/`: Stylesheets.
- `js/`: JavaScript files.
- `icon.svg` / `icon.png`: App icon (wallets fetch `icon.png` via MWA). Regenerate PNG: `python scripts/generate-icon.py` (requires `pillow`).

## How to open locally
Simply open `index.html` in any modern web browser. No server or compilation required.

## How to upload to Cloudflare Pages
1. Log in to Cloudflare.
2. Go to **Workers & Pages** -> **Create application** -> **Pages** -> **Connect to Git**.
3. Select your repository.
4. Set **Root Directory** to `android/website`.
5. Set **Build Settings**:
   - **Framework preset**: `None`
   - **Build command**: (empty)
   - **Output directory**: `.` (current directory relative to the root `android/website` folder)
6. Click **Save and Deploy**.

Domain configured in the app: `https://open-cookie.pages.dev`

## Digital Asset Links (MWA / wallet trust)

Wallets verify the Android app against this domain via `/.well-known/assetlinks.json`.
Without it, Phantom/Solflare show "unknown domain" on every transaction.

The file lives at `.well-known/assetlinks.json` and lists `com.opencookie.app` and
`com.opencookie.admin` with SHA-256 signing certificate fingerprints.

### Regenerate after keystore change

From the repo root (Windows):

```powershell
# Debug builds (default Android debug.keystore)
.\scripts\generate-assetlinks.ps1

# Release keystore (add fingerprint, keep existing debug entry)
.\scripts\generate-assetlinks.ps1 `
  -Keystore "C:\path\to\release.keystore" `
  -Alias "your-alias" `
  -StorePass "secret" `
  -Append
```

**Play Store / dApp Store:** use the **App signing certificate** SHA-256 from the store console, not only the upload key:

```powershell
.\scripts\generate-assetlinks.ps1 -Fingerprint "AA:BB:CC:..." -Append
```

After updating the file, redeploy Pages and confirm:

`https://open-cookie.pages.dev/.well-known/assetlinks.json`

Must return JSON with `Content-Type: application/json` and no redirects (`_headers` handles this on Cloudflare).
