#!/usr/bin/env bash
#
# make-keystore.sh — one-time setup for signing Preston's Player.
#
# Run this ONCE. It creates a signing key (prestons-player.jks) and prints the
# 4 secrets you paste into GitHub. Back up the .jks file forever — lose it and
# you can never ship an update that installs over someone's existing copy.
#
set -euo pipefail

JKS="prestons-player.jks"
ALIAS="prestons"

command -v keytool >/dev/null 2>&1 || {
  echo "❌ 'keytool' not found. Install a JDK first:"
  echo "     brew install --cask temurin"
  echo "   then re-run ./make-keystore.sh"
  exit 1
}

if [ -f "$JKS" ]; then
  echo "⚠️  $JKS already exists in this folder."
  echo "    Regenerating it means everyone with the OLD app must uninstall before they can update."
  read -r -p "    Overwrite it? [y/N] " ans
  case "$ans" in
    y|Y) rm -f "$JKS" ;;
    *)   echo "Aborted — keeping your existing keystore."; exit 1 ;;
  esac
fi

echo "Choose a password for your signing key."
echo "WRITE IT DOWN somewhere safe — it cannot be recovered."
while :; do
  read -r -s -p "Password: " PW;  echo
  read -r -s -p "Confirm : " PW2; echo
  [ -n "$PW" ]      || { echo "Password can't be empty."; continue; }
  [ "$PW" = "$PW2" ] && break
  echo "Didn't match — try again."
done

keytool -genkeypair -v \
  -keystore "$JKS" \
  -alias "$ALIAS" \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass "$PW" -keypass "$PW" \
  -dname "CN=Prestons Player, OU=Home, O=Prestons Player, L=City, ST=State, C=US" \
  >/dev/null 2>&1

B64=$(base64 < "$JKS" | tr -d '\n')

cat <<EOF

✅ Created $JKS  —  BACK THIS FILE UP NOW (and remember the password).

════════════════════════════════════════════════════════════════════════
 Add these as 4 separate secrets in your GitHub repo:
   Settings → Secrets and variables → Actions → New repository secret
════════════════════════════════════════════════════════════════════════

  Name:  KEYSTORE_PASSWORD
  Value: $PW

  Name:  KEY_ALIAS
  Value: $ALIAS

  Name:  KEY_PASSWORD
  Value: $PW

  Name:  KEYSTORE_BASE64
  Value: (the whole line below — no spaces, no line breaks)

$B64

════════════════════════════════════════════════════════════════════════
Leave this terminal open until all 4 secrets are saved in GitHub.
EOF
