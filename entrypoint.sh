#!/bin/sh
set -e

KEYSTORE_DIR="/app/keystore"
KEYSTORE_PATH="$KEYSTORE_DIR/keystore.jks"

ALIAS="${APP_KEYSTORE_ALIAS:-jwtkey}"
STOREPASS="${APP_KEYSTORE_PASSWORD:-Genius@Keypass1}"
KEYPASS="${APP_KEYSTORE_KEY_PASSWORD:-Genius@Keypass1}"

echo "======================================================"
echo "  EntryPoint - regenerating keystore"
echo "======================================================"

# Assurer que le dossier existe et a les bons droits
mkdir -p "$KEYSTORE_DIR"
chmod 755 "$KEYSTORE_DIR"

# Supprimer l’ancien keystore s’il existe
if [ -f "$KEYSTORE_PATH" ]; then
    echo "🔥 Removing existing keystore at $KEYSTORE_PATH"
    rm -f "$KEYSTORE_PATH"
fi

# Générer un keystore JKS propre
echo "🔐 Generating new keystore at $KEYSTORE_PATH..."

keytool -genkeypair \
    -alias "$ALIAS" \
    -keyalg RSA \
    -keysize 2048 \
    -storetype JKS \
    -keystore "$KEYSTORE_PATH" \
    -storepass "$STOREPASS" \
    -keypass "$KEYPASS" \
    -validity 3650 \
    -dname "CN=ConfDocs, OU=Security, O=Company, L=City, S=State, C=MG" \
    -noprompt

echo "🔐 Keystore generated successfully!"
echo "======================================================"

echo "🚀 Starting Spring Boot..."
exec java -jar /app/app.jar
