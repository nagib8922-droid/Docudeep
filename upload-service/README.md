# DocuDeep Upload Service

Ce service gère la création de dossiers et la validation des documents téléversés. Il repose uniquement sur la bibliothèque standard de Python afin d'être exécutable sans dépendances externes.

## Démarrage

```bash
cd upload-service
python -m docudeep_upload.server --host 0.0.0.0 --port 8001 --storage ../storage
```

Les fichiers sont enregistrés dans le dossier `../storage/cases` (modifiable via la variable `STORAGE_ROOT`).

## API

- `POST /cases` : crée un dossier à partir d'une liste de documents encodés en Base64.
- `POST /cases/reset` : supprime les dossiers pour faciliter les recettes locales.

## Tests

```bash
cd upload-service
python -m unittest
```
