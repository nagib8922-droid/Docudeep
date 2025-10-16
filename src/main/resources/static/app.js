const MAX_FILES = 5;
const ACCEPTED_TYPES = ['application/pdf', 'image/png', 'image/jpeg'];
const TYPE_LABELS = {
  BULLETIN_DE_PAIE: 'Bulletin de paie',
  AVIS_D_IMPOSITION: "Avis d'imposition",
  CHARGES: 'Charges'
};

const dropzone = document.getElementById('dropzone');
const fileInput = document.getElementById('file-input');
const fileList = document.getElementById('file-list');
const emptyState = document.getElementById('files-empty');
const submitButton = document.getElementById('submit');
const statusLog = document.getElementById('status');
const resetButton = document.getElementById('reset-storage');
const fileRowTemplate = document.getElementById('file-row-template');

const state = [];

function formatBytes(bytes) {
  if (!bytes) return '0 o';
  const units = ['o', 'Ko', 'Mo'];
  let size = bytes;
  let unitIndex = 0;
  while (size >= 1024 && unitIndex < units.length - 1) {
    size /= 1024;
    unitIndex++;
  }
  return `${size.toFixed(unitIndex === 0 ? 0 : 1)} ${units[unitIndex]}`;
}

function guessMimeType(file) {
  if (file.type) return file.type;
  const extension = file.name.split('.').pop()?.toLowerCase();
  switch (extension) {
    case 'pdf':
      return 'application/pdf';
    case 'png':
      return 'image/png';
    case 'jpg':
    case 'jpeg':
      return 'image/jpeg';
    default:
      return '';
  }
}

function logStatus(message, variant = 'info') {
  const entry = document.createElement('p');
  entry.textContent = message;
  entry.classList.add(variant);
  statusLog.appendChild(entry);
  statusLog.scrollTop = statusLog.scrollHeight;
}

function updateEmptyState() {
  if (state.length === 0) {
    emptyState.hidden = false;
  } else {
    emptyState.hidden = true;
  }
}

function updateSubmitState() {
  const ready = state.length > 0 && state.every(file => Boolean(file.documentType));
  submitButton.disabled = !ready;
}

function setFileStatus(file, message, progress) {
  const { elements } = file;
  if (!elements) return;
  if (typeof progress === 'number') {
    elements.progressBar.style.width = `${progress}%`;
    elements.progressBar.parentElement.setAttribute('aria-valuenow', Math.round(progress));
  }
  if (message) {
    elements.statusText.textContent = message;
  }
}

function markFileError(file, message) {
  const { elements } = file;
  if (!elements) return;
  elements.row.classList.add('error');
  elements.statusText.textContent = message;
  elements.progressBar.style.background = 'var(--danger)';
}

function renderFile(file) {
  const fragment = fileRowTemplate.content.cloneNode(true);
  const row = fragment.querySelector('.file-row');
  const nameEl = fragment.querySelector('.file-name');
  const sizeEl = fragment.querySelector('.file-size');
  const selectEl = fragment.querySelector('.file-type');
  const removeBtn = fragment.querySelector('.remove');
  const progressBar = fragment.querySelector('.progress-bar');
  const statusText = fragment.querySelector('.status-text');

  nameEl.textContent = file.file.name;
  nameEl.title = file.file.name;
  sizeEl.textContent = formatBytes(file.file.size);

  if (file.documentType) {
    selectEl.value = file.documentType;
  }

  selectEl.addEventListener('change', event => {
    file.documentType = event.target.value;
    row.classList.remove('error');
    updateSubmitState();
  });

  removeBtn.addEventListener('click', () => {
    const index = state.indexOf(file);
    if (index >= 0) {
      state.splice(index, 1);
      row.remove();
      updateEmptyState();
      updateSubmitState();
    }
  });

  file.elements = {
    row,
    progressBar,
    statusText
  };

  fileList.appendChild(fragment);
}

function addFiles(files) {
  const incoming = Array.from(files);
  for (const file of incoming) {
    if (state.length >= MAX_FILES) {
      logStatus(`Impossible d'ajouter « ${file.name} » : maximum ${MAX_FILES} fichiers.`, 'error');
      break;
    }

    const mime = guessMimeType(file);
    if (!ACCEPTED_TYPES.includes(mime)) {
      logStatus(`Le fichier « ${file.name} » n'est pas dans un format supporté.`, 'error');
      continue;
    }

    if (file.size > 10 * 1024 * 1024) {
      logStatus(`Le fichier « ${file.name} » dépasse la taille maximale de 10 Mo.`, 'error');
      continue;
    }

    const descriptor = {
      file,
      mime,
      documentType: '',
      elements: null
    };
    state.push(descriptor);
    renderFile(descriptor);
    setFileStatus(descriptor, 'En attente', 0);
  }
  updateEmptyState();
  updateSubmitState();
}

