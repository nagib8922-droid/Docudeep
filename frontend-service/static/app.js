const UPLOAD_API = window.UPLOAD_API || "http://localhost:8001";
const VIEW_API = window.VIEW_API || "http://localhost:8002";
const MAX_FILES = 5;
const MAX_SIZE = 10 * 1024 * 1024;
const ALLOWED_TYPES = [
  { value: "bulletin_de_paie", label: "Bulletin de paie" },
  { value: "avis_d_imposition", label: "Avis d'imposition" },
  { value: "charges", label: "Charges" },
];

const form = document.getElementById("upload-form");
const fileInput = document.getElementById("file-input");
const dropzone = document.getElementById("dropzone");
const fileList = document.getElementById("file-list");
const messageBox = document.getElementById("messages");
const refreshButton = document.getElementById("refresh-cases");
const caseList = document.getElementById("case-list");

const state = {
  files: [],
};

function setMessage(message, isError = false) {
  messageBox.textContent = message;
  messageBox.classList.toggle("error", isError);
}

function sanitizeFileName(name) {
  return name.replace(/[^a-zA-Z0-9_.\-]/g, "_");
}

function renderFileList() {
  fileList.innerHTML = "";
  state.files.forEach((entry, index) => {
    const wrapper = document.createElement("div");
    wrapper.className = "file-entry";

    const name = document.createElement("span");
    name.textContent = `${sanitizeFileName(entry.file.name)} (${(entry.file.size / 1024).toFixed(1)} Ko)`;

    const select = document.createElement("select");
    ALLOWED_TYPES.forEach((option) => {
      const opt = document.createElement("option");
      opt.value = option.value;
      opt.textContent = option.label;
      if (option.value === entry.type) {
        opt.selected = true;
      }
      select.appendChild(opt);
    });
    select.addEventListener("change", () => {
      entry.type = select.value;
    });

    wrapper.appendChild(name);
    wrapper.appendChild(select);
    fileList.appendChild(wrapper);
  });
}

function addFiles(files) {
  const incoming = Array.from(files || []);
  const combined = [...state.files];

  for (const file of incoming) {
    if (combined.length >= MAX_FILES) {
      setMessage("Maximum de cinq fichiers atteint.", true);
      break;
    }
    if (file.size > MAX_SIZE) {
      setMessage(`"${file.name}" dépasse la taille maximale de 10 Mo.`, true);
      continue;
    }
    const lower = file.name.toLowerCase();
    if (!(lower.endsWith(".pdf") || lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg"))) {
      setMessage(`Format non supporté pour ${file.name}.`, true);
      continue;
    }
    combined.push({ file, type: ALLOWED_TYPES[0].value });
  }

  state.files = combined;
  renderFileList();
}

async function toBase64(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => {
      const result = reader.result;
      const base64 = typeof result === "string" ? result.split(",")[1] || result : "";
      resolve(base64);
    };
    reader.onerror = () => reject(reader.error || new Error("Erreur de lecture"));
    reader.readAsDataURL(file);
  });
}

async function handleSubmit(event) {
  event.preventDefault();
  if (state.files.length === 0) {
    setMessage("Ajoutez au moins un document.", true);
    return;
  }

  setMessage("Téléversement en cours…");

  try {
    const documents = await Promise.all(
      state.files.map(async (entry) => ({
        name: entry.file.name,
        type: entry.type,
        content: await toBase64(entry.file),
      }))
    );

    const response = await fetch(`${UPLOAD_API}/cases`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ documents }),
    });

    if (!response.ok) {
      const payload = await response.json().catch(() => ({ message: "Erreur serveur." }));
      throw new Error(payload.message || "Téléversement impossible.");
    }

    const payload = await response.json();
    setMessage(`Dossier ${payload.case_id} créé avec succès.`);
    state.files = [];
    renderFileList();
    await loadCases();
  } catch (error) {
    console.error(error);
    setMessage(error.message || "Téléversement impossible.", true);
  }
}

async function loadCases() {
  try {
    const response = await fetch(`${VIEW_API}/cases`);
    if (!response.ok) {
      throw new Error("Impossible de récupérer les dossiers.");
    }
    const data = await response.json();
    renderCases(data.cases || []);
  } catch (error) {
    console.error(error);
    setMessage(error.message || "Impossible de récupérer les dossiers.", true);
  }
}

function renderCases(cases) {
  caseList.innerHTML = "";
  if (!cases.length) {
    const empty = document.createElement("li");
    empty.textContent = "Aucun dossier disponible pour le moment.";
    caseList.appendChild(empty);
    return;
  }

  cases.forEach((entry) => {
    const item = document.createElement("li");
    item.className = "case-card";
    const title = document.createElement("h3");
    title.textContent = `Dossier ${entry.case_id}`;
    item.appendChild(title);

    const documents = document.createElement("ul");
    entry.documents.forEach((doc) => {
      const docItem = document.createElement("li");
      const link = document.createElement("a");
      link.href = `${VIEW_API}/cases/${entry.case_id}/documents/${doc.document_id}`;
      link.textContent = `${doc.name} (${doc.type})`;
      link.target = "_blank";
      docItem.appendChild(link);
      documents.appendChild(docItem);
    });

    item.appendChild(documents);
    caseList.appendChild(item);
  });
}

fileInput.addEventListener("change", (event) => {
  addFiles(event.target.files);
  fileInput.value = "";
});

dropzone.addEventListener("dragover", (event) => {
  event.preventDefault();
  dropzone.classList.add("active");
});

dropzone.addEventListener("dragleave", () => {
  dropzone.classList.remove("active");
});

dropzone.addEventListener("drop", (event) => {
  event.preventDefault();
  dropzone.classList.remove("active");
  addFiles(event.dataTransfer.files);
});

form.addEventListener("submit", handleSubmit);
refreshButton.addEventListener("click", loadCases);

loadCases().catch((error) => console.error(error));
