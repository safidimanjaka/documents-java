# === PHASE 1 : Build Maven ===
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

# Copier tout le projet dans le conteneur
COPY . .

# Compilation + création du vrai jar Spring Boot
RUN mvn clean package -DskipTests

# === PHASE 2 : Run ===
FROM eclipse-temurin:21-jre-alpine AS run
WORKDIR /app

# Copier le vrai jar Spring Boot
COPY --from=builder /app/target/confidential-docs-0.0.1-SNAPSHOT.jar app.jar

# Copier les scripts pour gérer le keystore
COPY generate-keystore.sh .
COPY entrypoint.sh .

# Rendre les scripts exécutables
RUN chmod +x generate-keystore.sh entrypoint.sh

EXPOSE 8080

# Entrypoint personnalisé
ENTRYPOINT ["./entrypoint.sh"]
