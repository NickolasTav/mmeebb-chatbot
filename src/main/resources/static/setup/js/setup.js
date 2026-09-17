(function () {
    "use strict";

    const API_BASE = "/api/v1/setup";
    const ADMIN_KEY_STORAGE = "mmeebb_admin_api_key";

    let pollingTimer = null;
    let countdownTimer = null;

    function getAdminKey() {
        return sessionStorage.getItem(ADMIN_KEY_STORAGE) || "";
    }

    function setupAdminKeyField() {
        const input = document.getElementById("input-admin-key");
        const saveBtn = document.getElementById("btn-save-admin-key");
        input.value = getAdminKey();

        saveBtn.addEventListener("click", () => {
            sessionStorage.setItem(ADMIN_KEY_STORAGE, input.value.trim());
            loadStatus();
        });
    }

    async function api(path, options) {
        const response = await fetch(API_BASE + path, Object.assign({
            headers: Object.assign({
                "Content-Type": "application/json",
                "api_key": getAdminKey()
            }, (options && options.headers) || {})
        }, options));

        if (!response.ok) {
            throw new Error("HTTP " + response.status + " em " + path);
        }
        return response.status === 204 ? null : response.json();
    }

    function setBadge(elementId, online, onlineText, offlineText) {
        const el = document.getElementById(elementId);
        el.textContent = online ? onlineText : offlineText;
        el.className = "px-2 py-1 rounded text-sm font-medium " +
            (online ? "bg-emerald-100 text-emerald-800" : "bg-red-100 text-red-800");
    }

    async function loadStatus() {
        try {
            const status = await api("/status");
            renderStatus(status);
        } catch (e) {
            console.warn("[setup] Falha ao carregar status:", e.message);
        }
    }

    function renderStatus(status) {
        const ngrok = status.ngrok || {};
        setBadge("ngrok-status-badge", ngrok.online, "🟢 ONLINE", "🔴 OFFLINE");
        document.getElementById("ngrok-public-url").textContent = ngrok.publicUrl || "—";

        const uazapi = status.uazapi || {};
        setBadge("uazapi-status-badge", uazapi.connected, "🟢 Conectado", "🔴 Desconectado");
        if (uazapi.baseUrl) document.getElementById("input-base-url").value = uazapi.baseUrl;
        if (uazapi.instance) document.getElementById("input-instance").value = uazapi.instance;

        const webhook = status.webhook || {};
        document.getElementById("webhook-url").textContent = webhook.targetUrl || "—";
    }

    async function discoverTunnel() {
        try {
            await api("/tunnel/discover", { method: "POST" });
        } finally {
            loadStatus();
        }
    }

    async function provisionInstance() {
        const feedback = document.getElementById("provision-feedback");
        feedback.textContent = "Provisionando nova instância…";
        feedback.className = "mt-1 text-sm text-slate-500";

        try {
            const result = await api("/instance/provision", { method: "POST" });
            feedback.textContent = result.message;
            feedback.className = "mt-1 text-sm " + (result.success ? "text-emerald-700" : "text-red-600");
            if (result.success) {
                document.getElementById("input-instance").value = result.instance || "";
                document.getElementById("input-api-key").value = "";
                document.getElementById("input-api-key").placeholder = result.maskedApiKey || "token salvo no banco";
                loadStatus();
            }
        } catch (e) {
            feedback.textContent = "Falha ao provisionar: " + e.message;
            feedback.className = "mt-1 text-sm text-red-600";
        }
    }

    async function connectInstance() {
        const baseUrl = document.getElementById("input-base-url").value.trim();
        const instance = document.getElementById("input-instance").value.trim();
        const apiKey = document.getElementById("input-api-key").value.trim();

        try {
            const qr = await api("/instance/connect", {
                method: "POST",
                body: JSON.stringify({ baseUrl: baseUrl, instance: instance, apiKey: apiKey || null })
            });
            renderQrCode(qr);
            startStatusPolling();
        } catch (e) {
            document.getElementById("qrcode-container").innerHTML =
                '<span class="text-red-500 text-sm text-center px-4">Falha ao gerar QR Code: ' + e.message + '</span>';
        }
    }

    function renderQrCode(qr) {
        const container = document.getElementById("qrcode-container");
        if (qr && qr.qrcodeBase64) {
            const src = qr.qrcodeBase64.startsWith("data:")
                ? qr.qrcodeBase64
                : "data:image/png;base64," + qr.qrcodeBase64;
            container.innerHTML = '<img src="' + src + '" alt="QR Code WhatsApp" class="w-52 h-52 object-contain">';
            startQrCountdown(qr.expiresInSeconds || 45);
        } else {
            container.innerHTML = '<span class="text-slate-400 text-sm text-center px-4">' +
                ((qr && qr.message) || "QR Code indisponível.") + '</span>';
        }
    }

    function startQrCountdown(seconds) {
        clearInterval(countdownTimer);
        let remaining = seconds;
        const el = document.getElementById("qrcode-countdown");
        el.textContent = remaining;
        countdownTimer = setInterval(() => {
            remaining -= 1;
            el.textContent = remaining >= 0 ? remaining : 0;
            if (remaining <= 0) {
                clearInterval(countdownTimer);
            }
        }, 1000);
    }

    function startStatusPolling() {
        clearInterval(pollingTimer);
        let attempts = 0;
        pollingTimer = setInterval(async () => {
            attempts += 1;
            try {
                const instanceStatus = await api("/instance/status");
                if (instanceStatus.connected) {
                    clearInterval(pollingTimer);
                    clearInterval(countdownTimer);
                    setBadge("uazapi-status-badge", true, "🟢 Conectado", "🔴 Desconectado");
                    loadStatus();
                }
            } catch (e) {
                console.warn("[setup] Polling de status falhou:", e.message);
            }
            if (attempts >= 30) {
                clearInterval(pollingTimer);
            }
        }, 3000);
    }

    async function syncWebhook() {
        try {
            const result = await api("/webhook/sync", { method: "POST", body: "{}" });
            document.getElementById("webhook-url").textContent = result.configuredUrl || "—";
        } finally {
            loadStatus();
        }
    }

    async function sendTestMessage() {
        const phone = document.getElementById("input-test-phone").value.trim();
        const feedback = document.getElementById("test-feedback");
        if (!phone) {
            feedback.textContent = "Informe um telefone de teste.";
            feedback.className = "mt-2 text-sm text-red-600";
            return;
        }

        try {
            await api("/test-message", {
                method: "POST",
                body: JSON.stringify({
                    targetPhone: phone,
                    message: "🩺 Teste de Conexão MMEEBB: o chatbot está conectado e operando normalmente via Uazapi + Ngrok."
                })
            });
            feedback.textContent = "Mensagem de teste enviada com sucesso!";
            feedback.className = "mt-2 text-sm text-emerald-700";
        } catch (e) {
            feedback.textContent = "Falha ao enviar: " + e.message;
            feedback.className = "mt-2 text-sm text-red-600";
        }
    }

    document.addEventListener("DOMContentLoaded", () => {
        setupAdminKeyField();
        loadStatus();

        document.getElementById("btn-discover-tunnel").addEventListener("click", discoverTunnel);
        document.getElementById("btn-connect-instance").addEventListener("click", connectInstance);
        document.getElementById("btn-provision-instance").addEventListener("click", provisionInstance);
        document.getElementById("btn-new-qrcode").addEventListener("click", connectInstance);
        document.getElementById("btn-sync-webhook").addEventListener("click", syncWebhook);
        document.getElementById("btn-send-test").addEventListener("click", sendTestMessage);
    });
})();
