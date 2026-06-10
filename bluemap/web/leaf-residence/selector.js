(function () {
  const API_BASE = `${window.location.protocol}//${window.location.hostname}:8124`;
  const TOKEN_KEY = "leaf_resweb_token";
  const tokenFromUrl = new URLSearchParams(window.location.search).get("reswebToken");

  if (tokenFromUrl) {
    localStorage.setItem(TOKEN_KEY, tokenFromUrl);
    window.history.replaceState(null, "", window.location.pathname + window.location.hash);
  }

  const state = {
    token: localStorage.getItem(TOKEN_KEY) || "",
    data: null,
    selecting: false,
    first: null,
    second: null,
    name: localStorage.getItem("leaf_resweb_name") || "home",
    markerSet: null,
    marker: null,
    message: ""
  };

  const style = document.createElement("style");
  style.textContent = `
    .lrw-panel {
      position: fixed;
      top: 14px;
      left: 14px;
      z-index: 20000;
      width: 310px;
      border: 1px solid rgba(16, 24, 20, 0.18);
      border-radius: 8px;
      background: rgba(255, 250, 240, 0.94);
      color: #20241f;
      box-shadow: 0 16px 42px rgba(0, 0, 0, 0.22);
      font-family: ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
      overflow: hidden;
    }
    .lrw-head {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 8px;
      padding: 10px 11px;
      border-bottom: 1px solid rgba(16, 24, 20, 0.14);
      background: rgba(255, 247, 231, 0.96);
      font-weight: 700;
    }
    .lrw-body {
      display: grid;
      gap: 9px;
      padding: 11px;
      font-size: 13px;
      line-height: 1.45;
    }
    .lrw-row {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 8px;
    }
    .lrw-panel input {
      width: 100%;
      min-height: 34px;
      border: 1px solid #d6c9b3;
      border-radius: 6px;
      background: #fffdf7;
      color: #20241f;
      padding: 0 9px;
      font: inherit;
      outline: none;
    }
    .lrw-panel button {
      min-height: 34px;
      border: 1px solid #1f5d39;
      border-radius: 6px;
      background: #2f7d4a;
      color: #fff;
      padding: 0 10px;
      font: inherit;
      cursor: pointer;
    }
    .lrw-panel button.secondary {
      border-color: #d6c9b3;
      background: #fffaf0;
      color: #20241f;
    }
    .lrw-panel button:disabled {
      cursor: not-allowed;
      opacity: 0.55;
    }
    .lrw-muted {
      color: #6d705f;
    }
    .lrw-msg {
      border: 1px solid #d6c9b3;
      border-radius: 6px;
      background: rgba(255, 253, 247, 0.86);
      padding: 8px;
      color: #6d705f;
    }
    .lrw-msg.ok {
      border-color: rgba(47, 125, 74, 0.45);
      color: #1f5d39;
      background: rgba(47, 125, 74, 0.1);
    }
    .lrw-msg.bad {
      border-color: rgba(180, 67, 53, 0.45);
      color: #8d3026;
      background: rgba(180, 67, 53, 0.1);
    }
    .lrw-small {
      font-size: 12px;
    }
  `;
  document.head.appendChild(style);

  const panel = document.createElement("div");
  panel.className = "lrw-panel";
  document.body.appendChild(panel);

  function params(data) {
    return new URLSearchParams(data).toString();
  }

  async function request(path, options = {}) {
    const response = await fetch(`${API_BASE}${path}`, options);
    const payload = await response.json();
    if (!response.ok || payload.ok === false) {
      throw new Error(payload.error || "请求失败");
    }
    return payload;
  }

  function pointFromEvent(event) {
    let hit = event.detail && event.detail.hiresHit;
    if (!hit && event.detail && Array.isArray(event.detail.lowresHits)) {
      hit = event.detail.lowresHits.find(Boolean);
    }
    if (!hit) return null;
    const point = hit.pointOnLine || hit.point;
    if (!point) return null;
    return {
      x: Math.floor(point.x),
      y: Math.floor(point.y || 64),
      z: Math.floor(point.z)
    };
  }

  function selection() {
    if (!state.first || !state.second) return null;
    return {
      minX: Math.min(state.first.x, state.second.x),
      maxX: Math.max(state.first.x, state.second.x),
      minZ: Math.min(state.first.z, state.second.z),
      maxZ: Math.max(state.first.z, state.second.z)
    };
  }

  function selectionSize() {
    const selected = selection();
    if (!selected) return null;
    return {
      width: selected.maxX - selected.minX + 1,
      depth: selected.maxZ - selected.minZ + 1
    };
  }

  function ensureMarker() {
    if (!window.bluemap || !window.BlueMap || !window.bluemap.mapViewer) return null;
    if (!state.markerSet) {
      state.markerSet = new window.BlueMap.MarkerSet("leaf-residence-web");
      state.markerSet.data.label = "领地选区";
      state.markerSet.data.toggleable = false;
      window.bluemap.mapViewer.markers.add(state.markerSet);
    }
    if (!state.marker) {
      state.marker = new window.BlueMap.ShapeMarker("leaf-residence-preview");
      state.markerSet.add(state.marker);
    }
    return state.marker;
  }

  function updatePreview() {
    const selected = selection();
    if (!selected) return;
    const marker = ensureMarker();
    if (!marker) return;
    const y = Math.max(state.first.y || 64, state.second.y || 64) + 1;
    marker.updateFromData({
      type: "shape",
      label: "待创建领地",
      listed: false,
      position: { x: selected.minX, y, z: selected.minZ },
      shapeY: y,
      shape: [
        { x: selected.minX, z: selected.minZ },
        { x: selected.maxX + 1, z: selected.minZ },
        { x: selected.maxX + 1, z: selected.maxZ + 1 },
        { x: selected.minX, z: selected.maxZ + 1 }
      ],
      lineColor: { r: 40, g: 111, b: 145, a: 1 },
      fillColor: { r: 40, g: 111, b: 145, a: 0.24 },
      lineWidth: 4,
      depthTest: false
    });
    window.bluemap.mapViewer.redraw();
  }

  function validate() {
    if (!state.data) return "请先绑定";
    const selected = selection();
    if (!selected) return "点两个对角";
    const name = state.name.trim();
    if (!/^[A-Za-z0-9_-]{3,32}$/.test(name)) return "名字 3-32 位英文/数字/_/-";
    const limits = state.data.limits;
    const size = selectionSize();
    if (size.width < limits.minSize || size.depth < limits.minSize) return `最小 ${limits.minSize}x${limits.minSize}`;
    if (size.width > limits.maxSize || size.depth > limits.maxSize) return `最大 ${limits.maxSize}x${limits.maxSize}`;
    if (state.data.player.residenceCount >= limits.maxResidences) return "领地数量已满";
    const centerX = (selected.minX + selected.maxX + 1) / 2;
    const centerZ = (selected.minZ + selected.maxZ + 1) / 2;
    const distance = Math.round(Math.hypot(centerX - state.data.player.x, centerZ - state.data.player.z));
    if (limits.maxSubmitDistance > 0 && distance > limits.maxSubmitDistance) return `离玩家超过 ${limits.maxSubmitDistance} 格`;
    const overlap = state.data.claims.find((claim) => claim.areas.some((area) => (
      area.world === state.data.player.world &&
      selected.minX <= area.maxX &&
      selected.maxX >= area.minX &&
      selected.minZ <= area.maxZ &&
      selected.maxZ >= area.minZ
    )));
    if (overlap) return `和 ${overlap.name} 重叠`;
    return "";
  }

  function render() {
    const selected = selection();
    const size = selectionSize();
    const error = validate();
    const player = state.data && state.data.player;
    const limits = state.data && state.data.limits;

    panel.innerHTML = `
      <div class="lrw-head">
        <span>领地</span>
        ${player ? `<span class="lrw-muted lrw-small">${escapeHtml(player.name)}</span>` : ""}
      </div>
      <div class="lrw-body">
        ${state.token ? `
          <input id="lrw-name" maxlength="32" value="${escapeHtml(state.name)}" placeholder="领地名">
          <div class="lrw-row">
            <button id="lrw-select" type="button">${state.selecting ? "选区中..." : "点两个对角"}</button>
            <button id="lrw-submit" type="button" ${error ? "disabled" : ""}>提交</button>
          </div>
          <div class="lrw-msg ${error ? "bad" : selected ? "ok" : ""}">
            ${selected ? `${selected.minX},${selected.minZ} 到 ${selected.maxX},${selected.maxZ}<br>${size.width}x${size.depth}` : "在地图上点两个对角。"}
            ${error ? `<br>${escapeHtml(error)}` : ""}
          </div>
          <div class="lrw-row">
            <button class="secondary" id="lrw-refresh" type="button">刷新</button>
            <button class="secondary" id="lrw-logout" type="button">退出</button>
          </div>
          ${limits ? `<div class="lrw-muted lrw-small">${player.residenceCount}/${limits.maxResidences} 个，最大 ${limits.maxSize}x${limits.maxSize}</div>` : ""}
        ` : `
          <input id="lrw-code" inputmode="numeric" placeholder="绑定码">
          <button id="lrw-bind" type="button">绑定</button>
          <div class="lrw-msg">游戏内输入 /resweb link。</div>
        `}
        ${state.message ? `<div class="lrw-msg ${state.message.includes("已提交") ? "ok" : ""}">${escapeHtml(state.message)}</div>` : ""}
      </div>
    `;

    document.getElementById("lrw-bind")?.addEventListener("click", bind);
    document.getElementById("lrw-select")?.addEventListener("click", startSelection);
    document.getElementById("lrw-submit")?.addEventListener("click", submit);
    document.getElementById("lrw-refresh")?.addEventListener("click", refresh);
    document.getElementById("lrw-logout")?.addEventListener("click", logout);
    document.getElementById("lrw-name")?.addEventListener("input", (event) => {
      state.name = event.target.value.trim();
      localStorage.setItem("leaf_resweb_name", state.name);
      render();
    });
  }

  function escapeHtml(text) {
    return String(text).replace(/[&<>"']/g, (char) => ({
      "&": "&amp;",
      "<": "&lt;",
      ">": "&gt;",
      '"': "&quot;",
      "'": "&#039;"
    }[char]));
  }

  async function bind() {
    const code = document.getElementById("lrw-code").value.trim();
    try {
      const payload = await request("/api/link", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: params({ code })
      });
      state.token = payload.token;
      state.data = null;
      localStorage.setItem(TOKEN_KEY, state.token);
      state.message = "";
      await refresh();
    } catch (error) {
      state.message = error.message;
      render();
    }
  }

  async function refresh() {
    if (!state.token) return render();
    try {
      state.data = await request(`/api/state?token=${encodeURIComponent(state.token)}`);
      state.message = "";
      render();
      updatePreview();
    } catch (error) {
      localStorage.removeItem(TOKEN_KEY);
      state.token = "";
      state.data = null;
      state.message = error.message;
      render();
    }
  }

  function startSelection() {
    state.selecting = true;
    state.first = null;
    state.second = null;
    state.message = "点第一个角";
    render();
  }

  function logout() {
    localStorage.removeItem(TOKEN_KEY);
    state.token = "";
    state.data = null;
    state.first = null;
    state.second = null;
    state.message = "";
    render();
  }

  async function submit() {
    const selected = selection();
    if (!state.data || !selected) return;
    const name = state.name.trim();
    try {
      const payload = await request("/api/submit", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: params({
          token: state.token,
          name,
          world: state.data.player.world,
          minX: selected.minX,
          maxX: selected.maxX,
          minZ: selected.minZ,
          maxZ: selected.maxZ
        })
      });
      state.message = payload.message || "已提交，回游戏 /resweb confirm";
      await refresh();
    } catch (error) {
      state.message = error.message;
      render();
    }
  }

  function onMapInteraction(event) {
    if (!state.selecting) return;
    const point = pointFromEvent(event);
    if (!point) return;
    if (!state.first) {
      state.first = point;
      state.message = "点第二个角";
    } else {
      state.second = point;
      state.selecting = false;
      state.message = "";
      updatePreview();
    }
    render();
  }

  function attach() {
    if (!window.bluemap || !window.bluemap.events) {
      window.setTimeout(attach, 300);
      return;
    }
    window.bluemap.events.addEventListener("bluemapMapInteraction", onMapInteraction);
    refresh();
  }

  render();
  attach();
})();
