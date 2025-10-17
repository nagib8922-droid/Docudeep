# DocuDeep – Microservices indépendants

Cette implémentation sépare DocuDeep en trois projets entièrement autonomes écrits avec la bibliothèque standard Python. Chaque microservice peut être démarré, testé et livré sans dépendances externes ni accès réseau.

1. **frontend-service** – sert l'interface web moderne permettant d'uploader et de consulter les justificatifs.
2. **upload-service** – API REST responsable de la création des dossiers, de la validation des fichiers et de leur stockage sécurisé sur disque.
3. **view-service** – API REST qui liste les dossiers existants et offre le téléchargement des documents stockés.

Les services partagent un répertoire `storage/` à la racine du dépôt. Vous pouvez modifier son emplacement en définissant la variable d'environnement `STORAGE_ROOT` (ou via l'option `--storage`).

## Lancer la plateforme

Ouvrez trois terminaux et exécutez les commandes suivantes :

```bash
# Terminal 1 – interface web (http://localhost:8000)
cd frontend-service
python app.py

# Terminal 2 – service d'upload (http://localhost:8001)
cd upload-service
python -m docudeep_upload.server --storage ../storage

# Terminal 3 – service de consultation (http://localhost:8002)
cd view-service
python -m docudeep_view.server --storage ../storage
```

Une fois les trois services démarrés, ouvrez [http://localhost:8000](http://localhost:8000) pour accéder à la webview. L'écran supporte :

- le téléversement de **1 à 5** documents (PDF, PNG, JPG/JPEG) de **10 Mo maximum** chacun ;
- la sélection du type documentaire (*bulletin de paie*, *avis d'imposition*, *charges*) pour chaque fichier ;
- l'affichage immédiat des dossiers enregistrés avec liens de téléchargement.

Le service d'upload rejette automatiquement les fichiers trop volumineux, au mauvais format ou non lisibles (vérification d'en-tête PDF/PNG/JPEG). Un endpoint `POST /cases/reset` permet de nettoyer le stockage lors des recettes.

## Tests automatisés

Chaque microservice dispose de tests unitaires exécutables sans dépendances :

```bash
cd upload-service
python -m unittest

cd ../view-service
python -m unittest

cd ../frontend-service
python -m unittest
```

Tous les tests doivent réussir avant de valider une modification (`git commit`).

## Nettoyage du stockage

Le répertoire partagé peut être supprimé en toute sécurité pour repartir de zéro :

```bash
rm -rf storage/cases
mkdir -p storage/cases
```

Veillez à utiliser le même dossier pour les services d'upload et de consultation afin de conserver la cohérence des dossiers.
