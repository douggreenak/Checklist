# Checklist server

A small PHP API that sits next to your MariaDB/MySQL database. The Android app talks only to this
API (over HTTPS, signed in with a password). **The app never contains the database password**, and
the database password lives only in `config.php` on your web host.

## Setup (cPanel or any PHP 7.4+ host)

1. Copy `config.sample.php` to `config.php` and fill it in (database name/user/password, an app
   password hash, and a random token secret – the commands are in the file's comments).
2. Upload `api.php`, `config.php` and `.htaccess` to a folder on your site, for example
   `public_html/checklist/`.
3. Your site needs a valid HTTPS certificate on a domain name (Android won't accept a bare IP address
   with a self-signed certificate).
4. To also host the web version, upload the contents of `web/dist/` (after `npm run build` in `web/`) to the same folder, or run `web/deploy.sh`. Then open the address in a browser.
5. In the app, sign in with the server address (`yourdomain.com/checklist`) and your app password.

The tables are created automatically on first use.

## Changing the password

Use **Settings → Change password** in the app. The new password is stored (hashed) in the database and
overrides the hash in `config.php`, and every other signed-in device is signed out. If you ever forget
the password, delete the `password_hash` row from the `checklist_settings` table and the hash in
`config.php` applies again.

## How it works

- `POST api.php?action=login` – checks the password (5 wrong tries per IP locks out for 15 minutes)
  and returns a signed token valid for 90 days.
- `POST api.php?action=changepw` – needs a token and the current password; sets the new one.
- `GET api.php?action=get` / `POST api.php?action=save` – read and replace your lists. All of them are
  stored as one JSON document. If two devices change things at once, the most recent save wins.

## Security notes

- Never commit `config.php` (it is in `.gitignore`).
- If your database allows remote connections from anywhere (port 3306), restrict it to your web
  host in cPanel → Remote MySQL, since the app doesn't need direct access.
