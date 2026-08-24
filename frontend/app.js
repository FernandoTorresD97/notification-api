// Notification API frontend — plain JS, no build step required.

const state = {
  apiBaseUrl: localStorage.getItem("notif_api_base_url") || "http://localhost:8080",
  channels: [],
};

const el = {
  apiBaseUrl: document.getElementById("apiBaseUrl"),
  saveApiUrl: document.getElementById("saveApiUrl"),
  apiStatus: document.getElementById("apiStatus"),

  channelForm: document.getElementById("channelForm"),
  channelName: document.getElementById("channelName"),
  channelDescription: document.getElementById("channelDescription"),
  channelActive: document.getElementById("channelActive"),
  channelsList: document.getElementById("channelsList"),

  notificationForm: document.getElementById("notificationForm"),
  notifTitle: document.getElementById("notifTitle"),
  notifMessage: document.getElementById("notifMessage"),
  notifRecipient: document.getElementById("notifRecipient"),
  notifChannel: document.getElementById("notifChannel"),
  notifScheduledAt: document.getElementById("notifScheduledAt"),
  notificationsList: document.getElementById("notificationsList"),

  statusFilter: document.getElementById("statusFilter"),
  refreshNotifications: document.getElementById("refreshNotifications"),

  toast: document.getElementById("toast"),
};

el.apiBaseUrl.value = state.apiBaseUrl;

// ---------- helpers ----------

function showToast(message, type) {
  el.toast.textContent = message;
  el.toast.className = `toast ${type || ""}`;
  requestAnimationFrame(() => el.toast.classList.remove("hidden"));
  clearTimeout(showToast._t);
  showToast._t = setTimeout(() => el.toast.classList.add("hidden"), 3500);
}

async function api(path, options) {
  const res = await fetch(`${state.apiBaseUrl}${path}`, {
    headers: { "Content-Type": "application/json" },
    ...options,
  });

  if (res.status === 204) {
    return null;
  }

  const body = await res.json().catch(() => null);

  if (!res.ok) {
    const message = body && body.message ? body.message : `HTTP ${res.status}`;
    throw new Error(message);
  }

  return body;
}

function formatDate(value) {
  if (!value) return "—";
  return new Date(value).toLocaleString("pt-BR");
}

// ---------- API health ----------

async function checkApiHealth() {
  try {
    await api("/api/v1/channels");
    el.apiStatus.className = "status-dot online";
    el.apiStatus.title = "API online";
  } catch {
    el.apiStatus.className = "status-dot offline";
    el.apiStatus.title = "API offline ou inacessível";
  }
}

// ---------- Channels ----------

async function loadChannels() {
  try {
    state.channels = await api("/api/v1/channels");
    renderChannels();
    renderChannelOptions();
    el.apiStatus.className = "status-dot online";
  } catch (err) {
    el.channelsList.innerHTML = `<div class="empty-state">Não foi possível carregar os canais (${err.message}).</div>`;
    el.apiStatus.className = "status-dot offline";
  }
}

function renderChannels() {
  if (!state.channels.length) {
    el.channelsList.innerHTML = `<div class="empty-state">Nenhum canal cadastrado ainda.</div>`;
    return;
  }

  el.channelsList.innerHTML = state.channels
    .map(
      (c) => `
      <div class="card" data-id="${c.id}">
        <div class="card-header">
          <span class="card-title">${escapeHtml(c.name)}</span>
          <span class="badge badge-${c.active ? "active" : "inactive"}">${c.active ? "ativo" : "inativo"}</span>
        </div>
        <div class="card-meta">${escapeHtml(c.description || "sem descrição")} · id ${c.id}</div>
        <div class="card-actions">
          <button class="secondary toggle-active" data-id="${c.id}">${c.active ? "Desativar" : "Ativar"}</button>
          <button class="danger delete-channel" data-id="${c.id}">Excluir</button>
        </div>
      </div>`
    )
    .join("");

  el.channelsList.querySelectorAll(".toggle-active").forEach((btn) =>
    btn.addEventListener("click", () => toggleChannelActive(btn.dataset.id))
  );
  el.channelsList.querySelectorAll(".delete-channel").forEach((btn) =>
    btn.addEventListener("click", () => deleteChannel(btn.dataset.id))
  );
}

function renderChannelOptions() {
  el.notifChannel.innerHTML = state.channels
    .map((c) => `<option value="${c.id}">${escapeHtml(c.name)}${c.active ? "" : " (inativo)"}</option>`)
    .join("");
}

async function toggleChannelActive(id) {
  const channel = state.channels.find((c) => String(c.id) === String(id));
  if (!channel) return;

  try {
    await api(`/api/v1/channels/${id}`, {
      method: "PUT",
      body: JSON.stringify({
        name: channel.name,
        description: channel.description,
        active: !channel.active,
      }),
    });
    showToast("Canal atualizado.", "success");
    await loadChannels();
  } catch (err) {
    showToast(`Erro ao atualizar canal: ${err.message}`, "error");
  }
}

