# DocuDeep – Téléversement de justificatifs

Cette version du projet propose à la fois l'API de dépôt des documents et une interface web moderne permettant de les charger depuis un navigateur.

## Pré-requis

* Java 21
* Docker (optionnel si vous souhaitez lancer la base PostgreSQL fournie dans `docker-compose.yml`)

## Lancer l'application

1. (Optionnel) Démarrez l'infrastructure :
   ```bash
   docker compose up -d
   ```
2. Démarrez l'application Spring Boot :
   ```bash
   ./gradlew bootRun
   ```
3. Ouvrez votre navigateur sur [http://localhost:8080](http://localhost:8080) et utilisez l'interface de dépôt.

L'environnement de développement est configuré pour utiliser un stockage local (`storage/`) au lieu d'AWS S3. Vous pouvez réinitialiser ce stockage via le bouton « Réinitialiser » dans l'interface.

### Basculer sur un stockage S3

Pour utiliser un bucket S3 réel, définissez la propriété suivante (via `application.properties` ou une variable d'environnement) :

```properties
docudeep.storage.mode=s3
```

Assurez-vous alors de renseigner le bucket, la région et les identifiants AWS accessibles par l'application.

## Recette et parcours utilisateur

1. Sélectionnez jusqu'à 5 fichiers PDF/PNG/JPG via glisser-déposer ou le sélecteur de fichiers.
2. Pour chaque document, choisissez le type (bulletin de paie, avis d'imposition, charges).
3. Cliquez sur « Lancer l'envoi ». L'application va :
   - créer un dossier (`case_id`),
   - téléverser chaque fichier via l'URL présignée locale,
   - lancer la validation (lisibilité, absence de mot de passe, etc.).
4. Le panneau latéral affiche la progression et les éventuelles erreurs.

Un dossier peut être relancé après correction en cliquant sur « Réinitialiser » pour nettoyer le stockage local.

## Tests automatisés

Lancer la suite de tests :

```bash
./gradlew test
```

⚠️ Si certaines dépendances externes (AWS SDK, PDFBox) ne peuvent pas être téléchargées dans votre environnement réseau, Gradle remontera une erreur. Dans ce cas, vérifiez votre connexion ou mettez en place un proxy Maven.
