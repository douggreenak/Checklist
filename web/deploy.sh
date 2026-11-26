#!/usr/bin/env bash
# Uploads the built web app to your server over SFTP.
# Usage: SFTP_TARGET=user@host SFTP_KEY=~/.ssh/key SFTP_DIR=public_html/checklist ./deploy.sh
set -euo pipefail
: "${SFTP_TARGET:?set SFTP_TARGET, e.g. user@host}" "${SFTP_DIR:=public_html/checklist}"
cd "$(dirname "$0")" && node build.mjs
{
  for f in index.html app.js style.css icon.svg; do echo "put dist/$f $SFTP_DIR/$f"; done
  echo "-mkdir $SFTP_DIR/fonts"
  echo "put dist/fonts/RobotoFlex-latin.woff2 $SFTP_DIR/fonts/RobotoFlex-latin.woff2"
} | sftp ${SFTP_KEY:+-i "$SFTP_KEY" -o IdentitiesOnly=yes} -b - "$SFTP_TARGET"
