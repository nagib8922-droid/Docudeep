# DocuDeep Frontend Service

Ce microservice fournit l'interface web moderne de DocuDeep. Il se contente de servir les fichiers statiques depuis le dossier `static/` à l'aide de la bibliothèque standard Python.

## Lancer le service

```bash
cd frontend-service
python app.py --host 0.0.0.0 --port 8000
```

L'application est ensuite accessible sur `http://localhost:8000`. Les appels API sont envoyés vers `http://localhost:8001` (service d'upload) et `http://localhost:8002` (service de consultation).

## Tests

```bash
cd frontend-service
python -m unittest
```
