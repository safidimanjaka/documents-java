# Système de Gestion de Documents Confidentiels et Archivage

Ce README décrit l'application et documente l'ensemble des routes (API REST) disponibles, y compris les nouvelles routes de gestion des départements (création, modification du nom, suppression). Tout est en Français.

Résumé
- Spring Boot + Spring Security (JWT signé par une clé RSA dans un Java KeyStore).
- Chiffrement des fichiers au repos : AES/GCM par document, clé AES wrapée par RSA depuis le keystore.
- Postgres en production, H2 en mémoire pour les tests.
- Rôles hiérarchiques : USER, EMPLOYEE, DEPT_HEAD, DIRECTOR.
- HTTPS (server.ssl.*) configurable ; le keystore auto-généré fournit un certificat autosigné pour développement.
- Tests : unitaires (Mockito) + intégration (MockMvc). Postman collection fournie.

Pré-requis
- Java 17+
- Maven
- PostgreSQL (ou utiliser H2 en local pour tests)

Build & Run
1. Configurer `src/main/resources/application.properties` (datasource, keystore path/password si besoin).
2. mvn clean package
3. java -jar target/confidential-docs-0.0.1-SNAPSHOT.jar

Authentification (JWT)
- POST /api/auth/login
  - Body JSON: { "username": "admin", "password": "adminpass" }
  - Retour: { "token": "..." }
- Inclure le header `Authorization: Bearer <token>` pour les routes protégées.

Rôles et règles d'accès (récapitulatif)
- USER : peut lire ses propres documents (lecture seule).
- EMPLOYEE : lire + modifier (metadata) ses propres documents.
- DEPT_HEAD : lire + modifier documents de son département.
- DIRECTOR : lire, modifier, supprimer tous les documents ; gérer utilisateurs et départements.

Routes disponibles

1) Auth
- POST /api/auth/login
  - Permet d'obtenir un JWT.

2) Utilisateurs (User management)
- GET /api/users
  - Liste tous les utilisateurs.
  - Auth requis (token).
- POST /api/users
  - Créer un utilisateur.
  - Roles : DIRECTOR uniquement.
  - Body : User JSON (username, password, role, department: { id })
- PUT /api/users/{id}
  - Modifier utilisateur.
  - Roles : DIRECTOR, ou DEPT_HEAD pour les utilisateurs du même département.
  - Body : fields à modifier (username, password, department{id}, role — role modifiable seulement par DIRECTOR)
- DELETE /api/users/{id}
  - Supprimer un utilisateur.
  - Roles : DIRECTOR uniquement.

3) Documents
- POST /api/documents/upload
  - Upload d'un fichier (multipart form-data `file`).
  - Chiffrement AES/GCM ; clé AES wrapée et stockée.
  - Retour : metadata Document (id, filename, owner, department, createdAt, ...)
- GET /api/documents
  - Lister les documents accessibles selon rôle/département/propriétaire.
- GET /api/documents/{id}/download
  - Télécharger et déchiffrer le document.
  - Accès vérifié selon politique (voir rôles).
- PUT /api/documents/{id}
  - Modifier metadata (par ex. filename).
  - Roles autorisés : DIRECTOR, DEPT_HEAD (pour docs de son département), EMPLOYEE (pour ses propres docs).
- DELETE /api/documents/{id}
  - Supprimer document (fichier + métadonnées).
  - Roles : DIRECTOR uniquement.

4) Départements (NOUVEAU)
- POST /api/departments
  - Créer un département.
  - Roles : DIRECTOR uniquement.
  - Body (JSON) : { "name": "NomDuDepartement" }
  - Retour : Department créé.
- PUT /api/departments/{id}
  - Renommer / modifier le département.
  - Roles : DIRECTOR, ou DEPT_HEAD s'il est chef du département ciblé.
  - Body (JSON) : { "name": "NouveauNom" }
  - Retour : Department mis à jour.
- DELETE /api/departments/{id}
  - Supprimer le département.
  - Roles : DIRECTOR uniquement.
  - Condition : suppression seulement si aucun utilisateur ni document n'est rattaché au département.
    - Si des users/documents existent -> 400 Bad Request avec cause (department_has_users / department_has_documents).
  - Retour : 204 No Content si suppression réussie.

Entités principales (schéma simplifié)
- User (id, username, password (bcrypt), role, department_id)
- Department (id, name)
- Document (id, filename, file_path, wrapped_key (bytes), owner_id, department_id, created_at)

Sécurité, CSRF, HTTPS
- API REST stateless basée sur JWT : CSRF désactivé pour les endpoints API.
- SSL : `app.keystore.path` est utilisé comme keystore pour TLS ; pour dev un keystore .jks est généré si absent.
- Les clés RSA du keystore servent à signer les JWT et à protéger (wrap) les clés AES.

Gestion des clés et chiffrement
- Keystore JKS (app.keystore.path) contient l'alias configuré (app.keystore.alias).
- Chaque document :
  - clé AES générée (par document),
  - document chiffré avec AES/GCM, IV préfixé au ciphertext,
  - clé AES wrapée avec RSA/OAEP et stockée (wrapped_key).
- CryptoService propose les méthodes wrap/unwrap, encrypt/decrypt.

Tests
- Tests d'intégration : src/test/java/... (MockMvc). Utilisent H2 en mémoire (fichier de test `src/test/resources/application-test.properties`).
- Tests unitaires : Mockito pour isoler KeyStoreService / CryptoService.
- Avant d'exécuter les tests, le projet ajoute BouncyCastle provider pour les opérations PKIX (fichiers test bootstrapping fournis).
- Commande : mvn -q clean test

Postman
- Collection et environnement fournis : importez `postman_collection.json` et `postman_environment.json`.
- Mettez `baseUrl` et `filePath` (pour upload) et utilisez les requêtes Login pour obtenir et réutiliser le JWT dans les requêtes suivantes.

Exemples (curl)
- Login :
```bash
curl -s -X POST {{baseUrl}}/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"adminpass"}'
```

- Créer un département (DIRECTOR) :
```bash
curl -X POST {{baseUrl}}/api/departments \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"name":"R&D"}'
```

- Renommer un département (DIRECTOR ou DEPT_HEAD du département) :
```bash
curl -X PUT {{baseUrl}}/api/departments/3 \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"name":"NewName"}'
```

- Supprimer un département (DIRECTOR, seulement si vide) :
```bash
curl -X DELETE {{baseUrl}}/api/departments/3 \
  -H "Authorization: Bearer <token>"
```

Bonnes pratiques et remarques
- En production : utilisez un keystore protégé (pas de mot de passe par défaut), stockez le keystore dans un secret manager ou un HSM/KMS si nécessaire.
- Utilisez une autorité de certification pour TLS (pas d'autosigné).
- Validez et limitez la taille des uploads, ajoutez quotas et scanning antivirus selon vos règles.
- Pour la conservation / migration de départements, implémentez réaffectation des utilisateurs/documents avant suppression si vous souhaitez supprimer des départements contenant des entités.