async function deleteChannel(id) {
  if (!confirm("Excluir este canal?")) return;

  try {
    await api(`/api/v1/channels/${id}`, { method: "DELETE" });
    showToast("Canal excluído.", "success");
    await loadChannels();
  } catch (err) {
    showToast(`Erro ao excluir canal: ${err.message}`, "error");
  }
}

el.channelForm.addEventListener("submit", async (e) => {
  e.preventDefault();

  try {
    await api("/api/v1/channels", {
      method: "POST",
      body: JSON.stringify({
        name: el.channelName.value.trim(),
        description: el.channelDescription.value.trim(),
        active: el.channelActive.checked,
      }),
    });
    showToast("Canal criado.", "success");
    el.channelForm.reset();
    el.channelActive.checked = true;
    await loadChannels();
  } catch (err) {
    showToast(`Erro ao criar canal: ${err.message}`, "error");
  }
});

// ---------- Notifications ----------

async function loadNotifications() {
  const status = el.statusFilter.value;
  const query = status ? `?status=${status}&size=50` : "?size=50";

  try {
    const page = await api(`/api/v1/notifications${query}`);
    renderNotifications(page.content || []);
  } catch (err) {
    el.notificationsList.innerHTML = `<div class="empty-state">Não foi possível carregar notificações (${err.message}).</div>`;
  }
}

function renderNotifications(items) {
  if (!items.length) {
    el.notificationsList.innerHTML = `<div class="empty-state">Nenhuma notificação encontrada.</div>`;
    return;
  }

  el.notificationsList.innerHTML = items
    .map(
      (n) => `
      <div class="card" data-id="${n.id}">
        <div class="card-header">
          <span class="card-title">${escapeHtml(n.title)}</span>
          <span class="badge badge-${n.status}">${n.status}</span>
        </div>
        <div class="card-meta">${escapeHtml(n.message)}</div>
        <div class="card-meta">
          para <strong>${escapeHtml(n.recipient)}</strong> via <strong>${escapeHtml(n.channelName)}</strong>
        </div>
        <div class="card-meta">
          criada em ${formatDate(n.createdAt)}
          ${n.scheduledAt ? ` · agendada para ${formatDate(n.scheduledAt)}` : ""}
          ${n.sentAt ? ` · enviada em ${formatDate(n.sentAt)}` : ""}
        </div>
        <div class="card-actions">
          ${
            n.status === "PENDING" || n.status === "SCHEDULED"
              ? `<button class="secondary cancel-notif" data-id="${n.id}">Cancelar</button>`
              : ""
          }
          <button class="danger delete-notif" data-id="${n.id}">Excluir</button>
        </div>
      </div>`
    )
    .join("");

  el.notificationsList.querySelectorAll(".cancel-notif").forEach((btn) =>
    btn.addEventListener("click", () => cancelNotification(btn.dataset.id))
  );
  el.notificationsList.querySelectorAll(".delete-notif").forEach((btn) =>
    btn.addEventListener("click", () => deleteNotification(btn.dataset.id))
  );
}

async function cancelNotification(id) {
  try {
    await api(`/api/v1/notifications/${id}/cancel`, { method: "PATCH" });
    showToast("Notificação cancelada.", "success");
    await loadNotifications();
  } catch (err) {
    showToast(`Erro ao cancelar: ${err.message}`, "error");
  }
}

async function deleteNotification(id) {
  if (!confirm("Excluir esta notificação?")) return;

  try {
    await api(`/api/v1/notifications/${id}`, { method: "DELETE" });
    showToast("Notificação excluída.", "success");
    await loadNotifications();
  } catch (err) {
    showToast(`Erro ao excluir: ${err.message}`, "error");
  }
}

el.notificationForm.addEventListener("submit", async (e) => {
  e.preventDefault();

  const payload = {
    title: el.notifTitle.value.trim(),
    message: el.notifMessage.value.trim(),
    recipient: el.notifRecipient.value.trim(),
    channelId: Number(el.notifChannel.value),
  };

  if (el.notifScheduledAt.value) {
    payload.scheduledAt = el.notifScheduledAt.value;
  }

  try {
    await api("/api/v1/notifications", {
      method: "POST",
      body: JSON.stringify(payload),
    });
    showToast("Notificação criada.", "success");
    el.notificationForm.reset();
    await loadNotifications();
  } catch (err) {
    showToast(`Erro ao criar notificação: ${err.message}`, "error");
  }
});

el.statusFilter.addEventListener("change", loadNotifications);
el.refreshNotifications.addEventListener("click", loadNotifications);

el.saveApiUrl.addEventListener("click", () => {
  state.apiBaseUrl = el.apiBaseUrl.value.trim().replace(/\/$/, "");
  localStorage.setItem("notif_api_base_url", state.apiBaseUrl);
  showToast("URL da API atualizada.", "success");
  init();
});

function escapeHtml(str) {
  const div = document.createElement("div");
  div.textContent = str ?? "";
  return div.innerHTML;
}

// ---------- init ----------

async function init() {
  await checkApiHealth();
  await loadChannels();
  await loadNotifications();
}

init();
setInterval(checkApiHealth, 15000);
