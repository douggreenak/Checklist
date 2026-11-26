# Checklist web app

A Material 3 Expressive web version of Checklist. It uses the same API, password and data as the
Android app, so lists sync between your phone and the browser.

- Built with plain JavaScript (no framework). Colors come from Google's
  [material-color-utilities](https://github.com/material-foundation/material-color-utilities)
  (Vibrant scheme in light mode, Tonal Spot in dark mode), and the font is Roboto Flex, served from
  your own server (no calls to Google).
- Security: the page has a strict Content-Security-Policy, all text is inserted as text (never HTML),
  and it talks only to its own `api.php`.

## Build, test, deploy

```
npm install
npm run build        # writes dist/
npm test -- http://127.0.0.1:8000/   # end-to-end tests (run against a TEST server, not your real data)
SFTP_TARGET=user@host SFTP_KEY=~/.ssh/key SFTP_DIR=public_html/checklist ./deploy.sh
```

The tests drive headless Chromium (set `CHROME=/path/to/chromium` if it isn't at
`/usr/bin/chromium-browser`) and **create and delete data**, so point them at a scratch server.
`tests/shots.mjs` saves screenshots.
