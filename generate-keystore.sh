#!/bin/sh
set -e

KEYSTORE_PATH="$1"

KEYSTORE_DIR=$(dirname "$KEYSTORE_PATH")
mkdir -p "$KEYSTORE_DIR"

echo "[INFO] Génération d’un nouveau keystore au chemin : $KEYSTORE_PATH"

keytool -genkeypair \
    -alias jwtkey \
    -keyalg RSA \
    -keysize 2048 \
    -validity 3650 \
    -storetype JKS \
    -keystore "$KEYSTORE_PATH" \
    -storepass "${APP_KEYSTORE_PASSWORD:-Genius@Keypass1}" \
    -keypass "${APP_KEY_PASSWORD:-Genius@Keypass1}" \
    -dname "CN=confidential-docs, OU=Dev, O=App, L=City, ST=State, C=FR"

echo "[INFO] Nouveau keystore généré."