function openFilePicker() {
  fileInput.value = '';
  fileInput.click();
}

function handleDrop(event) {
  event.preventDefault();
  dropzone.classList.remove('dragover');
  if (event.dataTransfer?.files?.length) {
    addFiles(event.dataTransfer.files);
  }
}

dropzone.addEventListener('click', openFilePicker);

dropzone.addEventListener('keydown', event => {
  if (event.key === 'Enter' || event.key === ' ') {
    event.preventDefault();
    openFilePicker();
  }
});

dropzone.addEventListener('dragover', event => {
  event.preventDefault();
  dropzone.classList.add('dragover');
});

dropzone.addEventListener('dragleave', () => {
  dropzone.classList.remove('dragover');
});

dropzone.addEventListener('drop', handleDrop);

fileInput.addEventListener('change', event => {
  if (event.target.files?.length) {
    addFiles(event.target.files);
  }
});

async function submitCase() {
  const missingType = state.filter(file => !file.documentType);
  if (missingType.length > 0) {
    missingType.forEach(file => file.elements?.row.classList.add('error'));
    logStatus('Veuillez sélectionner un type pour chaque document.', 'error');
    updateSubmitState();
    return;
  }

  submitButton.disabled = true;
  submitButton.textContent = 'Préparation…';

  try {
    logStatus('Création du dossier…');
    const payload = {
      documents: state.map(file => ({
        filename: file.file.name,
        mimeType: file.mime,
        sizeBytes: file.file.size,
        documentType: file.documentType
      }))
    };

    const caseResponse = await fetch('/api/cases', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    if (!caseResponse.ok) {
      const error = await safeParseError(caseResponse);
      throw new Error(error ?? 'La création du dossier a échoué.');
    }

    const caseData = await caseResponse.json();
    logStatus(`Dossier ${caseData.caseId} créé.`, 'success');

    for (let index = 0; index < caseData.uploads.length; index++) {
      const plan = caseData.uploads[index];
      const file = state[index];
      await uploadDocument(caseData.caseId, plan, file);
    }

    logStatus('Tous les documents ont été transmis et validés ✅', 'success');
  } catch (error) {
    console.error(error);
    logStatus(error.message ?? 'Erreur inattendue lors de l\'envoi.', 'error');
  } finally {
    submitButton.textContent = "Lancer l'envoi";
    updateSubmitState();
  }
}

async function uploadDocument(caseId, plan, file) {
  try {
    setFileStatus(file, 'Téléversement du fichier…', 25);

    const headers = new Headers();
    if (plan.headers) {
      Object.entries(plan.headers).forEach(([key, value]) => {
        if (value) {
          headers.set(key, value);
        }
      });
    }

    const uploadResponse = await fetch(plan.uploadUrl, {
      method: plan.method ?? 'PUT',
      headers,
      body: file.file
    });

    if (!uploadResponse.ok) {
      const errorMessage = await safeParseError(uploadResponse);
      throw new Error(errorMessage ?? 'Le téléversement a échoué.');
    }

    setFileStatus(file, 'Validation du document…', 70);

    const completeResponse = await fetch(`/api/cases/${caseId}/documents/${plan.documentId}/complete`, {
      method: 'POST'
    });

    if (!completeResponse.ok) {
      const errorMessage = await safeParseError(completeResponse);
      throw new Error(errorMessage ?? 'La validation du document a échoué.');
    }

    const document = await completeResponse.json();
    setFileStatus(file, `Validé (${TYPE_LABELS[document.documentType] ?? document.documentType})`, 100);
  } catch (error) {
    markFileError(file, error.message ?? 'Erreur inattendue');
    logStatus(`Erreur sur « ${file.file.name} » : ${error.message}`, 'error');
    throw error;
  }
}

async function safeParseError(response) {
  try {
    const payload = await response.json();
    if (payload?.message) {
      return payload.message;
    }
    return typeof payload === 'string' ? payload : response.statusText;
  } catch (_) {
    return response.statusText;
  }
}

submitButton.addEventListener('click', submitCase);

if (resetButton) {
  resetButton.addEventListener('click', async () => {
    try {
      const resp = await fetch('/api/dev/storage/reset', { method: 'POST' });
      if (resp.ok) {
        state.splice(0, state.length);
        fileList.innerHTML = '';
        updateEmptyState();
        updateSubmitState();
        statusLog.innerHTML = '';
        logStatus('Stockage local réinitialisé.', 'success');
      } else {
        throw new Error('Réinitialisation indisponible.');
      }
    } catch (error) {
      logStatus(error.message ?? 'Impossible de réinitialiser le stockage.', 'error');
    }
  });
}

updateEmptyState();
logStatus('Sélectionnez vos justificatifs pour démarrer.', 'info');
