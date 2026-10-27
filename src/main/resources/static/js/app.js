(() => {
    "use strict";

    const SESSION_KEY = "freshmind.session.v1";
    const ROUTES = ["dashboard", "inventory", "recipes", "notifications"];
    const CATEGORY_LABELS = { VEGETABLES: "蔬菜", FRUITS: "水果", MEAT: "肉类", OTHERS: "其他" };
    const STATUS_LABELS = { FRESH: "新鲜", NEAR_EXPIRY: "临期", EXPIRED: "已过期" };
    const PAGE_META = {
        dashboard: ["OVERVIEW", "概览", "掌握今天的库存状态。"],
        inventory: ["PANTRY", "库存管理", "查询、补充和消耗你的食材。"],
        recipes: ["AI RECIPES", "智能菜谱", "让现有库存成为下一顿饭。"],
        notifications: ["REMINDERS", "提醒中心", "及时处理临期和过期食材。"]
    };

    const state = {
        session: readSession(),
        route: "dashboard",
        catalog: null,
        inventory: null,
        inventoryItems: new Map(),
        inventoryPage: 0,
        inventoryRequest: 0,
        recommendations: null,
        notifications: [],
        notificationFilter: "all",
        confirmResolve: null
    };

    const $ = (selector, root = document) => root.querySelector(selector);
    const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];

    class ApiProblem extends Error {
        constructor(response, body) {
            super(body?.message || `请求失败（${response.status}）`);
            this.status = response.status;
            this.code = body?.code || "HTTP_ERROR";
            this.errors = body?.errors || {};
        }
    }

    document.addEventListener("DOMContentLoaded", init);

    function init() {
        bindEvents();
        setTodayLabel();
        setDateConstraints();
        if (state.session && (!state.session.expiresAt || state.session.expiresAt > Date.now())) {
            showApplication();
            navigate(routeFromHash(), true);
        } else {
            clearSession();
            showAuthentication();
        }
    }

    function bindEvents() {
        document.addEventListener("click", handleDocumentClick);
        window.addEventListener("hashchange", () => {
            if (state.session) navigate(routeFromHash(), true);
        });

        $("#loginForm").addEventListener("submit", submitLogin);
        $("#registerForm").addEventListener("submit", submitRegister);
        $("#logoutButton").addEventListener("click", logout);
        $("#inventoryForm").addEventListener("submit", submitInventory);
        $("#consumeForm").addEventListener("submit", submitConsumption);
        $("#ingredientName").addEventListener("input", applyCatalogCategory);
        $("#purchasedOn").addEventListener("change", syncExpiryMinimum);

        const debouncedSearch = debounce(() => {
            state.inventoryPage = 0;
            loadInventory();
        }, 320);
        $("#inventorySearch").addEventListener("input", debouncedSearch);
        ["#inventoryCategory", "#inventoryStatus", "#inventorySort"].forEach(id => {
            $(id).addEventListener("change", () => {
                state.inventoryPage = 0;
                loadInventory();
            });
        });
        $("#refreshInventory").addEventListener("click", loadInventory);
        $("#previousPage").addEventListener("click", () => changeInventoryPage(-1));
        $("#nextPage").addEventListener("click", () => changeInventoryPage(1));
        $("#generateRecipes").addEventListener("click", loadRecommendations);
        $("#refreshNotifications").addEventListener("click", () => loadNotifications(false));

        $("#confirmCancel").addEventListener("click", () => resolveConfirmation(false));
        $("#confirmSubmit").addEventListener("click", () => resolveConfirmation(true));
        $("#confirmDialog").addEventListener("cancel", event => {
            event.preventDefault();
            resolveConfirmation(false);
        });

        $$("dialog").forEach(dialog => {
            dialog.addEventListener("click", event => {
                if (event.target === dialog && dialog.id !== "confirmDialog") dialog.close();
            });
        });
    }

    function handleDocumentClick(event) {
        const authTab = event.target.closest("[data-auth-tab]");
        if (authTab) switchAuthTab(authTab.dataset.authTab);

        const notificationFilter = event.target.closest("[data-notification-filter]");
        if (notificationFilter) {
            state.notificationFilter = notificationFilter.dataset.notificationFilter;
            renderNotifications();
        }

        const routeButton = event.target.closest("[data-route]");
        if (routeButton && state.session) navigate(routeButton.dataset.route);

        const closeButton = event.target.closest("[data-close-dialog]");
        if (closeButton) closeButton.closest("dialog")?.close();

        const action = event.target.closest("[data-action]");
        if (!action) return;
        switch (action.dataset.action) {
            case "open-add": openInventoryDialog(); break;
            case "edit-item": openInventoryDialog(state.inventoryItems.get(action.dataset.id)); break;
            case "consume-item": openConsumeDialog(state.inventoryItems.get(action.dataset.id)); break;
            case "delete-item": deleteInventoryItem(state.inventoryItems.get(action.dataset.id)); break;
            case "open-recipe": openRecipeDialog(Number(action.dataset.index)); break;
            case "mark-read": markNotificationRead(action.dataset.id); break;
            case "focus-item": navigate("inventory"); break;
            default: break;
        }
    }

    async function api(path, options = {}) {
        const headers = new Headers(options.headers || {});
        headers.set("Accept", "application/json");
        if (options.body && !headers.has("Content-Type")) headers.set("Content-Type", "application/json");
        if (state.session?.token) headers.set("X-Token", state.session.token);

        let response;
        try {
            response = await fetch(path, { ...options, headers });
        } catch (error) {
            throw new Error("无法连接到 FreshMind 服务，请确认后端已经启动。", { cause: error });
        }

        let body = null;
        if (response.status !== 204) {
            const text = await response.text();
            if (text) {
                try { body = JSON.parse(text); } catch { body = { message: text }; }
            }
        }

        if (!response.ok) {
            const problem = new ApiProblem(response, body);
            if (response.status === 401 && state.session) {
                clearSession();
                showAuthentication();
                toast("登录已过期，请重新登录。", "error");
            }
            throw problem;
        }
        return body;
    }

    async function submitLogin(event) {
        event.preventDefault();
        const form = event.currentTarget;
        if (!form.reportValidity()) return;
        clearFormErrors(form);
        const button = $("button[type='submit']", form);
        setBusy(button, true, "登录中…");
        try {
            const result = await api("/api/v1/auth/login", {
                method: "POST",
                body: JSON.stringify({
                    email: $("#loginEmail").value.trim(),
                    password: $("#loginPassword").value
                })
            });
            acceptAuthentication(result, "欢迎回来！");
        } catch (error) {
            showFormProblem(form, error, $("#loginError"));
        } finally {
            setBusy(button, false);
        }
    }

    async function submitRegister(event) {
        event.preventDefault();
        const form = event.currentTarget;
        if (!form.reportValidity()) return;
        clearFormErrors(form);
        const button = $("button[type='submit']", form);
        setBusy(button, true, "创建中…");
        try {
            const result = await api("/api/v1/auth/register", {
                method: "POST",
                body: JSON.stringify({
                    displayName: $("#registerName").value.trim(),
                    email: $("#registerEmail").value.trim(),
                    password: $("#registerPassword").value
                })
            });
            acceptAuthentication(result, "账户创建成功，欢迎使用 FreshMind！");
        } catch (error) {
            showFormProblem(form, error, $("#registerError"));
        } finally {
            setBusy(button, false);
        }
    }

    function acceptAuthentication(result, message) {
        state.session = {
            token: result.token,
            user: result.user,
            expiresAt: Date.now() + result.expiresInSeconds * 1000
        };
        localStorage.setItem(SESSION_KEY, JSON.stringify(state.session));
        resetApplicationState();
        showApplication();
        navigate("dashboard");
        toast(message, "success");
    }

    async function logout() {
        const button = $("#logoutButton");
        button.disabled = true;
        try { await api("/api/v1/auth/logout", { method: "POST" }); } catch { /* local logout still succeeds */ }
        clearSession();
        resetApplicationState();
        showAuthentication();
        button.disabled = false;
        toast("已安全退出登录。", "success");
    }

    function switchAuthTab(tab) {
        const login = tab === "login";
        $("#loginForm").hidden = !login;
        $("#registerForm").hidden = login;
        $$("[data-auth-tab]").forEach(button => {
            const active = button.dataset.authTab === tab;
            button.classList.toggle("active", active);
            button.setAttribute("aria-selected", String(active));
        });
    }

    function showAuthentication() {
        $("#appView").hidden = true;
        $("#authView").hidden = false;
        switchAuthTab("login");
        window.location.hash = "";
    }

    function showApplication() {
        $("#authView").hidden = true;
        $("#appView").hidden = false;
        const user = state.session.user;
        const name = user?.displayName || "用户";
        $("#sidebarUserName").textContent = name;
        $("#sidebarUserEmail").textContent = user?.email || "";
        $("#welcomeName").textContent = name;
        $("#userAvatar").textContent = firstCharacter(name);
    }

    function navigate(route, fromHash = false) {
        if (!ROUTES.includes(route)) route = "dashboard";
        state.route = route;
        if (!fromHash && window.location.hash !== `#${route}`) history.pushState(null, "", `#${route}`);

        $$("[data-page]").forEach(page => {
            const active = page.dataset.page === route;
            page.hidden = !active;
            page.classList.toggle("active", active);
        });
        $$("[data-route]").forEach(button => button.classList.toggle("active", button.dataset.route === route));
        const [eyebrow, title, subtitle] = PAGE_META[route];
        $("#pageEyebrow").textContent = eyebrow;
        $("#pageTitle").textContent = title;
        $("#pageSubtitle").textContent = subtitle;
        window.scrollTo({ top: 0, behavior: "smooth" });

        if (route === "dashboard") loadDashboard();
        if (route === "inventory") { ensureCatalog(); loadInventory(); }
        if (route === "notifications") loadNotifications(false);
    }

    async function loadDashboard() {
        $("#dashboardLoading").hidden = false;
        $("#statsGrid").hidden = true;
        try {
            const dashboard = await api("/api/v1/dashboard");
            renderDashboard(dashboard);
            await loadNotifications(true);
        } catch (error) {
            if (error.status !== 401) toast(errorMessage(error), "error");
        } finally {
            $("#dashboardLoading").hidden = true;
        }
    }

    function renderDashboard(data) {
        $("#statTotal").textContent = data.totalItems;
        $("#statFresh").textContent = data.freshItems;
        $("#statNear").textContent = data.nearExpiryItems;
        $("#statExpired").textContent = data.expiredItems;
        $("#legendFresh").textContent = data.freshItems;
        $("#legendNear").textContent = data.nearExpiryItems;
        $("#legendExpired").textContent = data.expiredItems;
        const percent = data.totalItems ? Math.round(data.freshItems / data.totalItems * 100) : 0;
        $("#healthPercent").textContent = `${percent}%`;
        $("#healthDonut").style.background = `conic-gradient(var(--green) 0 ${percent}%, #e9eee4 ${percent}% 100%)`;
        $("#statsGrid").hidden = false;

        const urgent = $("#urgentList");
        if (!data.urgentItems.length) {
            urgent.innerHTML = `<div class="empty-compact">目前没有临期或过期食材，状态很好。</div>`;
            return;
        }
        urgent.innerHTML = data.urgentItems.map(item => `
            <button class="urgent-item reset-button" data-action="focus-item" type="button">
                <span class="food-avatar">${foodEmoji(item.category)}</span>
                <span class="urgent-copy"><strong>${escapeHtml(item.ingredientName)}</strong><span>${escapeHtml(quantityText(item))} · ${expiryText(item)}</span></span>
                ${statusPill(item.status)}
            </button>`).join("");
    }

    async function ensureCatalog() {
        if (state.catalog) return state.catalog;
        try {
            state.catalog = await api("/api/v1/ingredients");
            const options = $("#ingredientOptions");
            options.innerHTML = state.catalog.map(item => `<option value="${escapeAttr(item.name)}">${CATEGORY_LABELS[item.category] || item.category}</option>`).join("");
        } catch (error) {
            if (error.status !== 401) toast(errorMessage(error), "error");
            state.catalog = [];
        }
        return state.catalog;
    }

    async function loadInventory() {
        if (!state.session) return;
        const requestNumber = ++state.inventoryRequest;
        $("#inventoryLoading").hidden = false;
        $("#inventoryContent").hidden = true;
        $("#inventoryEmpty").hidden = true;
        const params = new URLSearchParams({ page: state.inventoryPage, size: 12 });
        const query = $("#inventorySearch").value.trim();
        const category = $("#inventoryCategory").value;
        const status = $("#inventoryStatus").value;
        const [sortBy, direction] = $("#inventorySort").value.split(":");
        if (query) params.set("query", query);
        if (category) params.set("category", category);
        if (status) params.set("status", status);
        params.set("sortBy", sortBy);
        params.set("direction", direction);

        try {
            const page = await api(`/api/v1/inventory?${params}`);
            if (requestNumber !== state.inventoryRequest) return;
            state.inventory = page;
            state.inventoryItems = new Map(page.content.map(item => [item.id, item]));
            renderInventory(page);
        } catch (error) {
            if (requestNumber === state.inventoryRequest && error.status !== 401) toast(errorMessage(error), "error");
        } finally {
            if (requestNumber === state.inventoryRequest) $("#inventoryLoading").hidden = true;
        }
    }

    function renderInventory(page) {
        $("#inventoryCount").textContent = `共 ${page.totalElements} 条库存`;
        if (!page.content.length) {
            $("#inventoryEmpty").hidden = false;
            $("#inventoryContent").hidden = true;
            return;
        }
        $("#inventoryEmpty").hidden = true;
        $("#inventoryContent").hidden = false;
        $("#inventoryTableBody").innerHTML = page.content.map(item => `
            <tr>
                <td>${foodCell(item)}</td><td><span class="category-chip">${CATEGORY_LABELS[item.category] || escapeHtml(item.category)}</span></td>
                <td><strong>${escapeHtml(quantityText(item))}</strong></td><td>${formatDate(item.purchasedOn)}</td><td>${formatDate(item.expiresOn)}${item.expiryEstimated ? `<small class="estimated-label">估算</small>` : ""}</td>
                <td>${statusPill(item.status)}</td><td>${rowActions(item.id)}</td>
            </tr>`).join("");
        $("#inventoryCardList").innerHTML = page.content.map(item => `
            <article class="inventory-mobile-card"><div class="inventory-mobile-top">${foodCell(item)}${statusPill(item.status)}</div>
                <div class="inventory-mobile-details"><div><span>数量</span><strong>${escapeHtml(quantityText(item))}</strong></div><div><span>到期日期</span><strong>${formatDate(item.expiresOn)}</strong></div></div>${rowActions(item.id)}</article>`).join("");
        const totalPages = Math.max(page.totalPages, 1);
        $("#pageIndicator").textContent = `第 ${page.page + 1} / ${totalPages} 页`;
        $("#previousPage").disabled = page.first;
        $("#nextPage").disabled = page.last;
    }

    function foodCell(item) {
        return `<div class="food-cell"><span class="food-avatar">${foodEmoji(item.category)}</span><span><strong>${escapeHtml(item.ingredientName)}</strong><small>${CATEGORY_LABELS[item.category] || escapeHtml(item.category)}</small></span></div>`;
    }

    function rowActions(id) {
        return `<div class="row-actions">
            <button class="icon-button" data-action="consume-item" data-id="${id}" title="消耗" aria-label="消耗库存">${icon("minus")}</button>
            <button class="icon-button" data-action="edit-item" data-id="${id}" title="编辑" aria-label="编辑库存">${icon("edit")}</button>
            <button class="icon-button delete-action" data-action="delete-item" data-id="${id}" title="删除" aria-label="删除库存">${icon("trash")}</button>
        </div>`;
    }

    function changeInventoryPage(delta) {
        if (!state.inventory) return;
        const next = state.inventoryPage + delta;
        if (next < 0 || next >= state.inventory.totalPages) return;
        state.inventoryPage = next;
        loadInventory();
    }

    async function openInventoryDialog(item = null) {
        await ensureCatalog();
        const form = $("#inventoryForm");
        form.reset();
        clearFormErrors(form);
        $("#inventoryFormError").hidden = true;
        $("#inventoryId").value = item?.id || "";
        $("#inventoryDialogTitle").textContent = item ? "编辑食材" : "添加食材";
        $("#ingredientName").value = item?.ingredientName || "";
        $("#ingredientCategory").value = item?.category || "";
        $("#ingredientQuantity").value = item?.quantity ?? "";
        $("#ingredientUnit").value = item?.unit || "";
        $("#purchasedOn").value = item?.purchasedOn || localDateString(new Date());
        $("#expiresOn").value = item?.expiresOn || "";
        $("#expiresOn").required = Boolean(item);
        $("#expiryHelp").textContent = item ? "编辑库存时到期日期必须填写。" : "新增时可留空，系统将按食材目录估算。";
        syncExpiryMinimum();
        $("#inventoryDialog").showModal();
        setTimeout(() => $("#ingredientName").focus(), 50);
    }

    function applyCatalogCategory() {
        const name = $("#ingredientName").value.trim().toLocaleLowerCase();
        const match = state.catalog?.find(item => item.name.toLocaleLowerCase() === name);
        if (match) $("#ingredientCategory").value = match.category;
    }

    function syncExpiryMinimum() {
        const purchased = $("#purchasedOn").value;
        $("#expiresOn").min = purchased;
        if ($("#expiresOn").value && $("#expiresOn").value < purchased) $("#expiresOn").value = purchased;
    }

    async function submitInventory(event) {
        event.preventDefault();
        const form = event.currentTarget;
        if (!form.reportValidity()) return;
        clearFormErrors(form);
        $("#inventoryFormError").hidden = true;
        const id = $("#inventoryId").value;
        const expiresOn = $("#expiresOn").value;
        if (id && !expiresOn) {
            showFieldError(form, "expiresOn", "编辑库存时必须填写到期日期。");
            return;
        }
        const body = {
            ingredientName: $("#ingredientName").value.trim(),
            category: $("#ingredientCategory").value,
            quantity: Number($("#ingredientQuantity").value),
            unit: $("#ingredientUnit").value.trim(),
            purchasedOn: $("#purchasedOn").value
        };
        if (expiresOn) body.expiresOn = expiresOn;
        const button = $("#saveInventory");
        setBusy(button, true, "保存中…");
        try {
            await api(id ? `/api/v1/inventory/${id}` : "/api/v1/inventory", {
                method: id ? "PATCH" : "POST",
                body: JSON.stringify(body)
            });
            $("#inventoryDialog").close();
            state.inventoryPage = 0;
            state.recommendations = null;
            await loadInventory();
            if (state.route === "dashboard") await loadDashboard();
            toast(id ? "库存已更新。" : "食材已添加。", "success");
        } catch (error) {
            showFormProblem(form, error, $("#inventoryFormError"));
        } finally {
            setBusy(button, false);
        }
    }

    function openConsumeDialog(item) {
        if (!item) return;
        const form = $("#consumeForm");
        form.reset();
        clearFormErrors(form);
        $("#consumeFormError").hidden = true;
        $("#consumeItemId").value = item.id;
        $("#consumeItemName").textContent = item.ingredientName;
        $("#consumeCurrent").textContent = quantityText(item);
        $("#consumeAmount").max = item.quantity;
        $("#consumeDialog").showModal();
        setTimeout(() => $("#consumeAmount").focus(), 50);
    }

    async function submitConsumption(event) {
        event.preventDefault();
        const form = event.currentTarget;
        if (!form.reportValidity()) return;
        clearFormErrors(form);
        const button = $("button[type='submit']", form);
        setBusy(button, true, "处理中…");
        try {
            const result = await api(`/api/v1/inventory/${$("#consumeItemId").value}/consume`, {
                method: "POST",
                body: JSON.stringify({ amount: Number($("#consumeAmount").value) })
            });
            $("#consumeDialog").close();
            state.recommendations = null;
            await loadInventory();
            toast(result.depleted ? "库存已全部消耗并移除。" : `已消耗，剩余 ${result.remainingQuantity}。`, "success");
        } catch (error) {
            showFormProblem(form, error, $("#consumeFormError"));
        } finally {
            setBusy(button, false);
        }
    }

    async function deleteInventoryItem(item) {
        if (!item) return;
        const confirmed = await confirmAction("删除这条库存？", `删除“${item.ingredientName}”后无法恢复。`, "确认删除");
        if (!confirmed) return;
        try {
            await api(`/api/v1/inventory/${item.id}`, { method: "DELETE" });
            state.recommendations = null;
            await loadInventory();
            toast("库存已删除。", "success");
        } catch (error) {
            if (error.status !== 401) toast(errorMessage(error), "error");
        }
    }

    async function loadRecommendations() {
        const button = $("#generateRecipes");
        setBusy(button, true, "正在生成…");
        $("#recipeIdle").hidden = true;
        $("#recipeEmpty").hidden = true;
        $("#recipeGrid").hidden = true;
        $("#recipeLoading").hidden = false;
        try {
            state.recommendations = await api("/api/v1/recommendations?limit=8");
            renderRecommendations();
        } catch (error) {
            $("#recipeIdle").hidden = false;
            if (error.status !== 401) toast(errorMessage(error), "error");
        } finally {
            $("#recipeLoading").hidden = true;
            setBusy(button, false);
        }
    }

    function renderRecommendations() {
        const recommendations = state.recommendations || [];
        if (!recommendations.length) {
            $("#recipeEmpty").hidden = false;
            $("#recipeGrid").hidden = true;
            return;
        }
        $("#recipeEmpty").hidden = true;
        $("#recipeGrid").hidden = false;
        $("#recipeGrid").innerHTML = recommendations.map((result, index) => {
            const recipe = result.recipe;
            const matched = result.matchedIngredients.slice(0, 4).map(name => `<span class="ingredient-chip">${escapeHtml(name)}</span>`).join("");
            const missing = result.missingIngredients.slice(0, 2).map(name => `<span class="ingredient-chip missing">缺 ${escapeHtml(name)}</span>`).join("");
            return `<article class="recipe-card">
                <div class="recipe-cover cover-${hashName(recipe.name) % 4}"><svg><use href="#i-leaf"/></svg><span class="match-badge">${Math.round(result.matchRate * 100)}% 匹配</span></div>
                <div class="recipe-card-body"><h3>${escapeHtml(recipe.name)}</h3><div class="recipe-meta"><span>${icon("clock")}${recipe.cookingMinutes} 分钟</span><span>${icon("sparkles")}${result.nearExpiryIngredientsUsed.length} 种临期食材</span></div>
                <p class="recipe-description">${escapeHtml(recipe.description)}</p><div class="chip-row">${matched}${missing}</div>
                <div class="recipe-card-footer"><span>${recipe.ingredients.length} 种食材 · ${recipe.steps.length} 个步骤</span><button class="text-button" data-action="open-recipe" data-index="${index}">查看做法 ${icon("chevron")}</button></div></div>
            </article>`;
        }).join("");
    }

    function openRecipeDialog(index) {
        const result = state.recommendations?.[index];
        if (!result) return;
        const recipe = result.recipe;
        const matched = new Set(result.matchedIngredients.map(normalize));
        $("#recipeDialogTitle").textContent = recipe.name;
        $("#recipeDialogBody").innerHTML = `
            <p class="recipe-detail-intro">${escapeHtml(recipe.description)}</p>
            <div class="detail-summary"><span>${icon("clock")}${recipe.cookingMinutes} 分钟</span><span>${Math.round(result.matchRate * 100)}% 食材匹配</span><span>${result.nearExpiryIngredientsUsed.length} 种临期食材</span></div>
            <h3>所需食材</h3><div class="detail-ingredients">${recipe.ingredients.map(ingredient => {
                const available = matched.has(normalize(ingredient.name));
                return `<div class="detail-ingredient ${available ? "" : "missing"}"><span>${available ? "✓" : "＋"} ${escapeHtml(ingredient.name)}</span><small>${escapeHtml(String(ingredient.amount))} ${escapeHtml(ingredient.unit)}</small></div>`;
            }).join("")}</div>
            <h3>制作步骤</h3><ol class="step-list">${recipe.steps.map(step => `<li>${escapeHtml(step)}</li>`).join("")}</ol>`;
        $("#recipeDialog").showModal();
    }

    async function loadNotifications(silent) {
        if (!silent) {
            $("#notificationLoading").hidden = false;
            $("#notificationList").hidden = true;
            $("#notificationEmpty").hidden = true;
        }
        try {
            state.notifications = await api("/api/v1/notifications");
            updateUnreadBadges();
            renderNotifications();
            renderDashboardNotifications();
        } catch (error) {
            if (!silent && error.status !== 401) toast(errorMessage(error), "error");
        } finally {
            if (!silent) $("#notificationLoading").hidden = true;
        }
    }

    function renderNotifications() {
        const list = state.notificationFilter === "unread" ? state.notifications.filter(item => !item.read) : state.notifications;
        $$("[data-notification-filter]").forEach(button => button.classList.toggle("active", button.dataset.notificationFilter === state.notificationFilter));
        if (!list.length) {
            $("#notificationEmpty").hidden = false;
            $("#notificationList").hidden = true;
            return;
        }
        $("#notificationEmpty").hidden = true;
        $("#notificationList").hidden = false;
        $("#notificationList").innerHTML = list.map(item => notificationMarkup(item, false)).join("");
    }

    function renderDashboardNotifications() {
        const container = $("#dashboardNotifications");
        if (!state.notifications.length) {
            container.innerHTML = `<div class="empty-compact">还没有提醒；每日扫描后会显示在这里。</div>`;
            return;
        }
        container.innerHTML = state.notifications.slice(0, 3).map(item => notificationMarkup(item, true)).join("");
    }

    function notificationMarkup(item, compact) {
        return `<article class="notice-item ${item.read ? "read" : ""}"><span class="notice-marker"></span><div class="notice-copy"><strong>${escapeHtml(item.ingredientName)}</strong><span>${escapeHtml(item.message)} · ${formatDateTime(item.createdAt)}</span></div>${!item.read && !compact ? `<div class="notification-actions"><button class="button button-ghost" data-action="mark-read" data-id="${item.id}">标记已读</button></div>` : ""}</article>`;
    }

    async function markNotificationRead(id) {
        try {
            const updated = await api(`/api/v1/notifications/${id}/read`, { method: "PATCH" });
            state.notifications = state.notifications.map(item => item.id === id ? updated : item);
            updateUnreadBadges();
            renderNotifications();
            renderDashboardNotifications();
            toast("提醒已标记为已读。", "success");
        } catch (error) {
            if (error.status !== 401) toast(errorMessage(error), "error");
        }
    }

    function updateUnreadBadges() {
        const count = state.notifications.filter(item => !item.read).length;
        $$('[data-unread-badge]').forEach(badge => {
            badge.textContent = count > 99 ? "99+" : String(count);
            badge.hidden = count === 0;
        });
    }

    function confirmAction(title, message, confirmLabel) {
        $("#confirmTitle").textContent = title;
        $("#confirmMessage").textContent = message;
        $("#confirmSubmit").textContent = confirmLabel;
        $("#confirmDialog").showModal();
        return new Promise(resolve => { state.confirmResolve = resolve; });
    }

    function resolveConfirmation(value) {
        if (!state.confirmResolve) return;
        const resolve = state.confirmResolve;
        state.confirmResolve = null;
        $("#confirmDialog").close();
        resolve(value);
    }

    function showFormProblem(form, error, alert) {
        clearFormErrors(form);
        if (error instanceof ApiProblem && Object.keys(error.errors).length) {
            Object.entries(error.errors).forEach(([field, message]) => showFieldError(form, field, message));
        }
        alert.textContent = errorMessage(error);
        alert.hidden = false;
    }

    function clearFormErrors(form) {
        $$(".field-error", form).forEach(element => element.textContent = "");
        $$(".invalid", form).forEach(element => element.classList.remove("invalid"));
        const alert = $(".form-alert", form);
        if (alert) alert.hidden = true;
    }

    function showFieldError(form, field, message) {
        const output = $$('[data-error-for]', form).find(element => element.dataset.errorFor === field);
        if (output) output.textContent = translateValidation(message);
        const input = form.elements.namedItem(field);
        if (input?.classList) input.classList.add("invalid");
    }

    function errorMessage(error) {
        if (!(error instanceof ApiProblem)) return error?.message || "发生未知错误，请稍后重试。";
        const codeMessages = {
            UNAUTHORIZED: "邮箱或密码不正确，或者登录已经过期。",
            CONFLICT: "该数据已经存在，请检查后重试。",
            DATA_CONFLICT: "本次操作与现有数据冲突。",
            CONCURRENT_MODIFICATION: "这条库存刚刚被修改，请刷新后重试。",
            NOT_FOUND: "目标数据不存在或已经被删除。",
            VALIDATION_FAILED: "请检查表单中的错误字段。",
            INVALID_REQUEST: "请求内容不符合要求，请检查后重试。"
        };
        return codeMessages[error.code] || error.message;
    }

    function translateValidation(message) {
        const text = String(message || "字段不符合要求");
        if (text.includes("must not be blank")) return "此项不能为空。";
        if (text.includes("must not be null")) return "此项必须填写。";
        if (text.includes("must be a well-formed email")) return "请输入有效的邮箱地址。";
        if (text.includes("size must be between")) return "长度不符合要求。";
        if (text.includes("greater than or equal")) return "数值低于允许的最小值。";
        return text;
    }

    function setBusy(button, busy, busyText = "处理中…") {
        if (busy) {
            button.dataset.originalHtml = button.innerHTML;
            button.disabled = true;
            button.textContent = busyText;
        } else {
            button.disabled = false;
            if (button.dataset.originalHtml) {
                button.innerHTML = button.dataset.originalHtml;
                delete button.dataset.originalHtml;
            }
        }
    }

    function toast(message, type = "success") {
        const element = document.createElement("div");
        element.className = `toast ${type}`;
        const symbol = document.createElement("span");
        symbol.innerHTML = type === "success" ? icon("check") : icon("bell");
        const text = document.createElement("span");
        text.textContent = message;
        element.append(symbol, text);
        $("#toastRegion").append(element);
        setTimeout(() => element.remove(), 3800);
    }

    function statusPill(status) {
        const css = status === "FRESH" ? "fresh" : status === "NEAR_EXPIRY" ? "near" : "expired";
        return `<span class="status-pill status-${css}">${STATUS_LABELS[status] || escapeHtml(status)}</span>`;
    }

    function expiryText(item) {
        if (item.status === "EXPIRED") return `已过期 ${Math.abs(item.daysUntilExpiry)} 天`;
        if (item.daysUntilExpiry === 0) return "今天到期";
        return `${item.daysUntilExpiry} 天后到期`;
    }

    function quantityText(item) { return `${stripNumber(item.quantity)} ${item.unit}`; }
    function stripNumber(value) { const number = Number(value); return Number.isFinite(number) ? String(number) : String(value); }
    function foodEmoji(category) { return { VEGETABLES: "🥬", FRUITS: "🍎", MEAT: "🥩", OTHERS: "🧺" }[category] || "🥣"; }
    function normalize(value) { return String(value || "").trim().replace(/\s+/g, " ").toLocaleLowerCase(); }
    function firstCharacter(value) { return [...String(value || "F")][0]?.toUpperCase() || "F"; }
    function routeFromHash() { const route = window.location.hash.slice(1); return ROUTES.includes(route) ? route : "dashboard"; }
    function setTodayLabel() { $("#todayLabel").textContent = new Intl.DateTimeFormat("zh-CN", { month: "long", day: "numeric", weekday: "short" }).format(new Date()); }
    function setDateConstraints() { $("#purchasedOn").max = localDateString(new Date()); }
    function localDateString(date) { const y = date.getFullYear(); const m = String(date.getMonth() + 1).padStart(2, "0"); const d = String(date.getDate()).padStart(2, "0"); return `${y}-${m}-${d}`; }
    function formatDate(value) { if (!value) return "—"; return new Intl.DateTimeFormat("zh-CN", { year: "numeric", month: "short", day: "numeric" }).format(new Date(`${value}T00:00:00`)); }
    function formatDateTime(value) { if (!value) return "—"; return new Intl.DateTimeFormat("zh-CN", { month: "short", day: "numeric", hour: "2-digit", minute: "2-digit" }).format(new Date(value)); }
    function hashName(value) { return [...String(value)].reduce((hash, character) => (hash * 31 + character.codePointAt(0)) >>> 0, 0); }
    function debounce(fn, delay) { let timer; return (...args) => { clearTimeout(timer); timer = setTimeout(() => fn(...args), delay); }; }

    function icon(name) {
        const allowed = new Set(["minus", "edit", "trash", "clock", "sparkles", "chevron", "check", "bell"]);
        const safeName = allowed.has(name) ? name : "check";
        return `<svg aria-hidden="true"><use href="#i-${safeName}"/></svg>`;
    }

    function escapeHtml(value) {
        return String(value ?? "").replace(/[&<>'"]/g, character => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", "'": "&#39;", '"': "&quot;" }[character]));
    }
    function escapeAttr(value) { return escapeHtml(value).replace(/`/g, "&#96;"); }

    function readSession() {
        try { return JSON.parse(localStorage.getItem(SESSION_KEY) || "null"); }
        catch { localStorage.removeItem(SESSION_KEY); return null; }
    }
    function clearSession() { localStorage.removeItem(SESSION_KEY); state.session = null; }
    function resetApplicationState() {
        state.catalog = null; state.inventory = null; state.inventoryItems = new Map(); state.inventoryPage = 0;
        state.recommendations = null; state.notifications = []; state.notificationFilter = "all"; updateUnreadBadges();
    }
})();
