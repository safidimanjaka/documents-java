#!/bin/bash
set -e
mkdir -p keystore
# Generate a self-signed RSA keypair in a JKS keystore
keytool -genkeypair -alias jwtkey -keyalg RSA -keysize 2048 -storetype JKS -keystore keystore/keystore.jks -validity 3650 -storepass changeIt -keypass changeIt -dname "CN=local, OU=dev, O=example, L=City, ST=State, C=FR"
echo "Created keystore/keystore.jks with alias 'jwtkey' and password 'changeit'. Change passwords for production."
