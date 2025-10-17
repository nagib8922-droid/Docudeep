# DocuDeep View Service

Ce microservice expose les dossiers et documents stockés. Il parcourt le répertoire partagé produit par le service d'upload et retourne les métadonnées ou les fichiers binaires associés.

## Démarrage

```bash
cd view-service
python -m docudeep_view.server --host 0.0.0.0 --port 8002 --storage ../storage
```

## Endpoints

- `GET /cases` : liste des dossiers triés par date de création décroissante.
- `GET /cases/{case_id}` : détails d'un dossier.
- `GET /cases/{case_id}/documents/{document_id}` : téléchargement d'un document.

## Tests

```bash
cd view-service
python -m unittest
```
