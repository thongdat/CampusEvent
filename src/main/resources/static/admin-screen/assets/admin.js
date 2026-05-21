const Admin = (() => {
    const API_BASE = window.location.protocol === 'file:'
        ? 'http://localhost:8081/api/admin'
        : '/api/admin';
    let logoutInProgress = false;

    const navItems = [
        { group: 'Dashboard', id: 'overview', label: 'Dashboard', icon: 'layout-dashboard', href: 'overview.html' },
        { group: 'Dashboard', id: 'reports', label: 'Reports', icon: 'bar-chart-3', href: 'reports.html' },
        { group: 'People', id: 'users', label: 'Users', icon: 'users', href: 'users.html' },
        { group: 'People', id: 'departments', label: 'Departments', icon: 'building-2', href: 'departments.html' },
        { group: 'Events', id: 'proposals', label: 'Proposals', icon: 'clipboard-list', href: 'proposals.html' },
        { group: 'Events', id: 'events', label: 'Events', icon: 'calendar-days', href: 'events.html' },
        { group: 'Events', id: 'registrations', label: 'Registrations', icon: 'ticket-check', href: 'registrations.html' },
        { group: 'Events', id: 'feedback', label: 'Feedback', icon: 'message-square-heart', href: 'feedback.html' },
        { group: 'System', id: 'security', label: 'System', icon: 'settings-2', href: 'security.html' },
        { group: 'System', id: 'logs', label: 'Activity Logs', icon: 'history', href: 'logs.html', secondary: true },
        { group: 'System', id: 'roles', label: 'Roles', icon: 'shield-check', href: 'roles.html', secondary: true },
        { group: 'Events', id: 'committees', label: 'Committees', icon: 'users-round', href: 'committees.html', secondary: true },
        { group: 'System', id: 'email', label: 'Email', icon: 'mail-check', href: 'email.html', secondary: true },
        { group: 'System', id: 'deployment', label: 'Deployment', icon: 'server-cog', href: 'deployment.html', secondary: true }
    ];

    const pageMeta = {
        overview: ['Admin Dashboard', 'Tổng quan hệ thống, cảnh báo vận hành và các luồng đang cần xử lý.'],
        reports: ['Reports & Analytics', 'Dashboard báo cáo, event statistics, attendance reports và xuất file.'],
        logs: ['Activity Logs', 'Nhật ký hoạt động và dấu vết truy cập trong hệ thống.'],
        users: ['User Management', 'Danh sách tài khoản, chi tiết, tạo/sửa, khóa/mở khóa và reset mật khẩu.'],
        roles: ['Role & Permission Management', 'Role list, permission matrix và gán quyền cho user.'],
        departments: ['Department Management', 'Danh sách khoa, tạo/sửa/xóa và gán trưởng khoa.'],
        proposals: ['Event Proposal Management', 'Theo dõi proposal, phân committee, publish hoặc xóa proposal.'],
        events: ['Event Management', 'Danh sách event, chỉnh sửa, hủy/xóa, capacity và event nổi bật.'],
        committees: ['Committee Management', 'Committee list, assign/remove members và workflow duyệt.'],
        registrations: ['Registration Management', 'Danh sách đăng ký, waitlist và attendance tracking.'],
        feedback: ['Feedback Management', 'Danh sách feedback, rating analysis và xóa comment xấu.'],
        email: ['Notification & Email', 'Email templates, gửi thông báo, announcement và lịch sử email.'],
        security: ['Security & System', 'OAuth2, reCAPTCHA, SMTP, backup/restore, access logs và system settings.'],
        deployment: ['Docker / Deployment', 'Deployment monitor, server status, container status và error logs.']
    };

    const academicStructure = [
        { faculty: 'Công nghệ Thông tin', departments: ['Công nghệ Thông tin', 'Kỹ thuật phần mềm', 'An toàn thông tin', 'Trí tuệ nhân tạo', 'Data Science'] },
        { faculty: 'Kinh tế', departments: ['Kinh tế', 'Marketing', 'Quản trị kinh doanh', 'Tài chính Ngân hàng'] },
        { faculty: 'Thiết kế & Truyền thông', departments: ['Thiết kế Mỹ thuật số', 'Thiết kế Đồ họa', 'Truyền thông đa phương tiện'] },
        { faculty: 'Ngôn ngữ', departments: ['Ngôn ngữ Anh', 'Ngôn ngữ Nhật'] },
        { faculty: 'Du lịch - Khách sạn', departments: ['Du lịch - Khách sạn', 'Hospitality Management'] }
    ];

    const permissionRows = [
        'Dashboard', 'Users', 'Roles', 'Departments', 'Proposals', 'Events',
        'Committees', 'Registrations', 'Feedback', 'Reports', 'Email', 'System'
    ];

    const state = {
        page: document.body.dataset.page || 'overview',
        cache: {},
        filters: {}
    };

    function h(value) {
        return String(value ?? '').replace(/[&<>"']/g, char => ({
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            '"': '&quot;',
            "'": '&#039;'
        })[char]);
    }

    function number(value) {
        return Number(value || 0).toLocaleString('vi-VN');
    }

    function percent(value) {
        return `${Number(value || 0).toLocaleString('vi-VN', { maximumFractionDigits: 1 })}%`;
    }

    function money(value) {
        return Number(value || 0).toLocaleString('vi-VN', {
            style: 'currency',
            currency: 'VND',
            maximumFractionDigits: 0
        });
    }

    function dateTime(value) {
        if (!value) return 'N/A';
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) return h(value);
        return date.toLocaleString('vi-VN', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    }

    function dateOnly(value) {
        if (!value) return 'N/A';
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) return h(value);
        return date.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });
    }

    function compareEventsByTime(left, right) {
        const now = Date.now();
        const leftTime = new Date(left?.startTime || 0).getTime();
        const rightTime = new Date(right?.startTime || 0).getTime();
        const leftValid = Number.isFinite(leftTime) && leftTime > 0;
        const rightValid = Number.isFinite(rightTime) && rightTime > 0;
        const leftUpcoming = leftValid && leftTime >= now;
        const rightUpcoming = rightValid && rightTime >= now;
        if (leftUpcoming !== rightUpcoming) return leftUpcoming ? -1 : 1;
        if (!leftValid && !rightValid) return String(left?.title || '').localeCompare(String(right?.title || ''), 'vi');
        if (!leftValid) return 1;
        if (!rightValid) return -1;
        return leftUpcoming ? leftTime - rightTime : rightTime - leftTime;
    }

    function dateTimeInput(value, addHours = 0) {
        if (!value) return '';
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) return String(value).slice(0, 16);
        date.setHours(date.getHours() + addHours);
        const pad = part => String(part).padStart(2, '0');
        return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
    }

    function normalize(value) {
        return String(value || '').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '');
    }

    function facultyOfDepartment(name) {
        const key = normalize(name);
        if (['it department', 'information technology', 'cntt'].includes(key)) {
            return 'Công nghệ Thông tin';
        }
        if (['business department', 'economics', 'marketing department'].includes(key)) {
            return 'Kinh tế';
        }
        const group = academicStructure.find(item =>
            normalize(item.faculty) === key || item.departments.some(department => normalize(department) === key)
        );
        return group ? group.faculty : 'Khác';
    }

    function eventImageUrl(event) {
        if (event?.imageUrl) return event.imageUrl;
        const signal = normalize(`${event?.title || ''} ${event?.departmentName || ''}`);
        if (signal.includes('marketing') || signal.includes('kinh te') || signal.includes('business')) {
            return 'https://images.unsplash.com/photo-1556761175-b413da4baf72?auto=format&fit=crop&w=900&q=80';
        }
        if (signal.includes('security') || signal.includes('an toan') || signal.includes('ctf')) {
            return 'https://images.unsplash.com/photo-1550751827-4bd374c3f58b?auto=format&fit=crop&w=900&q=80';
        }
        if (signal.includes('ai') || signal.includes('tri tue') || signal.includes('data')) {
            return 'https://images.unsplash.com/photo-1555255707-c07966088b7b?auto=format&fit=crop&w=900&q=80';
        }
        if (signal.includes('ux') || signal.includes('design') || signal.includes('thiet ke')) {
            return 'https://images.unsplash.com/photo-1558655146-d09347e92766?auto=format&fit=crop&w=900&q=80';
        }
        return 'https://images.unsplash.com/photo-1517048676732-d65bc937f952?auto=format&fit=crop&w=900&q=80';
    }

    async function api(path, options = {}) {
        const response = await fetch(`${API_BASE}${path}`, {
            headers: {
                Accept: 'application/json',
                'Content-Type': 'application/json',
                ...(options.headers || {})
            },
            ...options
        });
        if (!response.ok) {
            let message = `HTTP ${response.status}`;
            try {
                const error = await response.json();
                message = error.message || error.error || message;
            } catch (ignore) {
                message = await response.text() || message;
            }
            throw new Error(message);
        }
        if (response.status === 204) return null;
        return response.json();
    }

    async function load(path, fallback) {
        try {
            return await api(path);
        } catch (error) {
            console.warn(`Admin API fallback for ${path}:`, error);
            return fallback;
        }
    }

    function icon(name, cls = 'h-4 w-4') {
        return `<i data-lucide="${name}" class="${cls}"></i>`;
    }

    function refreshIcons() {
        if (window.lucide) window.lucide.createIcons();
    }

    function localGet(key, fallback) {
        try {
            const value = window.localStorage.getItem(`aems-admin:${key}`);
            return value ? JSON.parse(value) : fallback;
        } catch (error) {
            return fallback;
        }
    }

    function localSet(key, value) {
        window.localStorage.setItem(`aems-admin:${key}`, JSON.stringify(value));
    }

    function toast(message) {
        const node = document.getElementById('toast');
        node.textContent = message;
        node.classList.add('show');
        window.clearTimeout(toast.timer);
        toast.timer = window.setTimeout(() => node.classList.remove('show'), 2800);
    }

    function badge(text, tone = 'gray') {
        return `<span class="badge ${tone}">${h(text || 'N/A')}</span>`;
    }

    function tone(value) {
        const status = String(value || '').toUpperCase();
        if (['ACTIVE', 'APPROVED', 'PUBLISHED', 'REGISTERED', 'ATTENDED', 'SENT', 'ONLINE', 'HEALTHY', 'RUNNING'].includes(status)) return 'green';
        if (['PENDING', 'REVIEW', 'REVISION', 'WAITLIST', 'QUEUED', 'BUILDING'].includes(status)) return 'amber';
        if (['LOCKED', 'REJECTED', 'CANCELLED', 'FAILED', 'ABSENT', 'ERROR', 'DOWN'].includes(status)) return 'rose';
        if (['ADMIN'].includes(status)) return 'gray';
        if (['DEPARTMENT', 'MANAGER'].includes(status)) return 'teal';
        if (['COMMITTEE'].includes(status)) return 'orange';
        if (['STUDENT'].includes(status)) return 'blue';
        return 'gray';
    }

    function currentUser() {
        let sessionUser = {};
        try {
            sessionUser = JSON.parse(sessionStorage.getItem('user') || '{}');
        } catch (error) {
            sessionUser = {};
        }
        const fullName = sessionUser.fullName || localStorage.getItem('userName') || 'Admin';
        const role = sessionUser.role || localStorage.getItem('userRole') || 'ADMIN';
        const email = sessionUser.email || localStorage.getItem('userEmail') || '';
        const initials = fullName
            .split(/\s+/)
            .filter(Boolean)
            .slice(-2)
            .map(part => part.charAt(0))
            .join('')
            .toUpperCase() || 'A';
        return { fullName, role, email, initials };
    }

    function accountMenu() {
        const user = currentUser();
        return `
            <div class="account-menu" id="accountMenu">
                <button class="account-trigger" type="button" id="accountTrigger" aria-expanded="false">
                    <span class="account-avatar">${h(user.initials)}</span>
                    <span class="account-copy">
                        <span class="account-name">${h(user.fullName)}</span>
                        <span class="account-role">${h(user.role)}</span>
                    </span>
                    ${icon('chevron-down', 'h-3.5 w-3.5')}
                </button>
                <div class="account-dropdown" id="accountDropdown">
                    <div class="account-summary">
                        <strong>${h(user.fullName)}</strong>
                        <span>${h(user.email || user.role)}</span>
                    </div>
                    <button class="account-action" type="button" id="changePasswordBtn">${icon('key-round')}Đổi mật khẩu</button>
                    <button class="account-action danger" type="button" id="accountLogout">${icon('log-out')}Đăng xuất</button>
                </div>
            </div>`;
    }

    function bindAccountMenu() {
        const menu = document.getElementById('accountMenu');
        const trigger = document.getElementById('accountTrigger');
        if (!menu || !trigger) return;
        trigger.addEventListener('click', event => {
            event.stopPropagation();
            const open = !menu.classList.contains('open');
            menu.classList.toggle('open', open);
            trigger.setAttribute('aria-expanded', String(open));
        });
        document.addEventListener('click', event => {
            if (!menu.contains(event.target)) {
                menu.classList.remove('open');
                trigger.setAttribute('aria-expanded', 'false');
            }
        });
        document.getElementById('changePasswordBtn')?.addEventListener('click', openPasswordHelp);
        document.getElementById('accountLogout')?.addEventListener('click', logout);
    }

    function openPasswordHelp() {
        const resetUrl = '../forgot-password.html';
        openDetail('Đổi mật khẩu', `
            <div class="mini-card">
                <strong>Tài khoản đang đăng nhập</strong>
                <span>${h(currentUser().fullName)} · ${h(currentUser().role)}</span>
            </div>
            <p class="panel-note">Để đổi mật khẩu an toàn, hệ thống sẽ đưa bạn sang luồng xác minh OTP rồi quay lại đăng nhập.</p>
        `, `<a class="btn primary" href="${resetUrl}">${icon('key-round')}Mở trang đổi mật khẩu</a>`);
    }

    function ensureLogoutNoticeStyles() {
        if (document.getElementById('logout-notice-styles')) return;
        const style = document.createElement('style');
        style.id = 'logout-notice-styles';
        style.textContent = `
            .logout-notice-layer {
                position: fixed;
                inset: 0;
                z-index: 9999;
                display: flex;
                align-items: center;
                justify-content: center;
                padding: 1rem;
                background: rgba(15, 23, 42, 0.32);
                opacity: 0;
                pointer-events: none;
                transition: opacity 0.18s ease;
            }
            .logout-notice-layer.is-visible {
                opacity: 1;
                pointer-events: auto;
            }
            .logout-notice-card {
                display: grid;
                justify-items: center;
                gap: 0.85rem;
                width: min(22rem, 100%);
                padding: 1.5rem;
                border-radius: 0.75rem;
                background: #ffffff;
                color: #0f172a;
                box-shadow: 0 24px 70px -24px rgba(15, 23, 42, 0.48);
                text-align: center;
                transform: translateY(0.35rem) scale(0.98);
                transition: transform 0.18s ease;
            }
            .logout-notice-layer.is-visible .logout-notice-card {
                transform: translateY(0) scale(1);
            }
            .logout-notice-icon {
                display: inline-flex;
                width: 3rem;
                height: 3rem;
                align-items: center;
                justify-content: center;
                border-radius: 999px;
                background: #fff7ed;
                color: #ea580c;
            }
            .logout-notice-text {
                margin: 0;
                font-size: 0.98rem;
                font-weight: 700;
                line-height: 1.5;
            }
        `;
        document.head.appendChild(style);
    }

    function showLogoutNotice(message) {
        ensureLogoutNoticeStyles();
        let notice = document.querySelector('[data-logout-notice]');
        if (!notice) {
            notice = document.createElement('div');
            notice.className = 'logout-notice-layer';
            notice.setAttribute('data-logout-notice', '');
            notice.innerHTML = `
                <div class="logout-notice-card" role="status" aria-live="polite">
                    <span class="logout-notice-icon" aria-hidden="true">${icon('log-out', 'h-6 w-6')}</span>
                    <p class="logout-notice-text"></p>
                </div>
            `;
            document.body.appendChild(notice);
        }
        const text = notice.querySelector('.logout-notice-text');
        if (text) text.textContent = message;
        notice.classList.add('is-visible');
    }

    function clearAuthSession() {
        localStorage.removeItem('authToken');
        localStorage.removeItem('userName');
        localStorage.removeItem('userRole');
        sessionStorage.clear();
    }

    function resolveLoginUrl() {
        if (window.location.protocol === 'file:') return '../login.html';
        return window.location.pathname.startsWith('/api/') ? '/api/login.html' : '/login.html';
    }

    function logout(event) {
        if (event) {
            event.preventDefault();
            event.stopPropagation();
        }
        if (logoutInProgress) return;
        logoutInProgress = true;

        showLogoutNotice('Đang đăng xuất...');
        clearAuthSession();

        const loginUrl = resolveLoginUrl();
        fetch('/api/logout', { method: 'POST' }).catch(() => null).finally(() => {
            showLogoutNotice('Đã đăng xuất. Đang chuyển về trang đăng nhập...');
            window.setTimeout(() => {
                window.location.href = loginUrl;
            }, 900);
        });
    }

    function renderNavList(items) {
        let currentGroup = '';
        return items.map(item => {
            const groupHeader = item.group !== currentGroup
                ? `<div class="nav-group">${h(item.group)}</div>`
                : '';
            currentGroup = item.group;
            return `${groupHeader}
                <a class="nav-link ${item.id === state.page ? 'active' : ''}" href="${item.href}">
                    ${icon(item.icon)}
                    <span>${h(item.label)}</span>
                </a>`;
        }).join('');
    }

    function shell(actions = '') {
        const [title, subtitle] = pageMeta[state.page] || pageMeta.overview;
        const primaryItems = navItems.filter(item => !item.secondary || item.id === state.page);
        const moreItems = navItems.filter(item => item.secondary && item.id !== state.page);
        const nav = renderNavList(primaryItems);
        const moreNav = moreItems.map(item => `
            <a class="nav-more-link" href="${item.href}">
                ${icon(item.icon, 'h-3.5 w-3.5')}
                <span>${h(item.label)}</span>
            </a>`).join('');
        const user = currentUser();

        document.getElementById('app').innerHTML = `
            <div class="app-shell">
                <aside class="sidebar">
                    <a class="brand" href="overview.html">
                        <span class="brand-mark">A</span>
                        <span>
                            <span class="brand-title">AEMS Admin</span>
                            <span class="brand-subtitle">Control console</span>
                        </span>
                    </a>
                    <nav class="nav">${nav}</nav>
                    <div class="sidebar-footer">
                        <details class="nav-more">
                            <summary>${icon('more-horizontal')}Khác</summary>
                            <div class="nav-more-list">${moreNav}</div>
                        </details>
                        <div class="sidebar-account">
                            <strong>${h(user.fullName)}</strong>
                            <span>${h(user.role)}${user.email ? ' · ' + h(user.email) : ''}</span>
                        </div>
                        <span class="sidebar-copy">SWP Event Management · ${new Date().getFullYear()}</span>
                    </div>
                </aside>
                <main class="main">
                    <header class="topbar">
                        <div>
                            <p class="page-kicker">Admin Console</p>
                            <h1 class="page-title">${h(title)}</h1>
                            <p class="page-subtitle">${h(subtitle)}</p>
                        </div>
                        <div class="topbar-actions">
                            <div class="toolbar">${actions}</div>
                            ${accountMenu()}
                        </div>
                    </header>
                    <section id="content" class="content">
                        <div class="empty">Đang tải dữ liệu...</div>
                    </section>
                </main>
            </div>
            <div id="modalRoot" class="modal-backdrop"></div>
            <div id="toast" class="toast"></div>
        `;
        bindAccountMenu();
        refreshIcons();
    }

    function content(html) {
        document.getElementById('content').innerHTML = html;
        refreshIcons();
    }

    function metric(label, value, hint = '') {
        return `
            <article class="metric">
                <p class="metric-label">${h(label)}</p>
                <p class="metric-value">${h(value)}</p>
                <p class="metric-hint">${h(hint)}</p>
            </article>`;
    }

    function table(headers, rows, emptyText = 'Không có dữ liệu.') {
        if (!rows.length) return `<div class="empty">${h(emptyText)}</div>`;
        return `
            <div class="table-panel">
                <div class="table-scroll">
                    <table>
                        <thead><tr>${headers.map(head => `<th>${h(head)}</th>`).join('')}</tr></thead>
                        <tbody>${rows.join('')}</tbody>
                    </table>
                </div>
            </div>`;
    }

    function searchBox(id, placeholder) {
        return `<label class="control">${icon('search')}<input id="${id}" type="search" placeholder="${h(placeholder)}"></label>`;
    }

    function selectBox(id, options) {
        return `<label class="control"><select id="${h(id)}">${options.map(option => `<option value="${h(option.value)}">${h(option.label)}</option>`).join('')}</select></label>`;
    }

    function chart(monthly = []) {
        const max = Math.max(...monthly.map(item => Number(item.events || 0)), 1);
        return `
            <div class="chart" style="grid-template-columns: repeat(${Math.max(monthly.length, 1)}, minmax(3.8rem, 1fr));">
                ${monthly.map(item => {
                    const height = Math.max(4, Math.round(Number(item.events || 0) / max * 100));
                    const current = item.currentMonth ? ' current' : '';
                    return `
                        <div class="bar-column">
                            <div class="bar-track"><div class="bar-value${current}" style="height:${height}%"></div></div>
                            <p class="bar-label">${h(item.label)}</p>
                            <p class="bar-sub">${number(item.events)} event</p>
                        </div>`;
                }).join('')}
            </div>`;
    }

    function openDetail(title, bodyHtml, actions = '') {
        const modal = document.getElementById('modalRoot');
        modal.innerHTML = `
            <div class="modal">
                <div class="modal-head">
                    <h2 class="modal-title">${h(title)}</h2>
                    <button class="icon-btn" type="button" data-close-modal aria-label="Đóng">${icon('x')}</button>
                </div>
                <div class="modal-body">${bodyHtml}</div>
                <div class="modal-actions">
                    ${actions}
                    <button class="btn" type="button" data-close-modal>Đóng</button>
                </div>
            </div>`;
        modal.classList.add('open');
        modal.querySelectorAll('[data-close-modal]').forEach(button => button.addEventListener('click', () => modal.classList.remove('open')));
        refreshIcons();
    }

    function openForm({ title, fields, values = {}, submitText = 'Lưu', small = false, onSubmit }) {
        const modal = document.getElementById('modalRoot');
        const fieldHtml = fields.map(field => {
            const value = values[field.name] ?? field.defaultValue ?? '';
            const full = field.full ? ' full' : '';
            if (field.type === 'textarea') {
                return `<div class="field${full}"><label>${h(field.label)}</label><textarea name="${h(field.name)}" ${field.required ? 'required' : ''}>${h(value)}</textarea></div>`;
            }
            if (field.type === 'select') {
                const options = (field.options || []).map(option => {
                    const selected = String(value) === String(option.value) ? 'selected' : '';
                    return `<option value="${h(option.value)}" ${selected}>${h(option.label)}</option>`;
                }).join('');
                return `<div class="field${full}"><label>${h(field.label)}</label><select name="${h(field.name)}" ${field.required ? 'required' : ''}>${options}</select></div>`;
            }
            return `<div class="field${full}"><label>${h(field.label)}</label><input name="${h(field.name)}" type="${h(field.type || 'text')}" value="${h(value)}" ${field.required ? 'required' : ''}></div>`;
        }).join('');

        modal.innerHTML = `
            <form class="modal ${small ? 'small' : ''}" id="modalForm">
                <div class="modal-head">
                    <h2 class="modal-title">${h(title)}</h2>
                    <button class="icon-btn" type="button" data-close-modal aria-label="Đóng">${icon('x')}</button>
                </div>
                <div class="form-grid">${fieldHtml}</div>
                <div class="modal-actions">
                    <button class="btn" type="button" data-close-modal>Hủy</button>
                    <button class="btn primary" type="submit">${icon('save')}${h(submitText)}</button>
                </div>
            </form>`;
        modal.classList.add('open');
        modal.querySelectorAll('[data-close-modal]').forEach(button => button.addEventListener('click', () => modal.classList.remove('open')));
        document.getElementById('modalForm').addEventListener('submit', async event => {
            event.preventDefault();
            const formData = new FormData(event.currentTarget);
            const payload = {};
            fields.forEach(field => {
                const raw = formData.get(field.name);
                if (field.type === 'number') payload[field.name] = raw === '' ? null : Number(raw);
                else payload[field.name] = raw;
            });
            try {
                await onSubmit(payload);
                modal.classList.remove('open');
            } catch (error) {
                toast(error.message || 'Không thể lưu dữ liệu.');
            }
        });
        refreshIcons();
    }

    function confirmAction(message, action) {
        if (!window.confirm(message)) return;
        action().catch(error => toast(error.message || 'Thao tác thất bại.'));
    }

    function detailGrid(entries) {
        return `
            <div class="triple-grid">
                ${entries.map(([label, value]) => `
                    <div class="mini-card">
                        <span>${h(label)}</span>
                        <strong>${h(value)}</strong>
                    </div>`).join('')}
            </div>`;
    }

    function moduleChecklist(items) {
        return `
            <div class="triple-grid">
                ${items.map(item => `
                    <div class="module-card">
                        <div class="panel-header">
                            <h3 class="panel-title">${h(item.title)}</h3>
                            ${badge(item.status || 'Ready', item.tone || 'green')}
                        </div>
                        <p class="panel-note">${h(item.copy)}</p>
                    </div>`).join('')}
            </div>`;
    }

    function summarizeStatuses(items, key = 'status') {
        return items.reduce((acc, item) => {
            const status = item[key] || 'N/A';
            acc[status] = (acc[status] || 0) + 1;
            return acc;
        }, {});
    }

    function bindFilters(id, items, render) {
        const input = document.getElementById(id);
        if (!input) return;
        input.addEventListener('input', event => {
            render(event.target.value, items);
            const restored = document.getElementById(id);
            if (restored) restored.value = event.target.value;
        });
    }

    function pagination(key, page, pages, total, visible) {
        if (pages <= 1) return `<span class="metric-hint">Hiển thị ${number(visible)} / ${number(total)} kết quả</span>`;
        return `
            <div class="pagination" data-pagination="${h(key)}">
                <button class="icon-btn" type="button" data-page-step="-1" ${page <= 1 ? 'disabled' : ''}>${icon('chevron-left')}</button>
                <span>Trang ${number(page)} / ${number(pages)} · ${number(visible)} / ${number(total)} kết quả</span>
                <button class="icon-btn" type="button" data-page-step="1" ${page >= pages ? 'disabled' : ''}>${icon('chevron-right')}</button>
            </div>`;
    }

    function exportCsv(filename, rows) {
        const csv = rows.map(row => row.map(value => `"${String(value ?? '').replace(/"/g, '""')}"`).join(',')).join('\n');
        const blob = new Blob([`\ufeff${csv}`], { type: 'text/csv;charset=utf-8;' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = filename;
        a.click();
        URL.revokeObjectURL(url);
    }

    async function renderOverview() {
        state.page = 'overview';
        shell(`<a class="btn primary" href="reports.html">${icon('bar-chart-3')}Mở báo cáo</a>`);
        const [overview, events, proposals, registrations, feedback] = await Promise.all([
            load('/overview', {}),
            load('/events', []),
            load('/proposals', []),
            load('/registrations', []),
            load('/feedback', [])
        ]);
        const stats = overview.stats || {};
        const reports = overview.reports || {};
        const logs = overview.activityLogs || [];
        const pending = proposals.filter(item => ['PENDING', 'REVISION'].includes(String(item.status).toUpperCase()));
        const upcoming = events.filter(item => new Date(item.startTime) >= new Date()).slice(0, 6);
        content(`
            <div class="metric-grid">
                ${metric('Tổng user', number(stats.totalUsers), `${number(stats.activeUsers)} active · ${number(stats.lockedUsers)} locked`)}
                ${metric('Events', number(stats.totalEvents), `${number(stats.todayEvents)} hôm nay · ${number(stats.upcomingEvents)} sắp tới`)}
                ${metric('Registrations', number(stats.totalRegistrations), `${number(stats.attendanceCount)} attendance`)}
                ${metric('Feedback', `${Number(stats.averageRating || reports.averageRating || 0).toLocaleString('vi-VN', { maximumFractionDigits: 1 })}/5`, `${number(feedback.length)} phản hồi`)}
            </div>
            <div class="split-grid">
                <section class="panel">
                    <div class="panel-header">
                        <div>
                            <h2 class="panel-title">Statistics Dashboard</h2>
                            <p class="panel-note">Event theo tháng, cập nhật tới ${dateOnly(overview.asOfDate)}</p>
                        </div>
                        <a class="btn" href="reports.html">${icon('external-link')}Chi tiết</a>
                    </div>
                    ${chart(reports.monthly || [])}
                </section>
                <section class="panel">
                    <h2 class="panel-title">Cần xử lý</h2>
                    <div class="content" style="padding:.85rem 0 0">
                        ${metric('Pending proposals', number(pending.length), 'Proposal chờ duyệt hoặc cần chỉnh sửa')}
                        ${metric('Waitlist', number(registrations.filter(item => item.status === 'WAITLIST').length), 'Sinh viên đang trong hàng chờ')}
                        ${metric('Email failed', number(stats.failedEmails), 'Lỗi gửi thông báo')}
                    </div>
                </section>
            </div>
            <div class="split-grid">
                <section class="panel">
                    <div class="panel-header">
                        <h2 class="panel-title">Upcoming Events</h2>
                        <a class="btn" href="events.html">${icon('calendar-days')}Quản lý event</a>
                    </div>
                    ${table(['Event', 'Khoa', 'Thời gian', 'Fill'], upcoming.map(event => `
                        <tr>
                            <td><span class="cell-title">${h(event.title)}</span><span class="cell-sub">${h(event.location || 'N/A')}</span></td>
                            <td>${h(event.departmentName)}</td>
                            <td>${dateTime(event.startTime)}</td>
                            <td><div class="progress"><span style="width:${Math.min(100, Number(event.fillRate || 0))}%"></span></div><span class="cell-sub">${percent(event.fillRate)}</span></td>
                        </tr>`), 'Chưa có event sắp tới.')}
                </section>
                <section class="panel">
                    <div class="panel-header">
                        <h2 class="panel-title">Activity Logs</h2>
                        <a class="btn" href="logs.html">${icon('history')}Xem logs</a>
                    </div>
                    ${renderTimeline(logs.slice(0, 8))}
                </section>
            </div>
            ${moduleChecklist([
                { title: 'User / Role / Department', status: 'CRUD', copy: 'Tạo, sửa, khóa, reset mật khẩu, gán role và quản lý khoa.' },
                { title: 'Proposal / Event / Registration', status: 'Workflow', tone: 'orange', copy: 'Theo dõi duyệt proposal, publish event, capacity, waitlist và attendance.' },
                { title: 'Feedback / Reports / Email', status: 'Analytics', tone: 'blue', copy: 'Phân tích rating, export báo cáo, template email và lịch sử gửi.' },
                { title: 'Security / Deployment', status: 'Ops', tone: 'teal', copy: 'Cấu hình OAuth2, captcha, SMTP, backup, server và container status.' }
            ])}
        `);
    }

    function renderTimeline(logs) {
        if (!logs.length) return '<div class="empty">Chưa có hoạt động.</div>';
        return `<div class="timeline">${logs.map(log => `
            <div class="timeline-item">
                <div class="timeline-time">${dateTime(log.createdAt)}</div>
                <div class="timeline-copy">
                    <strong>${h(log.activityType || 'Activity')}</strong>
                    <span>${h(log.description || log.userEmail || '')}</span>
                </div>
            </div>`).join('')}</div>`;
    }

    async function renderLogs() {
        state.page = 'logs';
        shell(`<button class="btn" id="exportLogs">${icon('download')}Export CSV</button>`);
        const payload = await load('/activity-logs?page=0&size=120', { items: [] });
        const logs = payload.items || [];
        const render = (q = '') => {
            const filtered = logs.filter(log => !q || normalize(`${log.activityType} ${log.description} ${log.userName} ${log.userEmail}`).includes(normalize(q)));
            content(`
                <div class="metric-grid">
                    ${metric('Total logs', number(payload.totalItems || logs.length), 'Activity logs')}
                    ${metric('Point earned', number(logs.reduce((sum, log) => sum + Number(log.pointsEarned || 0), 0)), 'Tổng điểm trong trang')}
                    ${metric('Users involved', number(new Set(logs.map(log => log.userEmail).filter(Boolean)).size), 'User có hoạt động')}
                    ${metric('Types', number(new Set(logs.map(log => log.activityType).filter(Boolean)).size), 'Loại hoạt động')}
                </div>
                <div class="toolbar">${searchBox('logSearch', 'Tìm activity, user, mô tả...')}<span class="metric-hint">${number(filtered.length)} log</span></div>
                ${table(['Thời gian', 'Loại', 'User', 'Mô tả', 'Điểm'], filtered.map(log => `
                    <tr>
                        <td>${dateTime(log.createdAt)}</td>
                        <td>${badge(log.activityType || 'N/A', 'blue')}</td>
                        <td><span class="cell-title">${h(log.userName || 'N/A')}</span><span class="cell-sub">${h(log.userEmail || '')}</span></td>
                        <td>${h(log.description || '')}</td>
                        <td>${number(log.pointsEarned)}</td>
                    </tr>`))}
            `);
            bindFilters('logSearch', logs, render);
            document.getElementById('exportLogs').onclick = () => exportCsv('activity-logs.csv', [
                ['Time', 'Type', 'User', 'Email', 'Description', 'Points'],
                ...logs.map(log => [log.createdAt, log.activityType, log.userName, log.userEmail, log.description, log.pointsEarned])
            ]);
        };
        render();
    }

    async function renderUsers() {
        state.page = 'users';
        shell(`<button class="btn primary" id="addUser">${icon('user-plus')}Create User</button>`);
        const [users, roles] = await Promise.all([load('/users', []), load('/roles', [])]);
        const roleOptions = roles.map(role => ({ value: role.id, label: role.name }));
        const statusOptions = [{ value: 'true', label: 'ACTIVE' }, { value: 'false', label: 'LOCKED' }];

        const openUserForm = (user = {}) => openForm({
            title: user.id ? 'Edit User' : 'Create User',
            fields: [
                { name: 'fullName', label: 'Họ tên', required: true },
                { name: 'email', label: 'Email', type: 'email', required: true },
                { name: 'phone', label: 'Số điện thoại', required: true },
                { name: 'roleId', label: 'Role', type: 'select', options: roleOptions, required: true },
                { name: 'active', label: 'Trạng thái', type: 'select', options: statusOptions },
                { name: 'password', label: user.id ? 'Mật khẩu mới' : 'Mật khẩu', type: 'password' },
                { name: 'major', label: 'Khoa/Major' },
                { name: 'semester', label: 'Kỳ/Năm', type: 'number' },
                { name: 'studentCode', label: 'Mã sinh viên' },
                { name: 'totalPoints', label: 'Điểm', type: 'number' }
            ],
            values: { ...user, active: user.active === false ? 'false' : 'true' },
            onSubmit: async payload => {
                payload.active = payload.active === 'true';
                if (!payload.password) delete payload.password;
                await api(user.id ? `/users/${user.id}` : '/users', {
                    method: user.id ? 'PUT' : 'POST',
                    body: JSON.stringify(payload)
                });
                toast('Đã lưu user.');
                renderUsers();
            }
        });

        const detail = user => openDetail('User Detail', `
            ${detailGrid([
                ['Họ tên', user.fullName],
                ['Email', user.email],
                ['Phone', user.phone],
                ['Role', user.role],
                ['Khoa/Major', user.major],
                ['Mã sinh viên', user.studentCode || 'N/A'],
                ['Trạng thái', user.status],
                ['Tổng điểm', number(user.totalPoints)],
                ['Ngày tạo', dateTime(user.createdAt)]
            ])}
        `);

        const toggleUser = async user => {
            const payload = { ...user, active: user.status === 'LOCKED' };
            await api(`/users/${user.id}`, { method: 'PUT', body: JSON.stringify(payload) });
            toast(payload.active ? 'Đã mở khóa user.' : 'Đã khóa user.');
            renderUsers();
        };

        const resetPassword = user => openForm({
            title: 'Reset Password',
            small: true,
            fields: [{ name: 'password', label: 'Mật khẩu mới', type: 'password', required: true }],
            onSubmit: async payload => {
                await api(`/users/${user.id}`, { method: 'PUT', body: JSON.stringify({ ...user, password: payload.password }) });
                toast('Đã reset mật khẩu.');
            }
        });

        const userFaculty = user => facultyOfDepartment(user.major || user.departmentName || '');
        const uniqueOptions = values => [...new Set(values.filter(Boolean))].sort((a, b) => a.localeCompare(b, 'vi'));
        const roleFilterOptions = [{ value: 'all', label: 'Tất cả role' }]
            .concat(uniqueOptions(users.map(user => user.role)).map(value => ({ value, label: value })));
        const facultyFilterOptions = [{ value: 'all', label: 'Tất cả khoa/bộ môn' }]
            .concat(uniqueOptions(users.map(user => userFaculty(user))).map(value => ({ value, label: value })));
        const sortOptions = [
            { value: 'role', label: 'Sort: Role' },
            { value: 'faculty', label: 'Sort: Khoa/Bộ môn' },
            { value: 'name', label: 'Sort: Tên A-Z' },
            { value: 'points', label: 'Sort: Điểm cao' },
            { value: 'created', label: 'Sort: Mới tạo' }
        ];

        const render = (q = state.filters.userSearch || '') => {
            state.filters.userSearch = q;
            const roleFilter = state.filters.userRole || 'all';
            const facultyFilter = state.filters.userFaculty || 'all';
            const sortKey = state.filters.userSort || 'role';
            const filtered = users
                .filter(user => !q || normalize(`${user.fullName} ${user.email} ${user.role} ${user.major}`).includes(normalize(q)))
                .filter(user => roleFilter === 'all' || String(user.role) === roleFilter)
                .filter(user => facultyFilter === 'all' || userFaculty(user) === facultyFilter)
                .sort((left, right) => {
                    if (sortKey === 'role') {
                        return String(left.role || '').localeCompare(String(right.role || ''), 'vi')
                            || userFaculty(left).localeCompare(userFaculty(right), 'vi')
                            || String(left.fullName || '').localeCompare(String(right.fullName || ''), 'vi');
                    }
                    if (sortKey === 'faculty') {
                        return userFaculty(left).localeCompare(userFaculty(right), 'vi')
                            || String(left.major || '').localeCompare(String(right.major || ''), 'vi')
                            || String(left.fullName || '').localeCompare(String(right.fullName || ''), 'vi');
                    }
                    if (sortKey === 'points') return Number(right.totalPoints || 0) - Number(left.totalPoints || 0);
                    if (sortKey === 'created') return new Date(right.createdAt || 0) - new Date(left.createdAt || 0);
                    return String(left.fullName || '').localeCompare(String(right.fullName || ''), 'vi');
                });
            content(`
                <div class="metric-grid">
                    ${metric('All Users', number(users.length), 'User List')}
                    ${metric('Active', number(users.filter(user => user.status === 'ACTIVE').length), 'Đang hoạt động')}
                    ${metric('Locked', number(users.filter(user => user.status === 'LOCKED').length), 'Tài khoản khóa')}
                    ${metric('Roles', number(roles.length), 'Role đang có')}
                </div>
                <div class="filter-strip">
                    ${searchBox('userSearch', 'Tìm tên, email, role, khoa...')}
                    ${selectBox('userRoleFilter', roleFilterOptions)}
                    ${selectBox('userFacultyFilter', facultyFilterOptions)}
                    ${selectBox('userSort', sortOptions)}
                    <span class="metric-hint">${number(filtered.length)} user</span>
                </div>
                ${table(['User', 'Role', 'Khoa', 'Status', 'Điểm', 'Actions'], filtered.map(user => `
                    <tr>
                        <td><span class="cell-title">${h(user.fullName)}</span><span class="cell-sub">${h(user.email)}</span></td>
                        <td>${badge(user.role || 'N/A', tone(user.role))}</td>
                        <td>${h(user.major || 'N/A')}<span class="cell-sub">${h(userFaculty(user))}${user.studentCode ? ' · ' + h(user.studentCode) : ''}</span></td>
                        <td>${badge(user.status, tone(user.status))}</td>
                        <td>${number(user.totalPoints)}</td>
                        <td><div class="row-actions">
                            <button class="icon-btn" data-detail="${user.id}" title="Detail">${icon('eye')}</button>
                            <button class="icon-btn" data-edit="${user.id}" title="Edit">${icon('pencil')}</button>
                            <button class="icon-btn" data-lock="${user.id}" title="Lock/Unlock">${icon(user.status === 'LOCKED' ? 'unlock' : 'lock')}</button>
                            <button class="icon-btn" data-reset="${user.id}" title="Reset Password">${icon('key-round')}</button>
                            <button class="icon-btn danger" data-delete="${user.id}" title="Delete">${icon('trash-2')}</button>
                        </div></td>
                    </tr>`))}
            `);
            const search = document.getElementById('userSearch');
            const roleSelect = document.getElementById('userRoleFilter');
            const facultySelect = document.getElementById('userFacultyFilter');
            const sortSelect = document.getElementById('userSort');
            if (search) search.value = q;
            if (roleSelect) roleSelect.value = roleFilter;
            if (facultySelect) facultySelect.value = facultyFilter;
            if (sortSelect) sortSelect.value = sortKey;
            if (search) search.addEventListener('input', event => render(event.target.value));
            if (roleSelect) roleSelect.addEventListener('change', event => {
                state.filters.userRole = event.target.value;
                render();
            });
            if (facultySelect) facultySelect.addEventListener('change', event => {
                state.filters.userFaculty = event.target.value;
                render();
            });
            if (sortSelect) sortSelect.addEventListener('change', event => {
                state.filters.userSort = event.target.value;
                render();
            });
            document.querySelectorAll('[data-detail]').forEach(button => button.onclick = () => detail(users.find(user => String(user.id) === button.dataset.detail)));
            document.querySelectorAll('[data-edit]').forEach(button => button.onclick = () => openUserForm(users.find(user => String(user.id) === button.dataset.edit)));
            document.querySelectorAll('[data-lock]').forEach(button => button.onclick = () => toggleUser(users.find(user => String(user.id) === button.dataset.lock)));
            document.querySelectorAll('[data-reset]').forEach(button => button.onclick = () => resetPassword(users.find(user => String(user.id) === button.dataset.reset)));
            document.querySelectorAll('[data-delete]').forEach(button => button.onclick = () => confirmAction('Xóa user này? Nếu có dữ liệu liên quan, hệ thống sẽ khóa thay vì xóa vật lý.', async () => {
                await api(`/users/${button.dataset.delete}`, { method: 'DELETE' });
                toast('Đã xử lý user.');
                renderUsers();
            }));
        };
        document.getElementById('addUser').onclick = () => openUserForm();
        render();
    }

    async function renderRoles() {
        state.page = 'roles';
        shell(`<button class="btn primary" id="addRole">${icon('plus')}Create Role</button><button class="btn" id="assignRole">${icon('user-cog')}Assign Role</button>`);
        const [roles, users] = await Promise.all([load('/roles', []), load('/users', [])]);
        const roleOptions = roles.map(role => ({ value: role.id, label: role.name }));

        const openRoleForm = (role = {}) => openForm({
            title: role.id ? 'Edit Role' : 'Create Role',
            fields: [
                { name: 'name', label: 'Tên role', required: true },
                { name: 'description', label: 'Mô tả', type: 'textarea', full: true }
            ],
            values: role,
            onSubmit: async payload => {
                await api(role.id ? `/roles/${role.id}` : '/roles', {
                    method: role.id ? 'PUT' : 'POST',
                    body: JSON.stringify(payload)
                });
                toast('Đã lưu role.');
                renderRoles();
            }
        });

        const assignRole = () => openForm({
            title: 'Assign Role',
            fields: [
                { name: 'userId', label: 'User', type: 'select', options: users.map(user => ({ value: user.id, label: `${user.fullName} - ${user.email}` })) },
                { name: 'roleId', label: 'Role', type: 'select', options: roleOptions }
            ],
            onSubmit: async payload => {
                const user = users.find(item => String(item.id) === String(payload.userId));
                await api(`/users/${user.id}`, { method: 'PUT', body: JSON.stringify({ ...user, roleId: payload.roleId }) });
                toast('Đã gán role.');
                renderRoles();
            }
        });

        const permissionMatrix = roles.map(role => `
            <tr>
                <td><span class="cell-title">${h(role.name)}</span><span class="cell-sub">${h(role.description || '')}</span></td>
                ${permissionRows.map(permission => {
                    const checked = role.name === 'ADMIN' || (role.name === 'DEPARTMENT' && ['Dashboard', 'Departments', 'Proposals', 'Events', 'Reports'].includes(permission))
                        || (role.name === 'COMMITTEE' && ['Dashboard', 'Proposals', 'Events', 'Registrations', 'Feedback'].includes(permission))
                        || (role.name === 'STUDENT' && ['Events', 'Registrations', 'Feedback'].includes(permission));
                    return `<td>${checked ? badge('Allow', 'green') : badge('No', 'gray')}</td>`;
                }).join('')}
            </tr>`).join('');

        content(`
            <div class="metric-grid">
                ${metric('Roles', number(roles.length), 'Role List')}
                ${metric('Assigned users', number(users.length), 'User đang có role')}
                ${metric('Admin users', number(users.filter(user => user.role === 'ADMIN').length), 'Quản trị viên')}
                ${metric('Permission modules', number(permissionRows.length), 'Module phân quyền')}
            </div>
            ${table(['Role', 'Description', 'Users', 'Actions'], roles.map(role => `
                <tr>
                    <td>${badge(role.name, tone(role.name))}</td>
                    <td>${h(role.description || 'N/A')}</td>
                    <td>${number(role.userCount)}</td>
                    <td><div class="row-actions">
                        <button class="icon-btn" data-edit-role="${role.id}">${icon('pencil')}</button>
                        <button class="icon-btn danger" data-delete-role="${role.id}">${icon('trash-2')}</button>
                    </div></td>
                </tr>`))}
            <section class="panel">
                <div class="panel-header">
                    <div>
                        <h2 class="panel-title">Permission Management</h2>
                        <p class="panel-note">Ma trận quyền theo module. Role ADMIN có toàn quyền.</p>
                    </div>
                </div>
                <div class="table-panel" style="margin-top:.85rem"><div class="table-scroll">
                    <table><thead><tr><th>Role</th>${permissionRows.map(item => `<th>${h(item)}</th>`).join('')}</tr></thead><tbody>${permissionMatrix}</tbody></table>
                </div></div>
            </section>
        `);
        document.getElementById('addRole').onclick = () => openRoleForm();
        document.getElementById('assignRole').onclick = assignRole;
        document.querySelectorAll('[data-edit-role]').forEach(button => button.onclick = () => openRoleForm(roles.find(role => String(role.id) === button.dataset.editRole)));
        document.querySelectorAll('[data-delete-role]').forEach(button => button.onclick = () => confirmAction('Xóa role này?', async () => {
            await api(`/roles/${button.dataset.deleteRole}`, { method: 'DELETE' });
            toast('Đã xóa role.');
            renderRoles();
        }));
    }

    async function renderDepartments() {
        state.page = 'departments';
        shell(`<button class="btn primary" id="addDepartment">${icon('plus')}Thêm bộ môn</button>`);
        const [departments, users] = await Promise.all([load('/departments', []), load('/users', [])]);
        const managers = localGet('departmentManagers', {});
        const grouped = departments.reduce((acc, department) => {
            const faculty = department.facultyName || facultyOfDepartment(department.name);
            if (!acc[faculty]) acc[faculty] = [];
            acc[faculty].push(department);
            return acc;
        }, {});
        const facultyRows = Object.entries(grouped).map(([faculty, units]) => ({
            faculty,
            units,
            events: units.reduce((sum, item) => sum + Number(item.eventCount || 0), 0),
            proposals: units.reduce((sum, item) => sum + Number(item.proposalCount || 0), 0),
            students: units.reduce((sum, item) => sum + Number(item.studentCount || 0), 0)
        }));

        const openDepartmentForm = (department = {}) => openForm({
            title: department.id ? 'Sửa bộ môn' : 'Thêm bộ môn',
            fields: [
                { name: 'facultyName', label: 'Khoa', type: 'select', options: academicStructure.map(item => ({ value: item.faculty, label: item.faculty })), defaultValue: facultyOfDepartment(department.name) },
                { name: 'name', label: 'Tên bộ môn/ngành', required: true },
                { name: 'description', label: 'Mô tả', type: 'textarea', full: true }
            ],
            values: { ...department, facultyName: department.facultyName || facultyOfDepartment(department.name) },
            onSubmit: async payload => {
                await api(department.id ? `/departments/${department.id}` : '/departments', {
                    method: department.id ? 'PUT' : 'POST',
                    body: JSON.stringify({
                        name: payload.name,
                        description: payload.description || `Thuộc khoa ${payload.facultyName}`
                    })
                });
                toast('Đã lưu bộ môn.');
                renderDepartments();
            }
        });

        const assignManager = department => openForm({
            title: 'Gán người phụ trách',
            fields: [{ name: 'managerId', label: 'Trưởng khoa', type: 'select', options: users.map(user => ({ value: user.id, label: `${user.fullName} - ${user.email}` })) }],
            values: { managerId: managers[department.id] || '' },
            onSubmit: async payload => {
                managers[department.id] = payload.managerId;
                localSet('departmentManagers', managers);
                toast('Đã gán trưởng khoa.');
                renderDepartments();
            }
        });

        content(`
            <div class="metric-grid">
                ${metric('Khoa lớn', number(facultyRows.length), 'Đã gom nhóm')}
                ${metric('Bộ môn/ngành', number(departments.length), 'Đơn vị quản lý')}
                ${metric('Events', number(departments.reduce((sum, item) => sum + Number(item.eventCount || 0), 0)), 'Event theo DB')}
                ${metric('Students', number(departments.reduce((sum, item) => sum + Number(item.studentCount || 0), 0)), 'Sinh viên ước tính')}
            </div>
            <div class="department-tree">
                ${facultyRows.map(group => `
                    <section class="panel department-group">
                        <div class="panel-header">
                            <div>
                                <h2 class="panel-title">${h(group.faculty)}</h2>
                                <p class="panel-note">${number(group.units.length)} bộ môn · ${number(group.events)} event · ${number(group.students)} sinh viên</p>
                            </div>
                            ${badge(`${number(group.proposals)} proposal`, group.proposals ? 'blue' : 'gray')}
                        </div>
                        <div class="table-panel department-table"><div class="table-scroll">
                            <table>
                                <thead><tr><th>Bộ môn/ngành</th><th>Manager</th><th>Events</th><th>Proposals</th><th>Students</th><th>Actions</th></tr></thead>
                                <tbody>
                                    ${group.units.map(department => {
                                        const manager = users.find(user => String(user.id) === String(managers[department.id]));
                                        return `<tr>
                                            <td><span class="cell-title">${h(department.name)}</span><span class="cell-sub">${h(department.description || `Thuộc khoa ${group.faculty}`)}</span></td>
                                            <td>${manager ? `${h(manager.fullName)}<span class="cell-sub">${h(manager.email)}</span>` : badge('Chưa gán', 'amber')}</td>
                                            <td>${number(department.eventCount)}</td>
                                            <td>${number(department.proposalCount)}</td>
                                            <td>${number(department.studentCount)}</td>
                                            <td><div class="row-actions">
                                                <button class="icon-btn" data-manager="${department.id}" title="Assign Manager">${icon('user-check')}</button>
                                                <button class="icon-btn" data-edit-department="${department.id}">${icon('pencil')}</button>
                                                <button class="icon-btn danger" data-delete-department="${department.id}">${icon('trash-2')}</button>
                                            </div></td>
                                        </tr>`;
                                    }).join('')}
                                </tbody>
                            </table>
                        </div></div>
                    </section>
                `).join('')}
            </div>
        `);
        document.getElementById('addDepartment').onclick = () => openDepartmentForm();
        document.querySelectorAll('[data-manager]').forEach(button => button.onclick = () => assignManager(departments.find(item => String(item.id) === button.dataset.manager)));
        document.querySelectorAll('[data-edit-department]').forEach(button => button.onclick = () => openDepartmentForm(departments.find(item => String(item.id) === button.dataset.editDepartment)));
        document.querySelectorAll('[data-delete-department]').forEach(button => button.onclick = () => confirmAction('Xóa khoa này?', async () => {
            await api(`/departments/${button.dataset.deleteDepartment}`, { method: 'DELETE' });
            toast('Đã xóa khoa.');
            renderDepartments();
        }));
    }

    async function renderProposals() {
        state.page = 'proposals';
        shell();
        const proposals = await load('/proposals', []);
        const committees = getCommittees();
        const committeeMap = localGet('proposalCommittees', {});
        const statusCounts = summarizeStatuses(proposals);
        const statusOptions = ['PENDING', 'APPROVED', 'REVISION', 'REJECTED'].map(value => ({ value, label: value }));
        const needsReview = proposals.filter(item => ['PENDING', 'REVISION'].includes(String(item.status).toUpperCase()));
        const readyToPublish = proposals.filter(item => String(item.status).toUpperCase() === 'APPROVED');

        const publish = proposal => openForm({
            title: 'Publish Proposal',
            fields: [
                { name: 'location', label: 'Địa điểm', required: true, defaultValue: 'FPT Campus' },
                { name: 'startTime', label: 'Bắt đầu', type: 'datetime-local', required: true },
                { name: 'endTime', label: 'Kết thúc', type: 'datetime-local', required: true },
                { name: 'capacity', label: 'Capacity', type: 'number', defaultValue: 100 },
                { name: 'budget', label: 'Ngân sách', type: 'number', defaultValue: 0 },
                { name: 'imageUrl', label: 'Ảnh sự kiện', type: 'url', full: true },
                { name: 'note', label: 'Ghi chú publish', type: 'textarea', full: true }
            ],
            values: {
                startTime: dateTimeInput(proposal.proposedDate),
                endTime: dateTimeInput(proposal.proposedDate, 2)
            },
            onSubmit: async payload => {
                const result = await api(`/proposals/${proposal.id}/publish`, { method: 'POST', body: JSON.stringify(payload) });
                if (result?.event?.id && committeeMap[proposal.id]) {
                    const eventCommitteeMap = localGet('eventCommittees', {});
                    eventCommitteeMap[result.event.id] = committeeMap[proposal.id];
                    localSet('eventCommittees', eventCommitteeMap);
                }
                toast('Đã publish proposal thành event.');
                renderProposals();
            }
        });

        const assignCommittee = proposal => openForm({
            title: 'Assign Committee',
            fields: [{ name: 'committeeId', label: 'Committee', type: 'select', options: committees.map(item => ({ value: item.id, label: item.name })) }],
            values: { committeeId: committeeMap[proposal.id] || '' },
            onSubmit: async payload => {
                committeeMap[proposal.id] = payload.committeeId;
                localSet('proposalCommittees', committeeMap);
                toast('Đã phân committee.');
                renderProposals();
            }
        });

        const updateStatus = proposal => openForm({
            title: 'Proposal Status Tracking',
            fields: [
                { name: 'status', label: 'Trạng thái', type: 'select', options: statusOptions },
                { name: 'note', label: 'Ghi chú', type: 'textarea', full: true }
            ],
            values: proposal,
            onSubmit: async payload => {
                await api(`/proposals/${proposal.id}/status`, { method: 'PUT', body: JSON.stringify(payload) });
                toast('Đã cập nhật proposal.');
                renderProposals();
            }
        });

        content(`
            <div class="metric-grid">
                ${metric('Workflow proposals', number(proposals.length), 'Chưa chuyển thành event')}
                ${metric('Need review', number(needsReview.length), 'Pending hoặc cần chỉnh sửa')}
                ${metric('Ready to publish', number(readyToPublish.length), 'Đã duyệt')}
                ${metric('Rejected', number(statusCounts.REJECTED), 'Không tiếp tục')}
            </div>
            ${table(['Proposal', 'Khoa', 'Ngày đề xuất', 'Status', 'Committee', 'Actions'], proposals.map(proposal => {
                const committee = committees.find(item => String(item.id) === String(committeeMap[proposal.id]));
                const canPublish = String(proposal.status).toUpperCase() === 'APPROVED';
                return `<tr>
                    <td><span class="cell-title">${h(proposal.title)}</span><span class="cell-sub">${h(proposal.description || '')}</span></td>
                    <td>${h(proposal.departmentName)}</td>
                    <td>${dateTime(proposal.proposedDate)}</td>
                    <td>${badge(proposal.status, tone(proposal.status))}</td>
                    <td>${committee ? badge(committee.name, 'blue') : badge('Chưa phân', 'amber')}</td>
                    <td><div class="row-actions">
                        <button class="icon-btn" data-proposal-detail="${proposal.id}">${icon('eye')}</button>
                        <button class="icon-btn" data-proposal-status="${proposal.id}">${icon('list-checks')}</button>
                        <button class="icon-btn" data-proposal-committee="${proposal.id}">${icon('users-round')}</button>
                        ${canPublish ? `<button class="icon-btn" data-proposal-publish="${proposal.id}" title="Publish thành event">${icon('send')}</button>` : ''}
                        <button class="icon-btn danger" data-proposal-delete="${proposal.id}">${icon('trash-2')}</button>
                    </div></td>
                </tr>`;
            }))}
        `);
        document.querySelectorAll('[data-proposal-detail]').forEach(button => button.onclick = () => {
            const proposal = proposals.find(item => String(item.id) === button.dataset.proposalDetail);
            openDetail('Proposal Detail', `${detailGrid([
                ['Title', proposal.title],
                ['Department', proposal.departmentName],
                ['Status', proposal.status],
                ['Proposed date', dateTime(proposal.proposedDate)],
                ['Created', dateTime(proposal.createdAt)],
                ['Note', proposal.note || 'N/A']
            ])}<div class="panel" style="margin-top:1rem"><p class="panel-note">${h(proposal.description || '')}</p></div>`);
        });
        document.querySelectorAll('[data-proposal-status]').forEach(button => button.onclick = () => updateStatus(proposals.find(item => String(item.id) === button.dataset.proposalStatus)));
        document.querySelectorAll('[data-proposal-committee]').forEach(button => button.onclick = () => assignCommittee(proposals.find(item => String(item.id) === button.dataset.proposalCommittee)));
        document.querySelectorAll('[data-proposal-publish]').forEach(button => button.onclick = () => publish(proposals.find(item => String(item.id) === button.dataset.proposalPublish)));
        document.querySelectorAll('[data-proposal-delete]').forEach(button => button.onclick = () => confirmAction('Xóa proposal này?', async () => {
            await api(`/proposals/${button.dataset.proposalDelete}`, { method: 'DELETE' });
            toast('Đã xóa proposal.');
            renderProposals();
        }));
    }

    async function renderEvents() {
        state.page = 'events';
        shell(`<button class="btn" id="addBudget">${icon('coins')}Thêm ngân sách</button><button class="btn primary" id="addEvent">${icon('calendar-plus')}Create Event</button>`);
        const [rawEvents, departments] = await Promise.all([load('/events', []), load('/departments', [])]);
        const events = [...rawEvents].sort(compareEventsByTime);
        const featured = localGet('featuredEvents', {});
        const eventCommitteeMap = localGet('eventCommittees', {});
        const committees = getCommittees();
        const departmentOptions = departments.map(item => ({ value: item.id, label: `${item.facultyName || facultyOfDepartment(item.name)} / ${item.name}` }));
        const statusOptions = ['PENDING', 'APPROVED', 'PUBLISHED', 'COMPLETED', 'CANCELLED'].map(value => ({ value, label: value }));
        const committeeOptions = [{ value: '', label: 'Chưa phân committee' }]
            .concat(committees.map(item => ({ value: item.id, label: item.name })));
        const fallbackCommittee = event => committees.length ? committees[Math.abs(Number(event.id || 0)) % committees.length] : null;
        const committeeForEvent = event => {
            const assigned = committees.find(item => String(item.id) === String(eventCommitteeMap[event.id]));
            return assigned || fallbackCommittee(event);
        };

        const openEventForm = (event = {}) => openForm({
            title: event.id ? 'Edit Event' : 'Create Event',
            fields: [
                { name: 'title', label: 'Tên event', required: true },
                { name: 'departmentId', label: 'Khoa', type: 'select', options: departmentOptions, required: true },
                { name: 'committeeId', label: 'Committee phụ trách', type: 'select', options: committeeOptions },
                { name: 'location', label: 'Địa điểm', required: true },
                { name: 'status', label: 'Status', type: 'select', options: statusOptions, required: true },
                { name: 'startTime', label: 'Bắt đầu', type: 'datetime-local', required: true },
                { name: 'endTime', label: 'Kết thúc', type: 'datetime-local', required: true },
                { name: 'capacity', label: 'Capacity', type: 'number', defaultValue: 100 },
                { name: 'budget', label: 'Ngân sách', type: 'number', defaultValue: 0 },
                { name: 'imageUrl', label: 'Ảnh sự kiện', type: 'url', full: true },
                { name: 'description', label: 'Mô tả', type: 'textarea', full: true }
            ],
            values: {
                ...event,
                committeeId: eventCommitteeMap[event.id] || '',
                startTime: event.startTime ? String(event.startTime).slice(0, 16) : '',
                endTime: event.endTime ? String(event.endTime).slice(0, 16) : ''
            },
            onSubmit: async payload => {
                const committeeId = payload.committeeId || '';
                delete payload.committeeId;
                const saved = await api(event.id ? `/events/${event.id}` : '/events', { method: event.id ? 'PUT' : 'POST', body: JSON.stringify(payload) });
                if (committeeId) eventCommitteeMap[saved.id || event.id] = committeeId;
                else delete eventCommitteeMap[saved.id || event.id];
                localSet('eventCommittees', eventCommitteeMap);
                toast('Đã lưu event.');
                renderEvents();
            }
        });

        const capacityForm = event => openForm({
            title: 'Manage Capacity',
            small: true,
            fields: [{ name: 'capacity', label: 'Capacity mới', type: 'number', required: true }],
            values: event,
            onSubmit: async payload => {
                await api(`/events/${event.id}/capacity`, { method: 'PUT', body: JSON.stringify(payload) });
                toast('Đã cập nhật capacity.');
                renderEvents();
            }
        });

        const statusForm = event => openForm({
            title: 'Update Event Status',
            small: true,
            fields: [{ name: 'status', label: 'Status', type: 'select', options: statusOptions }],
            values: event,
            onSubmit: async payload => {
                await api(`/events/${event.id}/status`, { method: 'PUT', body: JSON.stringify(payload) });
                toast('Đã cập nhật event.');
                renderEvents();
            }
        });

        const eventPayload = (event, overrides = {}) => ({
            title: event.title || '',
            departmentId: event.departmentId,
            location: event.location || '',
            status: event.status || 'PUBLISHED',
            startTime: event.startTime ? String(event.startTime).slice(0, 16) : '',
            endTime: event.endTime ? String(event.endTime).slice(0, 16) : '',
            capacity: Number(event.capacity || 100),
            budget: Number(event.budget || 0),
            imageUrl: event.imageUrl || '',
            description: event.description || '',
            ...overrides
        });

        const assignEventCommittee = event => openForm({
            title: `Gán committee: ${event.title}`,
            small: true,
            fields: [{ name: 'committeeId', label: 'Committee phụ trách', type: 'select', options: committeeOptions }],
            values: { committeeId: eventCommitteeMap[event.id] || '' },
            onSubmit: async payload => {
                if (payload.committeeId) eventCommitteeMap[event.id] = payload.committeeId;
                else delete eventCommitteeMap[event.id];
                localSet('eventCommittees', eventCommitteeMap);
                toast('Đã cập nhật committee cho event.');
                renderEvents();
            }
        });

        const budgetForm = (event = null) => openForm({
            title: event ? `Thêm ngân sách: ${event.title}` : 'Thêm ngân sách cho event',
            small: true,
            fields: [
                ...(event ? [] : [{
                    name: 'eventId',
                    label: 'Event',
                    type: 'select',
                    required: true,
                    options: events.map(item => ({ value: item.id, label: item.title }))
                }]),
                { name: 'amount', label: 'Số tiền thêm (VND)', type: 'number', required: true, defaultValue: 1000000 }
            ],
            values: { eventId: event?.id || events[0]?.id || '', amount: 1000000 },
            onSubmit: async payload => {
                const target = event || events.find(item => String(item.id) === String(payload.eventId));
                const amount = Number(payload.amount || 0);
                if (!target) throw new Error('Vui lòng chọn event cần thêm ngân sách.');
                if (!Number.isFinite(amount) || amount <= 0) throw new Error('Số tiền thêm phải lớn hơn 0.');
                const nextBudget = Number(target.budget || 0) + amount;
                await api(`/events/${target.id}`, { method: 'PUT', body: JSON.stringify(eventPayload(target, { budget: nextBudget })) });
                toast(`Đã thêm ${money(amount)} vào ngân sách.`);
                renderEvents();
            }
        });

        const toggleFeatured = event => {
            featured[event.id] = !featured[event.id];
            localSet('featuredEvents', featured);
            toast(featured[event.id] ? 'Đã đưa vào Featured Events.' : 'Đã bỏ khỏi Featured Events.');
            renderEvents();
        };

        content(`
            <div class="metric-grid">
                ${metric('All events', number(events.length), 'Event List')}
                ${metric('Published', number(events.filter(item => item.status === 'PUBLISHED').length), 'Đang public')}
                ${metric('Committee', number(events.filter(item => committeeForEvent(item)).length), 'Event có người phụ trách')}
                ${metric('Budget', money(events.reduce((sum, item) => sum + Number(item.budget || 0), 0)), 'Tổng ngân sách')}
            </div>
            ${table(['Event', 'Khoa', 'Committee', 'Thời gian', 'Capacity', 'Ngân sách', 'Status', 'Actions'], events.map(event => `
                <tr>
                    <td>
                        <div class="event-cell">
                            <img class="event-thumb" src="${h(eventImageUrl(event))}" alt="">
                            <div><span class="cell-title">${h(event.title)}</span><span class="cell-sub">${h(event.location || '')}</span></div>
                        </div>
                    </td>
                    <td>${h(event.departmentName)}</td>
                    <td>${committeeForEvent(event) ? badge(committeeForEvent(event).name, 'blue') : badge('Chưa phân', 'amber')}</td>
                    <td>${dateTime(event.startTime)}<span class="cell-sub">${dateTime(event.endTime)}</span></td>
                    <td><div class="progress"><span style="width:${Math.min(100, Number(event.fillRate || 0))}%"></span></div><span class="cell-sub">${number(event.registrationCount)} / ${number(event.capacity)}</span></td>
                    <td>${money(event.budget)}</td>
                    <td>${badge(event.status, tone(event.status))}${featured[event.id] ? ` ${badge('Featured', 'orange')}` : ''}</td>
                    <td><div class="row-actions">
                        <button class="icon-btn" data-event-detail="${event.id}">${icon('eye')}</button>
                        <button class="icon-btn" data-event-edit="${event.id}">${icon('pencil')}</button>
                        <button class="icon-btn" data-event-committee="${event.id}" title="Gán committee">${icon('users-round')}</button>
                        <button class="icon-btn" data-event-budget="${event.id}" title="Thêm ngân sách">${icon('coins')}</button>
                        <button class="icon-btn" data-event-status="${event.id}">${icon('refresh-cw')}</button>
                        <button class="icon-btn" data-event-capacity="${event.id}">${icon('gauge')}</button>
                        <button class="icon-btn" data-event-featured="${event.id}">${icon('star')}</button>
                        <button class="icon-btn danger" data-event-delete="${event.id}">${icon('trash-2')}</button>
                    </div></td>
                </tr>`))}
        `);
        document.getElementById('addEvent').onclick = () => openEventForm();
        document.getElementById('addBudget').onclick = () => events.length ? budgetForm() : toast('Chưa có event để thêm ngân sách.');
        document.querySelectorAll('[data-event-detail]').forEach(button => button.onclick = () => {
            const event = events.find(item => String(item.id) === button.dataset.eventDetail);
            const imagePreview = `<div class="event-preview"><img src="${h(eventImageUrl(event))}" alt=""></div>`;
            openDetail('Event Detail', `${imagePreview}${detailGrid([
                ['Title', event.title],
                ['Department', event.departmentName],
                ['Committee', committeeForEvent(event)?.name || 'Chưa phân'],
                ['Location', event.location],
                ['Status', event.status],
                ['Capacity', number(event.capacity)],
                ['Budget', money(event.budget)],
                ['Registrations', number(event.registrationCount)],
                ['Attendance', number(event.attendanceCount)],
                ['Rating', `${event.averageRating}/5`],
                ['Created', dateTime(event.createdAt)]
            ])}<div class="panel" style="margin-top:1rem"><p class="panel-note">${h(event.description || '')}</p></div>`);
        });
        document.querySelectorAll('[data-event-edit]').forEach(button => button.onclick = () => openEventForm(events.find(item => String(item.id) === button.dataset.eventEdit)));
        document.querySelectorAll('[data-event-committee]').forEach(button => button.onclick = () => assignEventCommittee(events.find(item => String(item.id) === button.dataset.eventCommittee)));
        document.querySelectorAll('[data-event-budget]').forEach(button => button.onclick = () => budgetForm(events.find(item => String(item.id) === button.dataset.eventBudget)));
        document.querySelectorAll('[data-event-status]').forEach(button => button.onclick = () => statusForm(events.find(item => String(item.id) === button.dataset.eventStatus)));
        document.querySelectorAll('[data-event-capacity]').forEach(button => button.onclick = () => capacityForm(events.find(item => String(item.id) === button.dataset.eventCapacity)));
        document.querySelectorAll('[data-event-featured]').forEach(button => button.onclick = () => toggleFeatured(events.find(item => String(item.id) === button.dataset.eventFeatured)));
        document.querySelectorAll('[data-event-delete]').forEach(button => button.onclick = () => confirmAction('Xóa event này? Nếu đã có dữ liệu liên quan, hệ thống sẽ hủy event thay vì xóa vật lý.', async () => {
            await api(`/events/${button.dataset.eventDelete}`, { method: 'DELETE' });
            toast('Đã xử lý event.');
            renderEvents();
        }));
    }

    function getCommittees() {
        return localGet('committees', [
            { id: 'ops', name: 'Event Operations Committee', status: 'ACTIVE', members: [] },
            { id: 'academic', name: 'Academic Review Committee', status: 'ACTIVE', members: [] },
            { id: 'student-life', name: 'Student Life Committee', status: 'REVIEW', members: [] }
        ]);
    }

    async function renderCommittees() {
        state.page = 'committees';
        shell(`<button class="btn primary" id="createCommittee">${icon('plus')}Create Committee</button>`);
        const [users, proposals] = await Promise.all([load('/users', []), load('/proposals', [])]);
        let committees = getCommittees();

        const save = () => localSet('committees', committees);
        const openCommitteeForm = (committee = {}) => openForm({
            title: committee.id ? 'Edit Committee' : 'Create Committee',
            fields: [
                { name: 'name', label: 'Tên committee', required: true },
                { name: 'status', label: 'Status', type: 'select', options: ['ACTIVE', 'REVIEW', 'PAUSED'].map(value => ({ value, label: value })) }
            ],
            values: committee,
            onSubmit: async payload => {
                if (committee.id) Object.assign(committee, payload);
                else committees.push({ id: `committee-${Date.now()}`, name: payload.name, status: payload.status, members: [] });
                save();
                toast('Đã lưu committee.');
                renderCommittees();
            }
        });

        const manageMembers = committee => openForm({
            title: 'Assign Members',
            fields: [{ name: 'memberId', label: 'Thêm thành viên', type: 'select', options: users.map(user => ({ value: user.id, label: `${user.fullName} - ${user.role}` })) }],
            onSubmit: async payload => {
                committee.members = [...new Set([...(committee.members || []), payload.memberId])];
                save();
                toast('Đã thêm thành viên.');
                renderCommittees();
            }
        });

        const workflow = ['PENDING', 'REVISION', 'APPROVED', 'PUBLISHED'].map(status => `
            <div class="kanban-column">
                <h3 class="kanban-title">${h(status)}</h3>
                ${proposals.filter(item => String(item.status).toUpperCase() === status).slice(0, 5).map(item => `
                    <div class="mini-card"><strong>${h(item.title)}</strong><span>${h(item.departmentName)} · ${dateTime(item.proposedDate)}</span></div>
                `).join('') || '<div class="empty">Trống</div>'}
            </div>`).join('');

        content(`
            <div class="metric-grid">
                ${metric('Committees', number(committees.length), 'Committee List')}
                ${metric('Members assigned', number(committees.reduce((sum, item) => sum + (item.members || []).length, 0)), 'Tổng thành viên')}
                ${metric('Active', number(committees.filter(item => item.status === 'ACTIVE').length), 'Đang hoạt động')}
                ${metric('Pending proposals', number(proposals.filter(item => item.status === 'PENDING').length), 'Workflow duyệt')}
            </div>
            ${table(['Committee', 'Members', 'Status', 'Actions'], committees.map(committee => {
                const memberNames = (committee.members || []).map(id => users.find(user => String(user.id) === String(id))?.fullName).filter(Boolean);
                return `<tr>
                    <td><span class="cell-title">${h(committee.name)}</span></td>
                    <td>${memberNames.length ? h(memberNames.join(', ')) : badge('Chưa có', 'amber')}</td>
                    <td>${badge(committee.status, tone(committee.status))}</td>
                    <td><div class="row-actions">
                        <button class="icon-btn" data-committee-members="${committee.id}">${icon('user-plus')}</button>
                        <button class="icon-btn" data-committee-edit="${committee.id}">${icon('pencil')}</button>
                        <button class="icon-btn danger" data-committee-remove="${committee.id}">${icon('user-minus')}</button>
                    </div></td>
                </tr>`;
            }))}
            <section class="panel">
                <h2 class="panel-title">Approval Workflow</h2>
                <p class="panel-note">Theo dõi proposal qua các trạng thái duyệt.</p>
                <div class="kanban" style="margin-top:.85rem">${workflow}</div>
            </section>
        `);
        document.getElementById('createCommittee').onclick = () => openCommitteeForm();
        document.querySelectorAll('[data-committee-members]').forEach(button => button.onclick = () => manageMembers(committees.find(item => item.id === button.dataset.committeeMembers)));
        document.querySelectorAll('[data-committee-edit]').forEach(button => button.onclick = () => openCommitteeForm(committees.find(item => item.id === button.dataset.committeeEdit)));
        document.querySelectorAll('[data-committee-remove]').forEach(button => button.onclick = () => {
            const committee = committees.find(item => item.id === button.dataset.committeeRemove);
            if ((committee.members || []).length) committee.members.pop();
            else committees = committees.filter(item => item.id !== committee.id);
            save();
            toast('Đã xóa thành viên hoặc committee trống.');
            renderCommittees();
        });
    }

    async function renderRegistrations() {
        state.page = 'registrations';
        shell();
        const registrations = await load('/registrations', []);
        const statusCounts = summarizeStatuses(registrations);

        const updateStatus = registration => openForm({
            title: 'Registration Detail / Status',
            fields: [
                { name: 'status', label: 'Status', type: 'select', options: ['REGISTERED', 'WAITLIST', 'CANCELLED'].map(value => ({ value, label: value })) },
                { name: 'note', label: 'Ghi chú', type: 'textarea', full: true }
            ],
            values: registration,
            onSubmit: async payload => {
                await api(`/registrations/${registration.id}/status`, { method: 'PUT', body: JSON.stringify(payload) });
                toast('Đã cập nhật registration.');
                renderRegistrations();
            }
        });

        const render = (q = '') => {
            const filtered = registrations.filter(item => !q || normalize(`${item.eventTitle} ${item.studentName} ${item.studentEmail} ${item.studentCode} ${item.status}`).includes(normalize(q)));
            const pageSize = 50;
            const pages = Math.max(1, Math.ceil(filtered.length / pageSize));
            const page = Math.min(Math.max(Number(state.filters.registrationPage || 1), 1), pages);
            const start = (page - 1) * pageSize;
            const visible = filtered.slice(start, start + pageSize);
            state.filters.registrationPage = page;
            content(`
                <div class="metric-grid">
                    ${metric('Registrations', number(registrations.length), 'Registration List')}
                    ${metric('Registered', number(statusCounts.REGISTERED), 'Đã đăng ký')}
                    ${metric('Waitlist', number(statusCounts.WAITLIST), 'Quản lý waitlist')}
                    ${metric('Attendance', number(registrations.filter(item => item.attendanceStatus === 'ATTENDED').length), 'Attendance Tracking')}
                </div>
                <div class="toolbar">
                    ${searchBox('registrationSearch', 'Tìm event, sinh viên, status...')}
                    ${pagination('registrations', page, pages, filtered.length, visible.length)}
                </div>
                ${table(['Sinh viên', 'Event', 'Registration', 'Status', 'Attendance', 'Actions'], visible.map(item => `
                    <tr>
                        <td><span class="cell-title">${h(item.studentName || item.studentCode)}</span><span class="cell-sub">${h(item.studentEmail || item.studentMajor)}</span></td>
                        <td>${h(item.eventTitle)}<span class="cell-sub">${dateTime(item.eventStartTime)}</span></td>
                        <td>${dateTime(item.registrationDate)}</td>
                        <td>${badge(item.status, tone(item.status))}</td>
                        <td>${badge(item.attendanceStatus, tone(item.attendanceStatus))}</td>
                        <td><div class="row-actions">
                            <button class="icon-btn" data-registration-detail="${item.id}">${icon('eye')}</button>
                            <button class="icon-btn" data-registration-status="${item.id}">${icon('list-checks')}</button>
                        </div></td>
                    </tr>`))}
            `);
            const search = document.getElementById('registrationSearch');
            if (search) {
                search.value = q;
                search.addEventListener('input', event => {
                    state.filters.registrationPage = 1;
                    render(event.target.value);
                });
            }
            document.querySelectorAll('[data-pagination="registrations"] [data-page-step]').forEach(button => {
                button.onclick = () => {
                    state.filters.registrationPage = Math.min(Math.max(page + Number(button.dataset.pageStep), 1), pages);
                    render(q);
                };
            });
            document.querySelectorAll('[data-registration-detail]').forEach(button => button.onclick = () => {
                const item = registrations.find(registration => String(registration.id) === button.dataset.registrationDetail);
                openDetail('Registration Detail', detailGrid([
                    ['Student', item.studentName || item.studentCode],
                    ['Email', item.studentEmail],
                    ['Event', item.eventTitle],
                    ['Status', item.status],
                    ['Attendance', item.attendanceStatus],
                    ['Registered at', dateTime(item.registrationDate)],
                    ['Check-in', item.attendance?.checkinTime ? dateTime(item.attendance.checkinTime) : 'N/A'],
                    ['Note', item.note || 'N/A']
                ]));
            });
            document.querySelectorAll('[data-registration-status]').forEach(button => button.onclick = () => updateStatus(registrations.find(item => String(item.id) === button.dataset.registrationStatus)));
        };
        render();
    }

    async function renderFeedback() {
        state.page = 'feedback';
        shell(`<button class="btn" id="exportFeedback">${icon('download')}Export CSV</button>`);
        const feedback = await load('/feedback', []);
        const avg = feedback.length ? feedback.reduce((sum, item) => sum + Number(item.rating || 0), 0) / feedback.length : 0;
        const ratings = [5, 4, 3, 2, 1].map(star => ({ star, count: feedback.filter(item => Number(item.rating) === star).length }));
        const max = Math.max(...ratings.map(item => item.count), 1);
        const render = (q = '') => {
            const filtered = feedback.filter(item => !q || normalize(`${item.eventTitle} ${item.studentName} ${item.comment}`).includes(normalize(q)));
            content(`
                <div class="metric-grid">
                    ${metric('Feedback', number(feedback.length), 'Feedback List')}
                    ${metric('Average rating', `${avg.toLocaleString('vi-VN', { maximumFractionDigits: 1 })}/5`, 'Rating Analysis')}
                    ${metric('Low ratings', number(feedback.filter(item => Number(item.rating) <= 2).length), 'Cần xử lý')}
                    ${metric('Events reviewed', number(new Set(feedback.map(item => item.eventId)).size), 'Event có feedback')}
                </div>
                <div class="split-grid">
                    <section class="panel">
                        <div class="toolbar">${searchBox('feedbackSearch', 'Tìm event, sinh viên, comment...')}</div>
                        ${table(['Event', 'Student', 'Rating', 'Comment', 'Actions'], filtered.map(item => `
                            <tr>
                                <td>${h(item.eventTitle)}</td>
                                <td><span class="cell-title">${h(item.studentName || item.studentCode)}</span><span class="cell-sub">${h(item.studentEmail || '')}</span></td>
                                <td>${badge(`${item.rating}/5`, Number(item.rating) <= 2 ? 'rose' : 'green')}</td>
                                <td>${h(item.comment || '')}</td>
                                <td><button class="icon-btn danger" data-feedback-delete="${item.id}" title="Delete Comment">${icon('trash-2')}</button></td>
                            </tr>`))}
                    </section>
                    <section class="panel">
                        <h2 class="panel-title">Rating Analysis</h2>
                        <div class="rating-bars" style="margin-top:1rem">
                            ${ratings.map(item => `
                                <div class="rating-row">
                                    <span>${item.star} sao</span>
                                    <div class="progress"><span style="width:${Math.round(item.count / max * 100)}%"></span></div>
                                    <span>${number(item.count)}</span>
                                </div>`).join('')}
                        </div>
                    </section>
                </div>
            `);
            bindFilters('feedbackSearch', feedback, render);
            document.querySelectorAll('[data-feedback-delete]').forEach(button => button.onclick = () => confirmAction('Xóa comment này?', async () => {
                await api(`/feedback/${button.dataset.feedbackDelete}`, { method: 'DELETE' });
                toast('Đã xóa feedback.');
                renderFeedback();
            }));
            document.getElementById('exportFeedback').onclick = () => exportCsv('feedback.csv', [
                ['Event', 'Student', 'Email', 'Rating', 'Comment', 'Created At'],
                ...feedback.map(item => [item.eventTitle, item.studentName, item.studentEmail, item.rating, item.comment, item.createdAt])
            ]);
        };
        render();
    }

    async function renderReports() {
        state.page = 'reports';
        shell(`<button class="btn" id="exportReport">${icon('file-spreadsheet')}Export Excel</button><button class="btn" id="printReport">${icon('file-text')}Export PDF</button>`);
        const [reports, events, registrations, users] = await Promise.all([
            load('/reports', {}),
            load('/events', []),
            load('/registrations', []),
            load('/users', [])
        ]);
        const participationByMajor = users.reduce((acc, user) => {
            const major = user.major || 'N/A';
            acc[major] = (acc[major] || 0) + 1;
            return acc;
        }, {});
        content(`
            <div class="metric-grid">
                ${metric('Registration rate', percent(reports.registrationRate), `${number(reports.elapsedRegistrations)} / ${number(reports.elapsedCapacity)} capacity`)}
                ${metric('Attendance rate', percent(reports.attendanceRate), `${number(reports.elapsedAttendance)} check-in`)}
                ${metric('Average rating', `${Number(reports.averageRating || 0).toLocaleString('vi-VN', { maximumFractionDigits: 1 })}/5`, 'Event đã diễn ra')}
                ${metric('Student participation', number(registrations.length), `${number(users.filter(user => user.role === 'STUDENT').length)} sinh viên`)}
            </div>
            <section class="panel">
                <h2 class="panel-title">Event Statistics</h2>
                ${chart(reports.monthly || [])}
            </section>
            <div class="split-grid">
                <section class="panel">
                    <h2 class="panel-title">Attendance Reports</h2>
                    ${table(['Event', 'Registered', 'Attendance', 'Fill'], events.slice(0, 10).map(event => `
                        <tr>
                            <td>${h(event.title)}<span class="cell-sub">${h(event.departmentName)}</span></td>
                            <td>${number(event.registrationCount)}</td>
                            <td>${number(event.attendanceCount)}</td>
                            <td><div class="progress"><span style="width:${Math.min(100, Number(event.fillRate || 0))}%"></span></div><span class="cell-sub">${percent(event.fillRate)}</span></td>
                        </tr>`))}
                </section>
                <section class="panel">
                    <h2 class="panel-title">Student Participation</h2>
                    ${table(['Khoa/Major', 'Users'], Object.entries(participationByMajor).map(([major, count]) => `
                        <tr><td>${h(major)}</td><td>${number(count)}</td></tr>`))}
                </section>
            </div>
            <section class="panel">
                <h2 class="panel-title">Công thức</h2>
                <p class="panel-note">${h(reports.formula?.registrationRate || '')}</p>
                <p class="panel-note">${h(reports.formula?.attendanceRate || '')}</p>
                <p class="panel-note">${h(reports.formula?.averageRating || '')}</p>
            </section>
        `);
        document.getElementById('exportReport').onclick = () => exportCsv('admin-report.csv', [
            ['Metric', 'Value'],
            ['Registration rate', percent(reports.registrationRate)],
            ['Attendance rate', percent(reports.attendanceRate)],
            ['Average rating', reports.averageRating],
            ['Events', events.length],
            ['Registrations', registrations.length]
        ]);
        document.getElementById('printReport').onclick = () => window.print();
    }

    async function renderEmail() {
        state.page = 'email';
        shell(`<button class="btn primary" id="sendNotification">${icon('send')}Send Notification</button><button class="btn" id="addTemplate">${icon('file-plus')}Email Template</button>`);
        const payload = await load('/email-logs?page=0&size=80', { items: [] });
        const logs = payload.items || [];
        let templates = localGet('emailTemplates', [
            { id: 'welcome', name: 'Welcome Event', subject: 'Chào mừng bạn đến với sự kiện', content: 'Xin chào, vé QR của bạn đã sẵn sàng.' },
            { id: 'reminder', name: 'Event Reminder', subject: 'Nhắc lịch sự kiện', content: 'Sự kiện sẽ bắt đầu trong thời gian tới.' }
        ]);
        let announcements = localGet('announcements', []);

        const saveTemplates = () => localSet('emailTemplates', templates);
        const saveAnnouncements = () => localSet('announcements', announcements);

        const templateForm = (template = {}) => openForm({
            title: template.id ? 'Email Templates' : 'Create Email Template',
            fields: [
                { name: 'name', label: 'Tên template', required: true },
                { name: 'subject', label: 'Subject', required: true },
                { name: 'content', label: 'Nội dung', type: 'textarea', full: true, required: true }
            ],
            values: template,
            onSubmit: async payload => {
                if (template.id) Object.assign(template, payload);
                else templates.push({ id: `tpl-${Date.now()}`, ...payload });
                saveTemplates();
                toast('Đã lưu template.');
                renderEmail();
            }
        });

        const sendForm = () => openForm({
            title: 'Send Notifications',
            fields: [
                { name: 'toEmail', label: 'Email nhận', type: 'email', required: true },
                { name: 'subject', label: 'Subject', required: true },
                { name: 'content', label: 'Nội dung', type: 'textarea', full: true, required: true }
            ],
            onSubmit: async payload => {
                await api('/email-logs', { method: 'POST', body: JSON.stringify({ ...payload, status: 'SENT', sentAt: new Date().toISOString().slice(0, 16) }) });
                toast('Đã ghi nhận thông báo đã gửi.');
                renderEmail();
            }
        });

        const announcementForm = () => openForm({
            title: 'Announcement Management',
            fields: [
                { name: 'title', label: 'Tiêu đề', required: true },
                { name: 'audience', label: 'Đối tượng', type: 'select', options: ['All', 'Students', 'Departments', 'Committees'].map(value => ({ value, label: value })) },
                { name: 'content', label: 'Nội dung', type: 'textarea', full: true, required: true }
            ],
            onSubmit: async payload => {
                announcements.unshift({ id: Date.now(), createdAt: new Date().toISOString(), ...payload });
                saveAnnouncements();
                toast('Đã tạo announcement.');
                renderEmail();
            }
        });

        content(`
            <div class="metric-grid">
                ${metric('Email History', number(payload.totalItems || logs.length), 'Lịch sử email')}
                ${metric('Sent', number(logs.filter(log => log.status === 'SENT').length), 'Gửi thành công')}
                ${metric('Failed', number(logs.filter(log => log.status === 'FAILED').length), 'Gửi lỗi')}
                ${metric('Templates', number(templates.length), 'Template email')}
            </div>
            <div class="split-grid">
                <section class="panel">
                    <div class="panel-header">
                        <h2 class="panel-title">Email History</h2>
                        <button class="btn" id="createAnnouncement">${icon('megaphone')}Announcement</button>
                    </div>
                    ${table(['Email', 'Subject', 'Status', 'Sent At'], logs.map(log => `
                        <tr>
                            <td>${h(log.toEmail)}</td>
                            <td>${h(log.subject)}<span class="cell-sub">${h(log.eventTitle || '')}</span></td>
                            <td>${badge(log.status, tone(log.status))}</td>
                            <td>${dateTime(log.sentAt)}</td>
                        </tr>`))}
                </section>
                <section class="panel">
                    <h2 class="panel-title">Email Templates</h2>
                    <div style="display:grid;gap:.55rem;margin-top:.85rem">
                        ${templates.map(template => `
                            <div class="mini-card">
                                <strong>${h(template.name)}</strong>
                                <span>${h(template.subject)}</span>
                                <div class="inline-actions" style="margin-top:.55rem;justify-content:flex-start">
                                    <button class="btn" data-template-edit="${template.id}">${icon('pencil')}Edit</button>
                                </div>
                            </div>`).join('')}
                    </div>
                    <h2 class="panel-title" style="margin-top:1rem">Announcements</h2>
                    <div style="display:grid;gap:.55rem;margin-top:.85rem">
                        ${announcements.slice(0, 4).map(item => `<div class="mini-card"><strong>${h(item.title)}</strong><span>${h(item.audience)} · ${dateTime(item.createdAt)}</span></div>`).join('') || '<div class="empty">Chưa có announcement.</div>'}
                    </div>
                </section>
            </div>
        `);
        document.getElementById('sendNotification').onclick = sendForm;
        document.getElementById('addTemplate').onclick = () => templateForm();
        document.getElementById('createAnnouncement').onclick = announcementForm;
        document.querySelectorAll('[data-template-edit]').forEach(button => button.onclick = () => templateForm(templates.find(item => item.id === button.dataset.templateEdit)));
    }

    async function renderSecurity() {
        state.page = 'security';
        shell(`<button class="btn primary" id="saveSystemSettings">${icon('save')}Lưu cấu hình</button><button class="btn" id="backupDb">${icon('database-backup')}Backup</button>`);
        const [overview, logs] = await Promise.all([
            load('/dashboard', {}),
            load('/activity-logs?page=0&size=40', { items: [] })
        ]);
        const settings = localGet('systemSettings', {
            oauthClient: 'Google OAuth2',
            recaptcha: 'Enabled',
            smtpHost: 'smtp.gmail.com',
            smtpPort: 587,
            maintenance: 'Off',
            sessionMinutes: 60
        });

        const settingForm = () => openForm({
            title: 'System Settings',
            fields: [
                { name: 'oauthClient', label: 'OAuth2 Configuration', required: true },
                { name: 'recaptcha', label: 'reCAPTCHA Settings', type: 'select', options: ['Enabled', 'Disabled', 'Test mode'].map(value => ({ value, label: value })) },
                { name: 'smtpHost', label: 'SMTP Host', required: true },
                { name: 'smtpPort', label: 'SMTP Port', type: 'number', required: true },
                { name: 'maintenance', label: 'Maintenance', type: 'select', options: ['Off', 'Read-only', 'Full maintenance'].map(value => ({ value, label: value })) },
                { name: 'sessionMinutes', label: 'Session timeout', type: 'number', required: true }
            ],
            values: settings,
            onSubmit: async payload => {
                localSet('systemSettings', payload);
                toast('Đã lưu cấu hình hệ thống.');
                renderSecurity();
            }
        });

        content(`
            <div class="metric-grid">
                ${metric('OAuth2', settings.oauthClient, 'Google login')}
                ${metric('reCAPTCHA', settings.recaptcha, 'Captcha protection')}
                ${metric('SMTP', `${settings.smtpHost}:${settings.smtpPort}`, 'Mail server')}
                ${metric('Access Logs', number(logs.totalItems || logs.items.length), 'Nhật ký truy cập')}
            </div>
            <div class="split-grid">
                <section class="panel">
                    <h2 class="panel-title">Security & System</h2>
                    ${detailGrid([
                        ['OAuth2 Configuration', settings.oauthClient],
                        ['reCAPTCHA Settings', settings.recaptcha],
                        ['SMTP Settings', `${settings.smtpHost}:${settings.smtpPort}`],
                        ['Maintenance mode', settings.maintenance],
                        ['Session timeout', `${settings.sessionMinutes} phút`],
                        ['Backup scope', 'Users, events, reports, email logs']
                    ])}
                    <div class="inline-actions" style="justify-content:flex-start;margin-top:1rem">
                        <button class="btn" id="restoreDb">${icon('upload')}Restore database</button>
                    </div>
                </section>
                <section class="panel">
                    <h2 class="panel-title">Access Logs</h2>
                    ${renderTimeline((logs.items || []).slice(0, 10))}
                </section>
            </div>
        `);
        document.getElementById('saveSystemSettings').onclick = settingForm;
        document.getElementById('backupDb').onclick = () => exportCsv('aems-backup-summary.csv', [
            ['Section', 'Count'],
            ['Users', overview.stats?.totalUsers || 0],
            ['Events', overview.stats?.totalEvents || 0],
            ['Registrations', overview.stats?.totalRegistrations || 0],
            ['Emails', (overview.stats?.sentEmails || 0) + (overview.stats?.failedEmails || 0)]
        ]);
        document.getElementById('restoreDb').onclick = () => toast('Restore UI đã sẵn sàng. Backend cần endpoint restore để chạy thật.');
    }

    async function renderDeployment() {
        state.page = 'deployment';
        shell(`<button class="btn" id="refreshDeploy">${icon('refresh-cw')}Refresh</button>`);
        const [overview, email] = await Promise.all([
            load('/overview', {}),
            load('/email-logs?page=0&size=20', { items: [] })
        ]);
        const containers = localGet('containers', [
            { name: 'spring-api', image: 'eventmanagementt:1.0.0', status: 'RUNNING', cpu: '11%', memory: '512 MB' },
            { name: 'sqlserver', image: 'mssql/server:latest', status: 'RUNNING', cpu: '24%', memory: '1.8 GB' },
            { name: 'mail-worker', image: 'smtp-relay:local', status: email.items?.some(log => log.status === 'FAILED') ? 'ERROR' : 'RUNNING', cpu: '3%', memory: '96 MB' }
        ]);
        const failed = (email.items || []).filter(log => log.status === 'FAILED');
        content(`
            <div class="metric-grid">
                ${metric('Server Status', 'ONLINE', 'Tomcat 8081 / context /api')}
                ${metric('Deployment Monitor', 'Healthy', 'Static + API responding')}
                ${metric('Containers', number(containers.length), 'Docker Container Status')}
                ${metric('Error Logs', number(failed.length), 'Email/API lỗi gần đây')}
            </div>
            <div class="split-grid">
                <section class="panel">
                    <h2 class="panel-title">Docker Container Status</h2>
                    ${table(['Container', 'Image', 'Status', 'CPU', 'Memory'], containers.map(container => `
                        <tr>
                            <td><span class="cell-title">${h(container.name)}</span></td>
                            <td>${h(container.image)}</td>
                            <td>${badge(container.status, tone(container.status))}</td>
                            <td>${h(container.cpu)}</td>
                            <td>${h(container.memory)}</td>
                        </tr>`))}
                </section>
                <section class="panel">
                    <h2 class="panel-title">Error Logs</h2>
                    ${failed.length ? table(['Time', 'To', 'Subject'], failed.map(log => `
                        <tr><td>${dateTime(log.sentAt)}</td><td>${h(log.toEmail)}</td><td>${h(log.subject)}</td></tr>`)) : '<div class="empty">Không có error log email trong trang hiện tại.</div>'}
                </section>
            </div>
            <section class="panel">
                <h2 class="panel-title">Deployment Monitor</h2>
                <p class="panel-note">Snapshot hệ thống: ${number(overview.stats?.totalUsers)} users, ${number(overview.stats?.totalEvents)} events, ${number(overview.stats?.totalRegistrations)} registrations.</p>
            </section>
        `);
        document.getElementById('refreshDeploy').onclick = renderDeployment;
    }

    const handlers = {
        overview: renderOverview,
        users: renderUsers,
        roles: renderRoles,
        departments: renderDepartments,
        reports: renderReports,
        email: renderEmail,
        logs: renderLogs,
        proposals: renderProposals,
        events: renderEvents,
        committees: renderCommittees,
        registrations: renderRegistrations,
        feedback: renderFeedback,
        security: renderSecurity,
        deployment: renderDeployment
    };

    function init() {
        (handlers[state.page] || renderOverview)().catch(error => {
            console.error(error);
            shell();
            content(`<div class="error">${h(error.message || error)}</div>`);
        });
    }

    return { init };
})();

document.addEventListener('DOMContentLoaded', Admin.init);
