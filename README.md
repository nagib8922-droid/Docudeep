# DocuDeep – Architecture microservices

Cette version sépare DocuDeep en trois services indépendants :

1. **frontend-service** – application web (port 8080) permettant d'uploader et de consulter les justificatifs.
2. **upload-service** – API de création de dossiers et de stockage des documents (port 8081).
3. **view-service** – API de consultation et de téléchargement des documents validés (port 8082).

Tous les services partagent un stockage fichier local (`./storage/upload`) pour simplifier la recette en environnement de développement.

## Pré-requis

- Java 21.0.8
- Maven 3.9+ installé (`mvn`)

## Démarrage des services

Dans trois terminaux distincts :

```bash
# Terminal 1 – interface web
mvn -pl frontend-service spring-boot:run

# Terminal 2 – upload & stockage
mvn -pl upload-service spring-boot:run

# Terminal 3 – consultation
mvn -pl view-service spring-boot:run
```

Ensuite ouvrez [http://localhost:8080](http://localhost:8080) pour accéder à l'application web.

## Parcours de recette

1. Depuis l'écran web, ajoutez jusqu'à **5 fichiers** (PDF, PNG, JPG). Chaque fichier doit être associé à un type documentaire.
2. Au clic sur « Lancer l'envoi », le frontend appelle l'API `upload-service` :
   - création d'un dossier (`caseId`).
   - génération d'URL d'upload direct (`PUT /storage/cases/{caseId}/documents/{documentId}`).
   - validation côté serveur (taille ≤ 10 Mo, format supporté, fichier non vide).
3. Le panneau latéral liste les dossiers disponibles en interrogeant `view-service` (`GET /cases`). Chaque document peut être téléchargé via `GET /cases/{caseId}/documents/{documentId}`.
4. Pour repartir d'un stockage vierge, utilisez le bouton « Réinitialiser » qui appelle `POST /dev/storage/reset` sur `upload-service`.

## Configuration

Les trois services partagent la même propriété `storage.root` (par défaut `./storage/upload`). Vous pouvez la modifier via une variable d'environnement :

```bash
export STORAGE_ROOT=/chemin/vers/mon/repertoire
mvn -pl upload-service spring-boot:run
```

Appliquez la même configuration aux trois services pour conserver un stockage cohérent.

## Tests automatisés

Lancez toutes les suites :

```bash
mvn test
```

Les tests de chaque microservice se basent sur les contextes Spring Boot et ne nécessitent pas de dépendances externes.
