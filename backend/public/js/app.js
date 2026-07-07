document.addEventListener('DOMContentLoaded', () => {
    let allPlayers = [];

    const playersGrid = document.getElementById('playersGrid');
    const searchInput = document.getElementById('searchInput');
    const modal = document.getElementById('playerModal');
    const closeModalBtn = document.getElementById('closeModal');
    const tabBtns = document.querySelectorAll('.tab-btn');
    const clearDataBtn = document.getElementById('clearDataBtn');
    const liveTrackingToggle = document.getElementById('liveTrackingToggle');
    const trackingStatusText = document.getElementById('trackingStatusText');
    let currentPlayerUuid = null;

    // ══════════════════════════ SANITIZE (XSS protection) ══════════════════════════
    function escapeHtml(str) {
        if (!str) return '';
        const div = document.createElement('div');
        div.textContent = str;
        return div.innerHTML;
    }

    // ══════════════════════════ TIME HELPERS ══════════════════════════
    function timeAgo(timestamp) {
        if (!timestamp) return 'Never';
        const now = new Date();
        const date = new Date(timestamp);
        const diffMs = now - date;
        const seconds = Math.floor(diffMs / 1000);
        const minutes = Math.floor(seconds / 60);
        const hours = Math.floor(minutes / 60);
        const days = Math.floor(hours / 24);

        if (seconds < 60) return 'Vừa xong';
        if (minutes < 60) return `${minutes} phút trước`;
        if (hours < 24) return `${hours} giờ trước`;
        if (days < 7) return `${days} ngày trước`;
        return date.toLocaleDateString('vi-VN');
    }

    function isRecentlyOnline(timestamp) {
        if (!timestamp) return false;
        return (new Date() - new Date(timestamp)) < 6 * 60 * 1000;
    }

    // ══════════════════════════ FETCH & RENDER PLAYERS ══════════════════════════
    async function fetchPlayers() {
        try {
            const res = await fetch('/api/dashboard/players');
            const data = await res.json();
            if (data.success) {
                allPlayers = data.data;
                renderPlayers(allPlayers);
            }
        } catch (error) {
            console.error('Failed to load players', error);
            playersGrid.innerHTML = '<div class="error">⚠️ Failed to load players.</div>';
        }
    }

    function renderPlayers(players) {
        if (players.length === 0) {
            playersGrid.innerHTML = '<div class="empty-state"><span class="icon">👻</span>No players found.</div>';
            return;
        }

        playersGrid.innerHTML = players.map(p => {
            const online = isRecentlyOnline(p.last_online);
            const statusClass = online ? 'online' : '';
            const statusText = online ? 'Online' : timeAgo(p.last_online);
            const safeIgn = escapeHtml(p.ign);

            return `
                <div class="player-card" data-uuid="${escapeHtml(p.player_uuid)}">
                    <img src="https://mc-heads.net/avatar/${encodeURIComponent(p.ign)}/64"
                         alt="${safeIgn}" loading="lazy"
                         onerror="this.src='https://mc-heads.net/avatar/MHF_Steve/64'">
                    <h3>${safeIgn}</h3>
                    <p class="status ${statusClass}">${statusText}</p>
                </div>
            `;
        }).join('');

        document.querySelectorAll('.player-card').forEach(card => {
            card.addEventListener('click', () => openPlayerModal(card.dataset.uuid));
        });
    }

    // ══════════════════════════ SEARCH ══════════════════════════
    searchInput.addEventListener('input', (e) => {
        const term = e.target.value.toLowerCase().trim();
        const filtered = allPlayers.filter(p => p.ign.toLowerCase().includes(term));
        renderPlayers(filtered);
    });

    // ══════════════════════════ MODAL ══════════════════════════
    async function openPlayerModal(uuid) {
        try {
            const res = await fetch(`/api/dashboard/player/${encodeURIComponent(uuid)}`);
            const data = await res.json();

            if (data.success) {
                currentPlayerUuid = uuid;
                populateModal(data.data);
                modal.classList.add('active');
                document.body.style.overflow = 'hidden';
            }
        } catch (error) {
            console.error('Failed to fetch player details', error);
        }
    }

    function closeModal() {
        modal.classList.remove('active');
        document.body.style.overflow = '';
    }

    function populateModal(data) {
        const { player, events, waypoints, heartbeats } = data;
        const safeIgn = escapeHtml(player.ign);

        // Header
        document.getElementById('modalIgn').textContent = player.ign;
        document.getElementById('modalAvatar').src = `https://mc-heads.net/avatar/${encodeURIComponent(player.ign)}/100`;
        document.getElementById('modalServer').textContent = player.server_ip || 'Unknown Server';
        
        // Live Tracking Toggle
        liveTrackingToggle.checked = player.is_live_tracking === true;
        trackingStatusText.textContent = player.is_live_tracking ? "Live Tracking: ON" : "Live Tracking: OFF";
        trackingStatusText.style.color = player.is_live_tracking ? "#4caf50" : "#888";

        // ── Tab 1: Latest Position ──
        const latestTab = document.getElementById('tab-latest');
        if (heartbeats.length > 0) {
            const hb = heartbeats[0];
            latestTab.innerHTML = `
                <div class="position-card">
                    <h3>📍 Last Known Location</h3>
                    <div class="position-grid">
                        <div class="position-item">
                            <div class="label">X</div>
                            <div class="value">${Math.round(hb.x)}</div>
                        </div>
                        <div class="position-item">
                            <div class="label">Y</div>
                            <div class="value">${Math.round(hb.y)}</div>
                        </div>
                        <div class="position-item">
                            <div class="label">Z</div>
                            <div class="value">${Math.round(hb.z)}</div>
                        </div>
                    </div>
                    <div class="position-meta">
                        <span>🌍 ${escapeHtml((hb.dimension || '').replace('minecraft:', ''))}</span>
                        <span>🕐 ${timeAgo(hb.created_at)}</span>
                    </div>
                </div>
            `;
        } else {
            latestTab.innerHTML = '<div class="empty-state"><span class="icon">📡</span>No location data yet.</div>';
        }

        // ── Tab 2: Blocks ──
        const blocks = data.events; // All events now
        document.getElementById('tab-blocks').innerHTML = renderEventTable(blocks, 'events');

        // ── Tab 3: Proximity Logs ──
        const proximityLogs = data.proximity_logs || [];
        document.getElementById('tab-proximity').innerHTML = renderProximityTable(proximityLogs, 'proximity_logs');

        // Reset to first tab
        tabBtns.forEach(b => b.classList.remove('active'));
        document.querySelectorAll('.tab-pane').forEach(p => p.classList.remove('active'));
        tabBtns[0].classList.add('active');
        document.getElementById('tab-latest').classList.add('active');
    }

    // ══════════════════════════ TABLE RENDERERS ══════════════════════════
    function renderEventTable(items, tableType) {
        if (!items || items.length === 0) {
            return `<div class="empty-state"><span class="icon">📦</span>No block events yet.</div>`;
        }

        let html = '<table class="data-table"><thead><tr>';
        html += '<th>Block/Event</th><th>Coordinates</th><th>Dimension</th><th>Time</th><th>Action</th></tr></thead><tbody>';

        items.forEach(item => {
            html += '<tr>';
            const badge = `<span class="event-badge block">📦 ${escapeHtml(item.metadata || item.event_type)}</span>`;
            html += `<td>${badge}</td>`;
            html += `<td><span class="coord">${Math.round(item.x)}, ${Math.round(item.y)}, ${Math.round(item.z)}</span></td>`;
            html += `<td>${escapeHtml((item.dimension || '').replace('minecraft:', ''))}</td>`;
            html += `<td><span class="time-ago">${timeAgo(item.created_at)}</span></td>`;
            html += `<td><button class="delete-item-btn" onclick="deleteItem('${tableType}', ${item.id})">❌</button></td>`;
            html += '</tr>';
        });

        html += '</tbody></table>';
        return html;
    }

    function renderProximityTable(items, tableType) {
        if (!items || items.length === 0) {
            return '<div class="empty-state"><span class="icon">👥</span>No nearby players recorded.</div>';
        }

        let html = '<table class="data-table"><thead><tr><th>Event</th><th>Nearby Players</th><th>Closest</th><th>Coordinates</th><th>Time</th><th>Action</th></tr></thead><tbody>';

        items.forEach(item => {
            html += `<tr>`;
            html += `<td><strong>${item.event_type === 'NEARBY_CHANGED' ? '🔴 Phát hiện mới' : '🟢 Báo cáo 10p'}</strong></td>`;
            
            let playersList = '';
            try {
                const parsed = JSON.parse(item.nearby_players);
                playersList = Array.isArray(parsed) ? parsed.join(', ') : item.nearby_players;
            } catch (e) {
                playersList = item.nearby_players;
            }
            
            html += `<td>${escapeHtml(playersList)}</td>`;
            html += `<td>${escapeHtml(item.closest_player || 'N/A')}</td>`;
            html += `<td><span class="coord">${Math.round(item.x)}, ${Math.round(item.y)}, ${Math.round(item.z)}</span></td>`;
            html += `<td><span class="time-ago">${timeAgo(item.created_at)}</span></td>`;
            html += `<td><button class="delete-item-btn" onclick="deleteItem('${tableType}', ${item.id})">❌</button></td>`;
            html += `</tr>`;
        });

        html += '</tbody></table>';
        return html;
    }

    window.deleteItem = async function(tableType, id) {
        if (!confirm('Bạn có chắc muốn xóa mục này không?')) return;
        try {
            const res = await fetch(`/api/dashboard/item/${tableType}/${id}`, { method: 'DELETE' });
            const data = await res.json();
            if (data.success) {
                // Refresh current modal data
                if (currentPlayerUuid) {
                    openPlayerModal(currentPlayerUuid);
                }
            } else {
                alert('Lỗi: ' + data.message);
            }
        } catch (error) {
            console.error('Lỗi khi xóa:', error);
            alert('Có lỗi xảy ra khi xóa!');
        }
    };

    // ══════════════════════════ MODAL EVENTS ══════════════════════════
    closeModalBtn.addEventListener('click', closeModal);
    modal.addEventListener('click', (e) => { if (e.target === modal) closeModal(); });
    document.addEventListener('keydown', (e) => { if (e.key === 'Escape') closeModal(); });

    clearDataBtn.addEventListener('click', async () => {
        if (!currentPlayerUuid) return;
        
        const ign = document.getElementById('modalIgn').textContent;
        if (!confirm(`Bạn có chắc muốn xóa TOÀN BỘ dữ liệu của người chơi ${ign} không?\nHành động này không thể hoàn tác!`)) {
            return;
        }

        try {
            const res = await fetch(`/api/dashboard/player/${encodeURIComponent(currentPlayerUuid)}`, {
                method: 'DELETE'
            });
            const data = await res.json();
            if (data.success) {
                alert('Đã xóa dữ liệu thành công!');
                closeModal();
                fetchPlayers(); // Refresh list
            } else {
                alert('Lỗi: ' + data.message);
            }
        } catch (error) {
            console.error('Error clearing data', error);
            alert('Có lỗi xảy ra khi xóa dữ liệu!');
        }
    });

    liveTrackingToggle.addEventListener('change', async (e) => {
        if (!currentPlayerUuid) return;
        const enabled = e.target.checked;
        
        trackingStatusText.textContent = enabled ? "Live Tracking: ON" : "Live Tracking: OFF";
        trackingStatusText.style.color = enabled ? "#4caf50" : "#888";

        try {
            const res = await fetch(`/api/dashboard/player/${encodeURIComponent(currentPlayerUuid)}/toggle-tracking`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ enabled })
            });
            const data = await res.json();
            if (!data.success) {
                alert('Lỗi: ' + data.message);
                e.target.checked = !enabled; // revert
            }
        } catch (err) {
            console.error(err);
            alert('Có lỗi xảy ra khi bật tắt live tracking!');
            e.target.checked = !enabled; // revert
        }
    });

    tabBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            tabBtns.forEach(b => b.classList.remove('active'));
            document.querySelectorAll('.tab-pane').forEach(p => p.classList.remove('active'));
            btn.classList.add('active');
            document.getElementById(`tab-${btn.dataset.tab}`).classList.add('active');
        });
    });

    // ══════════════════════════ INIT ══════════════════════════
    fetchPlayers();
    setInterval(fetchPlayers, 30000);
});
