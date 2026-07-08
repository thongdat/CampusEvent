const Admin = (() => {
    const API_BASE = window.location.protocol === 'file:'
        ? 'http://localhost:8081/api/admin'
        : '/api/admin';
    const CACHE_TTL_MS = 20_000;
    const CACHE_PREFIX = 'aems-admin-api-cache:';
    let logoutInProgress = false;

    const eventScopeOptions = [
        { value: 'all', label: 'Tất cả sự kiện' },
        { value: 'active', label: 'Sự kiện diễn ra' },
        { value: 'completed', label: 'Sự kiện kết thúc' }
    ];

    const navItems = [
        { group: 'Tổng quan', id: 'overview', label: 'Tổng quan', icon: 'layout-dashboard', href: 'overview.html', keywords: 'tong quan home dashboard' },
        { group: 'Tổng quan', id: 'reports', label: 'Báo cáo', icon: 'bar-chart-3', href: 'reports.html', keywords: 'reports analytics bao cao thong ke' },
        { group: 'Người dùng', id: 'users', label: 'Người dùng', icon: 'users', href: 'users.html', keywords: 'users nguoi dung tai khoan' },
        { group: 'Người dùng', id: 'roles', label: 'Phân quyền', icon: 'shield-check', href: 'roles.html', keywords: 'roles permissions phan quyen' },
        { group: 'Người dùng', id: 'departments', label: 'Khoa & Bộ môn', icon: 'building-2', href: 'departments.html', keywords: 'departments khoa bo mon' },
        { group: 'Sự kiện', id: 'proposals', label: 'Đề xuất', icon: 'clipboard-list', href: 'proposals.html', keywords: 'proposals de xuat workflow' },
        { group: 'Sự kiện', id: 'events', label: 'Tất cả sự kiện', icon: 'calendar-days', href: 'events.html', keywords: 'events su kien tat ca dang dien ra da ket thuc', children: eventScopeOptions },
        { group: 'Sự kiện', id: 'registrations', label: 'Đăng ký', icon: 'ticket-check', href: 'registrations.html', keywords: 'registrations dang ky waitlist attendance' },
        { group: 'Sự kiện', id: 'feedback', label: 'Phản hồi', icon: 'message-square-heart', href: 'feedback.html', keywords: 'feedback phan hoi rating' },
        { group: 'Hệ thống', id: 'email', label: 'Email & Thông báo', icon: 'mail-check', href: 'email.html', keywords: 'email mail notification thong bao' },
        { group: 'Hệ thống', id: 'logs', label: 'Nhật ký hoạt động', icon: 'history', href: 'logs.html', keywords: 'logs nhat ky activity audit' }
    ];

    const pageMeta = {
        overview: ['Xin chào, Admin User! 👋', 'Đây là tổng quan hệ thống hôm nay.', 'Tổng quan'],
        reports: ['Báo cáo & Phân tích', 'Thống kê event, tỷ lệ tham dự, rating và xuất file báo cáo.', 'Báo cáo'],
        logs: ['Nhật ký hoạt động', 'Theo dõi mọi hành động và truy cập của người dùng.', 'Nhật ký'],
        users: ['Quản lý người dùng', 'Danh sách tài khoản, phân loại, khóa/mở khóa và đặt lại mật khẩu.', 'Người dùng'],
        roles: ['Phân quyền & Vai trò', 'Tạo vai trò, ma trận quyền và gán quyền cho người dùng.', 'Phân quyền'],
        departments: ['Khoa & Bộ môn', 'Quản lý cây tổ chức theo khoa lớn và bộ môn, gán trưởng đơn vị.', 'Khoa & Bộ môn'],
        proposals: ['Đề xuất sự kiện', 'Theo dõi proposal, phân hội đồng, công bố hoặc loại bỏ.', 'Đề xuất'],
        events: ['Tất cả sự kiện', 'Chọn xem sự kiện đang diễn ra hoặc sự kiện đã kết thúc.', 'Tất cả sự kiện'],
        registrations: ['Đăng ký & Điểm danh', 'Danh sách đăng ký, waitlist và check-in của sinh viên.', 'Đăng ký'],
        feedback: ['Phản hồi & Đánh giá', 'Danh sách feedback, phân tích rating và xử lý bình luận.', 'Phản hồi'],
        email: ['Email & Thông báo', 'Email templates, gửi thông báo, announcement và lịch sử email.', 'Email']
    };

    const academicStructure = [
        { faculty: 'Công nghệ Thông tin', departments: ['Công nghệ Thông tin', 'Kỹ thuật phần mềm', 'An toàn thông tin', 'Trí tuệ nhân tạo', 'Data Science'] },
        { faculty: 'Kinh tế', departments: ['Kinh tế', 'Marketing', 'Quản trị kinh doanh', 'Tài chính Ngân hàng'] },
        { faculty: 'Thiết kế & Truyền thông', departments: ['Thiết kế Mỹ thuật số', 'Thiết kế Đồ họa', 'Truyền thông đa phương tiện'] },
        { faculty: 'Ngôn ngữ', departments: ['Ngôn ngữ Anh', 'Ngôn ngữ Nhật'] },
        { faculty: 'Du lịch - Khách sạn', departments: ['Du lịch - Khách sạn', 'Hospitality Management'] }
    ];

    const initialPage = document.body.dataset.page || 'overview';
    const state = {
        page: initialPage === 'eventsCompleted' ? 'events' : initialPage,
        cache: {},
        filters: initialPage === 'eventsCompleted' || document.body.dataset.eventScope
            ? { eventScope: document.body.dataset.eventScope || 'completed' }
            : initialPage === 'events'
                ? { eventScope: document.body.dataset.eventScope || 'all' }
                : {}
    };

    function currentEventScope() {
        const scope = state.filters.eventScope || 'all';
        return eventScopeOptions.some(item => item.value === scope) ? scope : 'all';
    }

    function eventScopeLabel(scope = currentEventScope()) {
        return eventScopeOptions.find(item => item.value === scope)?.label || 'Tất cả sự kiện';
    }

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

    function matchesSearch(haystack, query) {
        const text = normalize(haystack);
        return normalize(query).split(/\s+/).filter(Boolean).every(token => text.includes(token));
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
        const gallery = imageValues(event?.imageUrls);
        if (event?.imageUrl) return event.imageUrl;
        if (gallery.length) return gallery[0];
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

    const imageLibrary = [
        { label: 'Cong nghe', url: 'https://images.unsplash.com/photo-1517048676732-d65bc937f952?auto=format&fit=crop&w=900&q=80' },
        { label: 'Workshop', url: 'https://images.unsplash.com/photo-1540575467063-027a26d3b38c?auto=format&fit=crop&w=900&q=80' },
        { label: 'Lap trinh', url: 'https://images.unsplash.com/photo-1498050108023-c5249f4df085?auto=format&fit=crop&w=900&q=80' },
        { label: 'Cloud', url: 'https://images.unsplash.com/photo-1451187580459-43490279c0fa?auto=format&fit=crop&w=900&q=80' },
        { label: 'Bao mat', url: 'https://images.unsplash.com/photo-1550751827-4bd374c3f58b?auto=format&fit=crop&w=900&q=80' },
        { label: 'AI/Data', url: 'https://images.unsplash.com/photo-1555255707-c07966088b7b?auto=format&fit=crop&w=900&q=80' },
        { label: 'Kinh te', url: 'https://images.unsplash.com/photo-1556761175-b413da4baf72?auto=format&fit=crop&w=900&q=80' },
        { label: 'Thiet ke', url: 'https://images.unsplash.com/photo-1558655146-d09347e92766?auto=format&fit=crop&w=900&q=80' }
    ];

    function imageValues(value) {
        const list = [];
        const add = item => {
            if (Array.isArray(item)) {
                item.forEach(add);
                return;
            }
            String(item || '').split(/[\r\n,|]+/).forEach(part => {
                const url = part.trim();
                if (url && !list.some(existing => existing.toLowerCase() === url.toLowerCase())) list.push(url);
            });
        };
        add(value);
        return list.slice(0, 8);
    }

    function imageChoices(seed = []) {
        const selected = imageValues(seed);
        return imageValues(selected.concat(imageLibrary.map(item => item.url)));
    }

    function cacheKey(path) {
        return `${CACHE_PREFIX}${path}`;
    }

    function clearApiCache() {
        try {
            Object.keys(sessionStorage)
                .filter(key => key.startsWith(CACHE_PREFIX))
                .forEach(key => sessionStorage.removeItem(key));
        } catch (error) {
            // Cache is an optimization only.
        }
    }

    function getCached(path) {
        try {
            const raw = sessionStorage.getItem(cacheKey(path));
            if (!raw) return null;
            const entry = JSON.parse(raw);
            if (!entry || Date.now() - Number(entry.time || 0) > CACHE_TTL_MS) {
                sessionStorage.removeItem(cacheKey(path));
                return null;
            }
            return entry.data;
        } catch (error) {
            return null;
        }
    }

    function setCached(path, data) {
        try {
            sessionStorage.setItem(cacheKey(path), JSON.stringify({ time: Date.now(), data }));
        } catch (error) {
            // Storage may be full or disabled; the app should still work.
        }
    }

    async function api(path, options = {}) {
        const method = String(options.method || 'GET').toUpperCase();
        // Timeout để request không treo vô hạn khi máy chủ/DB đang "ngủ dậy" (cold start).
        const timeoutMs = options.timeoutMs || 60000;
        const controller = new AbortController();
        const timer = window.setTimeout(() => controller.abort(), timeoutMs);
        let response;
        try {
            response = await fetch(`${API_BASE}${path}`, {
                headers: {
                    Accept: 'application/json',
                    'Content-Type': 'application/json',
                    ...(options.headers || {})
                },
                signal: controller.signal,
                ...options
            });
        } catch (err) {
            if (err && err.name === 'AbortError') {
                throw new Error('Máy chủ đang khởi động (cold start). Vui lòng thử lại sau vài giây.');
            }
            throw err;
        } finally {
            window.clearTimeout(timer);
        }
        if (response.status === 401) {
            window.location.href = '/api/login.html';
            throw new Error('Bạn cần đăng nhập để tiếp tục.');
        }
        if (response.status === 403) {
            throw new Error('Bạn không có quyền truy cập chức năng này.');
        }
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
        if (method !== 'GET') {
            clearApiCache();
        }
        if (response.status === 204) return null;
        return response.json();
    }

    async function load(path, fallback) {
        const cached = getCached(path);
        if (cached !== null) {
            return cached;
        }
        try {
            const data = await api(path);
            setCached(path, data);
            return data;
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

    function toast(message, tone = '') {
        const node = document.getElementById('toast');
        if (!node) return;
        node.textContent = message;
        node.className = 'toast show' + (tone ? ' ' + tone : '');
        window.clearTimeout(toast.timer);
        toast.timer = window.setTimeout(() => {
            node.classList.remove('show');
        }, 2800);
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
            if (item.children?.length) {
                const scope = state.page === 'events' ? currentEventScope() : '';
                const childKeywords = item.children.map(child => child.label).join(' ');
                const expanded = state.page === 'events' || localGet('navEventsOpen', false);
                return `${groupHeader}
                    <div class="nav-dropdown${expanded ? ' open' : ''}" data-nav-dropdown="${h(item.id)}">
                        <button type="button" class="nav-link nav-link-parent${state.page === 'events' ? ' active' : ''}" data-nav-toggle="${h(item.id)}" data-keywords="${h(item.label + ' ' + childKeywords + ' ' + (item.keywords || ''))}">
                            ${icon(item.icon, 'h-4 w-4')}
                            <span>${h(item.label)}</span>
                            ${icon('chevron-down', 'nav-chevron h-3.5 w-3.5')}
                        </button>
                        <div class="nav-sub">
                            ${item.children.map(child => `
                                <a class="nav-link nav-sublink${scope === child.value ? ' active' : ''}" href="${h(item.href)}" data-nav="${h(item.id)}" data-event-scope="${h(child.value)}" data-keywords="${h(child.label + ' ' + (item.keywords || ''))}">
                                    <span>${h(child.label)}</span>
                                </a>`).join('')}
                        </div>
                    </div>`;
            }
            return `${groupHeader}
                <a class="nav-link${item.id === state.page ? ' active' : ''}" href="${item.href}" data-nav="${h(item.id)}" data-keywords="${h(item.label + ' ' + (item.keywords || ''))}">
                    ${icon(item.icon, 'h-4 w-4')}
                    <span>${h(item.label)}</span>
                </a>`;
        }).join('');
    }

    function shell(actions = '') {
        const meta = state.page === 'events'
            ? [eventScopeLabel(), 'Danh sách sự kiện theo bộ lọc đang chọn.', eventScopeLabel()]
            : (pageMeta[state.page] || pageMeta.overview);
        const [title, subtitle, crumb] = meta;
        const nav = renderNavList(navItems);
        const user = currentUser();
        const collapsedClass = localGet('sidebarCollapsed', false) ? ' collapsed' : '';
        const today = new Date().toLocaleDateString('vi-VN');

        document.getElementById('app').innerHTML = `
            <div class="app-shell${collapsedClass}" id="appShell">
                <aside class="sidebar" id="sidebar">
                    <a class="brand" href="overview.html">
                        <span class="brand-mark">A</span>
                        <span class="brand-copy">
                            <span class="brand-title">AEMS Admin</span>
                            <span class="brand-subtitle">Control console</span>
                        </span>
                        <button class="sidebar-toggle" type="button" id="sidebarCollapseBtn" aria-label="Thu gọn sidebar" title="Thu gọn">
                            ${icon('panel-left-close', 'h-3.5 w-3.5')}
                        </button>
                    </a>
                    <label class="nav-search" title="Tìm trang">
                        ${icon('search', 'h-3.5 w-3.5')}
                        <input type="search" id="navSearch" placeholder="Tìm trang..." autocomplete="off">
                        <kbd>Ctrl K</kbd>
                    </label>
                    <nav class="nav" id="navList">${nav}</nav>
                    <div class="sidebar-footer">
                        <div class="sidebar-account">
                            <span class="account-mark">${h(user.initials)}</span>
                            <span>
                                <strong>${h(user.fullName)}</strong>
                                <span>${h(user.role)}${user.email ? ' · ' + h(user.email) : ''}</span>
                            </span>
                        </div>
                        <span class="sidebar-copy">SWP Event Management · ${new Date().getFullYear()}</span>
                    </div>
                </aside>
                <div class="sidebar-backdrop" id="sidebarBackdrop"></div>
                <main class="main">
                    <header class="topbar">
                        <div class="topbar-left">
                            <button class="menu-toggle" type="button" id="menuToggle" aria-label="Mở menu">
                                ${icon('menu', 'h-4 w-4')}
                            </button>
                            <div class="topbar-title">
                                <nav class="breadcrumb" aria-label="Đường dẫn">
                                    ${icon('home', 'h-3 w-3')}
                                    <span>Admin Console</span>
                                    ${icon('chevron-right', 'h-3 w-3')}
                                    <span class="breadcrumb-current">${h(crumb || title)}</span>
                                </nav>
                                <h1 class="page-title">${h(title)}</h1>
                                <p class="page-subtitle">${h(subtitle)}</p>
                            </div>
                        </div>
                        <div class="topbar-actions">
                            <button class="command-trigger" type="button" id="commandTrigger" title="Mở Command Palette (Ctrl+K)">
                                ${icon('search', 'h-4 w-4')}<span>Tìm kiếm...</span><kbd>Ctrl K</kbd>
                            </button>
                            <div class="date-chip" title="Ngày hiện tại">
                                ${icon('calendar-days', 'h-4 w-4')}<span>${h(today)}</span>
                            </div>
                            <div class="toolbar">${actions}</div>
                            ${accountMenu()}
                        </div>
                    </header>
                    <section id="content" class="content">
                        ${skeleton()}
                    </section>
                </main>
            </div>
            <div id="modalRoot" class="modal-backdrop"></div>
            <div id="cmdkRoot" class="cmdk-backdrop"></div>
            <div id="toast" class="toast"></div>
        `;
        bindShell();
        refreshIcons();
    }

    function skeleton(type = 'page') {
        if (type === 'table') {
            return `
                <div class="panel"><div class="skeleton skeleton-line medium"></div>
                    <div class="skeleton skeleton-line"></div>
                    <div class="skeleton skeleton-line"></div>
                    <div class="skeleton skeleton-line short"></div>
                </div>`;
        }
        return `
            <div class="skeleton-grid">
                <div class="skeleton skeleton-card"></div>
                <div class="skeleton skeleton-card"></div>
                <div class="skeleton skeleton-card"></div>
                <div class="skeleton skeleton-card"></div>
            </div>
            <div class="panel">
                <div class="skeleton skeleton-line medium"></div>
                <div class="skeleton skeleton-line"></div>
                <div class="skeleton skeleton-line"></div>
                <div class="skeleton skeleton-line short"></div>
            </div>`;
    }

    function emptyState({ icon: iconName = 'inbox', title = 'Chưa có dữ liệu', copy = '', actions = '' } = {}) {
        return `
            <div class="empty">
                <span class="empty-icon">${icon(iconName, 'h-5 w-5')}</span>
                <span class="empty-title">${h(title)}</span>
                ${copy ? `<span class="empty-copy">${h(copy)}</span>` : ''}
                ${actions ? `<div class="empty-actions">${actions}</div>` : ''}
            </div>`;
    }

    function bindShell() {
        bindAccountMenu();
        const collapseBtn = document.getElementById('sidebarCollapseBtn');
        if (collapseBtn) {
            collapseBtn.addEventListener('click', event => {
                event.preventDefault();
                event.stopPropagation();
                const shellEl = document.getElementById('appShell');
                const next = !shellEl.classList.contains('collapsed');
                shellEl.classList.toggle('collapsed', next);
                localSet('sidebarCollapsed', next);
            });
        }
        const menuToggle = document.getElementById('menuToggle');
        const sidebar = document.getElementById('sidebar');
        const backdrop = document.getElementById('sidebarBackdrop');
        if (menuToggle && sidebar && backdrop) {
            const openMenu = () => {
                sidebar.classList.add('open');
                backdrop.classList.add('open');
            };
            const closeMenu = () => {
                sidebar.classList.remove('open');
                backdrop.classList.remove('open');
            };
            menuToggle.addEventListener('click', openMenu);
            backdrop.addEventListener('click', closeMenu);
            sidebar.querySelectorAll('a').forEach(link => link.addEventListener('click', closeMenu));
        }
        document.querySelectorAll('[data-nav-toggle]').forEach(button => {
            button.addEventListener('click', event => {
                event.preventDefault();
                event.stopPropagation();
                const wrap = button.closest('[data-nav-dropdown]');
                const opening = !wrap?.classList.contains('open');
                wrap?.classList.toggle('open', opening);
                localSet('navEventsOpen', opening);
            });
        });
        const navSearch = document.getElementById('navSearch');
        if (navSearch) {
            navSearch.addEventListener('input', event => {
                const query = event.target.value;
                document.querySelectorAll('#navList .nav-link, #navList .nav-link-parent').forEach(link => {
                    const keywords = link.dataset.keywords || '';
                    link.style.display = !query || matchesSearch(keywords, query) ? '' : 'none';
                });
                document.querySelectorAll('#navList .nav-dropdown').forEach(dropdown => {
                    const parent = dropdown.querySelector('.nav-link-parent');
                    const subs = dropdown.querySelectorAll('.nav-sublink');
                    const parentMatch = parent && parent.style.display !== 'none';
                    const subMatch = Array.from(subs).some(link => link.style.display !== 'none');
                    dropdown.style.display = parentMatch || subMatch ? '' : 'none';
                    if (query && subMatch) dropdown.classList.add('open');
                });
                document.querySelectorAll('#navList .nav-group').forEach(group => {
                    let next = group.nextElementSibling;
                    let any = false;
                    while (next && !next.classList.contains('nav-group')) {
                        if ((next.classList.contains('nav-link') || next.classList.contains('nav-dropdown')) && next.style.display !== 'none') any = true;
                        next = next.nextElementSibling;
                    }
                    group.style.display = any ? '' : 'none';
                });
            });
        }
        const trigger = document.getElementById('commandTrigger');
        if (trigger) trigger.addEventListener('click', openCommandPalette);

        const appShell = document.getElementById('appShell');
        if (appShell) {
            appShell.addEventListener('click', event => {
                const link = event.target.closest('a[href$=".html"]');
                if (!link || !appShell.contains(link)) return;
                if (event.defaultPrevented || event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;
                const page = pageFromHref(link.getAttribute('href'));
                if (!page) return;
                const scope = link.dataset.eventScope;
                if (scope) {
                    state.filters.eventScope = scope;
                    state.filters.eventsPage = 1;
                    localSet('navEventsOpen', true);
                }
                event.preventDefault();
                navigate(page, link.getAttribute('href'), !!scope);
            });
        }
    }

    function content(html) {
        document.getElementById('content').innerHTML = html;
        refreshIcons();
    }

    function pageFromHref(href) {
        const file = String(href || '').split('?')[0].split('#')[0].split('/').pop();
        return navItems.find(item => item.href === file)?.id || '';
    }

    function navigate(page, href = '', force = false) {
        if (!handlers[page]) return false;
        if (page === state.page && document.getElementById('content') && !force) return true;
        state.page = page;
        if (href && window.location.pathname.split('/').pop() !== href) {
            window.history.pushState({ page }, '', href);
        }
        handlers[page]().catch(error => {
            console.error(error);
            shell();
            content(`<div class="error"><strong>Không thể tải dữ liệu</strong><br>${h(error.message || error)}</div>`);
        });
        return true;
    }

    function metric(label, value, hint = '', tone = '', iconName = '') {
        const toneClass = tone ? ` tone-${tone}` : '';
        const iconBadge = iconName ? `<span class="metric-icon">${icon(iconName, 'h-4 w-4')}</span>` : '';
        const spark = tone ? `<svg class="metric-spark" viewBox="0 0 92 28" aria-hidden="true"><path d="M2 21 C12 12 18 18 27 11 S43 3 53 11 S67 18 76 13 S86 14 90 18"></path></svg>` : '';
        return `
            <article class="metric${toneClass}">
                <div class="metric-top">
                    <p class="metric-label">${h(label)}</p>
                    ${iconBadge}
                </div>
                <p class="metric-value">${h(value)}</p>
                <p class="metric-hint">${h(hint)}</p>
                ${spark}
            </article>`;
    }

    function table(headers, rows, emptyText = 'Không có dữ liệu.') {
        if (!rows.length) {
            return emptyState({
                icon: 'inbox',
                title: emptyText,
                copy: 'Khi có dữ liệu mới, danh sách sẽ tự cập nhật ở đây.'
            });
        }
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

    function inputDateValue(date) {
        const value = date instanceof Date ? date : new Date(date);
        if (Number.isNaN(value.getTime())) return '';
        const pad = part => String(part).padStart(2, '0');
        return `${value.getFullYear()}-${pad(value.getMonth() + 1)}-${pad(value.getDate())}`;
    }

    function parseDateInput(value, endOfDay = false) {
        if (!value) return null;
        const date = new Date(`${value}T${endOfDay ? '23:59:59' : '00:00:00'}`);
        return Number.isNaN(date.getTime()) ? null : date;
    }

    function defaultReportRange(events) {
        const now = new Date();
        const firstDayOfMonth = new Date(now.getFullYear(), now.getMonth(), 1);
        return {
            from: inputDateValue(firstDayOfMonth),
            to: inputDateValue(now)
        };
    }

    function reportEventDate(event) {
        const date = new Date(event?.startTime || event?.createdAt || 0);
        return Number.isNaN(date.getTime()) ? null : date;
    }

    function eventBucketLabel(date, mode) {
        if (mode === 'month') {
            return `${String(date.getMonth() + 1).padStart(2, '0')}/${date.getFullYear()}`;
        }
        if (mode === 'week') {
            const first = new Date(date);
            const day = date.getDay() || 7;
            first.setDate(date.getDate() - day + 1);
            return `Tuần ${inputDateValue(first).slice(5).replace('-', '/')}`;
        }
        return inputDateValue(date).slice(5).replace('-', '/');
    }

    function buildEventTrend(events, fromDate, toDate) {
        const days = Math.max(1, Math.ceil((toDate - fromDate) / 86400000) + 1);
        const mode = days > 120 ? 'month' : days > 45 ? 'week' : 'day';
        const buckets = new Map();
        const cursor = new Date(fromDate);
        while (cursor <= toDate) {
            buckets.set(eventBucketLabel(cursor, mode), { label: eventBucketLabel(cursor, mode), events: 0, registrations: 0, attendance: 0 });
            cursor.setDate(cursor.getDate() + (mode === 'day' ? 1 : mode === 'week' ? 7 : 32));
            if (mode === 'month') cursor.setDate(1);
        }
        events.forEach(event => {
            const date = reportEventDate(event);
            if (!date) return;
            const label = eventBucketLabel(date, mode);
            const bucket = buckets.get(label) || { label, events: 0, registrations: 0, attendance: 0 };
            bucket.events += 1;
            bucket.registrations += Number(event.registrationCount || 0);
            bucket.attendance += Number(event.attendanceCount || 0);
            buckets.set(label, bucket);
        });
        return [...buckets.values()];
    }

    function buildEventFaculties(events) {
        const colors = ['#2563eb', '#0f766e', '#f37021', '#7c3aed', '#db2777', '#0891b2', '#65a30d'];
        const grouped = events.reduce((acc, event) => {
            const name = facultyOfDepartment(event.departmentName || event.department || event.major || 'Khác');
            if (!acc[name]) acc[name] = [];
            acc[name].push(event);
            return acc;
        }, {});
        return Object.entries(grouped)
            .map(([name, items], index) => ({ name, items, count: items.length, color: colors[index % colors.length] }))
            .sort((left, right) => right.count - left.count || left.name.localeCompare(right.name, 'vi'));
    }

    function eventLineChart(trend = []) {
        if (!trend.length) {
            return emptyState({ icon: 'bar-chart-3', title: 'Không có dữ liệu trong khoảng thời gian đã chọn.' });
        }
        const width = 720;
        const height = 260;
        const padX = 44;
        const padTop = 22;
        const padBottom = 46;
        const values = trend.map(item => Number(item.events || 0));
        const max = Math.max(...values, 5);
        const stepX = trend.length > 1 ? (width - padX * 2) / (trend.length - 1) : 0;
        const yOf = value => padTop + (height - padTop - padBottom) * (1 - (Number(value || 0) / max));
        const points = trend.map((item, index) => ({
            x: padX + stepX * index,
            y: yOf(item.events),
            value: Number(item.events || 0),
            registrations: Number(item.registrations || 0),
            attendance: Number(item.attendance || 0),
            label: item.label
        }));
        const line = points.map((point, index) => `${index ? 'L' : 'M'} ${point.x.toFixed(1)} ${point.y.toFixed(1)}`).join(' ');
        const area = `${line} L ${points[points.length - 1].x.toFixed(1)} ${height - padBottom} L ${points[0].x.toFixed(1)} ${height - padBottom} Z`;
        const grid = [0, .25, .5, .75, 1].map(ratio => {
            const y = padTop + (height - padTop - padBottom) * ratio;
            const label = Math.round(max * (1 - ratio));
            return `<g><line x1="${padX}" y1="${y.toFixed(1)}" x2="${width - padX}" y2="${y.toFixed(1)}" class="chart-grid-line"></line><text x="14" y="${(y + 4).toFixed(1)}" class="chart-axis">${label}</text></g>`;
        }).join('');
        const tooltipWidth = 184;
        const tooltipHeight = 92;
        const tooltipFor = point => {
            const x = Math.min(Math.max(point.x - tooltipWidth / 2, padX + 6), width - padX - tooltipWidth - 6);
            const y = point.y > 126 ? point.y - tooltipHeight - 36 : point.y + 34;
            const caretY = point.y > 126 ? y + tooltipHeight : y;
            const caret = point.y > 126
                ? `M ${point.x - 7} ${caretY} L ${point.x} ${caretY + 8} L ${point.x + 7} ${caretY} Z`
                : `M ${point.x - 7} ${caretY} L ${point.x} ${caretY - 8} L ${point.x + 7} ${caretY} Z`;
            return `
                <g class="chart-tooltip" transform="translate(${x.toFixed(1)} ${y.toFixed(1)})">
                    <rect width="${tooltipWidth}" height="${tooltipHeight}" rx="10"></rect>
                    <text x="14" y="22" class="chart-tooltip-title">${h(point.label)}</text>
                    <text x="14" y="46" class="chart-tooltip-main">${number(point.value)} sự kiện</text>
                    <text x="14" y="66" class="chart-tooltip-sub">Đăng ký: ${number(point.registrations)}</text>
                    <text x="104" y="66" class="chart-tooltip-sub">Tham dự: ${number(point.attendance)}</text>
                </g>
                <path d="${caret}" class="chart-tooltip-caret"></path>`;
        };
        return `
            <div class="line-chart">
                <svg viewBox="0 0 ${width} ${height}" role="img" aria-label="Xu hướng số lượng sự kiện">
                    <defs>
                        <linearGradient id="eventAreaGrad" x1="0" y1="0" x2="0" y2="1">
                            <stop offset="0%" stop-color="#2563eb" stop-opacity=".26"></stop>
                            <stop offset="100%" stop-color="#2563eb" stop-opacity=".02"></stop>
                        </linearGradient>
                    </defs>
                    ${grid}
                    <path d="${area}" class="chart-area"></path>
                    <path d="${line}" class="chart-line"></path>
                    ${points.map(point => `
                        <g class="chart-point" tabindex="0" aria-label="${h(`${point.label}: ${number(point.value)} sự kiện, ${number(point.registrations)} đăng ký, ${number(point.attendance)} tham dự`)}">
                            <line x1="${point.x.toFixed(1)}" y1="${padTop}" x2="${point.x.toFixed(1)}" y2="${height - padBottom}" class="chart-hover-line"></line>
                            <circle cx="${point.x.toFixed(1)}" cy="${point.y.toFixed(1)}" r="4.5" class="chart-dot"></circle>
                            <circle cx="${point.x.toFixed(1)}" cy="${point.y.toFixed(1)}" r="10" class="chart-dot-halo"></circle>
                            <text x="${point.x.toFixed(1)}" y="${(point.y - 13).toFixed(1)}" class="chart-value">${number(point.value)}</text>
                            <text x="${point.x.toFixed(1)}" y="${height - 16}" class="chart-label">${h(point.label)}</text>
                            ${tooltipFor(point)}
                            <rect x="${(point.x - Math.max(24, stepX / 2)).toFixed(1)}" y="${padTop}" width="${Math.max(48, stepX).toFixed(1)}" height="${height - padTop - padBottom}" class="chart-hit-area"></rect>
                        </g>`).join('')}
                </svg>
            </div>`;
    }

    function chart(monthly = []) {
        return eventLineChart(monthly);
    }

    function pieSlicePath(cx, cy, radius, startAngle, endAngle) {
        const start = {
            x: cx + radius * Math.cos(startAngle),
            y: cy + radius * Math.sin(startAngle)
        };
        const end = {
            x: cx + radius * Math.cos(endAngle),
            y: cy + radius * Math.sin(endAngle)
        };
        const largeArc = endAngle - startAngle > Math.PI ? 1 : 0;
        return `M ${cx} ${cy} L ${start.x.toFixed(2)} ${start.y.toFixed(2)} A ${radius} ${radius} 0 ${largeArc} 1 ${end.x.toFixed(2)} ${end.y.toFixed(2)} Z`;
    }

    function eventPieChart(groups = []) {
        const total = groups.reduce((sum, item) => sum + item.count, 0);
        if (!total) {
            return emptyState({ icon: 'pie-chart', title: 'Không có dữ liệu trong khoảng thời gian đã chọn.' });
        }
        let angle = -Math.PI / 2;
        const slices = groups.map(group => {
            const startAngle = angle;
            const nextAngle = angle + (group.count / total) * Math.PI * 2;
            const midAngle = startAngle + (nextAngle - startAngle) / 2;
            const full = groups.length === 1;
            const slice = {
                ...group,
                path: full ? '' : pieSlicePath(120, 120, 92, startAngle, nextAngle),
                percent: group.count / total * 100,
                midAngle,
                full
            };
            angle = nextAngle;
            return slice;
        });
        return `
            <div class="pie-chart-wrap">
                <svg class="pie-chart" viewBox="0 0 240 240" role="img" aria-label="Cơ cấu sự kiện theo ngành">
                    ${slices.map(slice => slice.full
                        ? `<circle class="pie-slice pie-slice-full" cx="120" cy="120" r="92" fill="${slice.color}"></circle>`
                        : `<path class="pie-slice" d="${slice.path}" fill="${slice.color}"></path>`).join('')}
                    <circle cx="120" cy="120" r="48" class="pie-hole"></circle>
                    <text x="120" y="114" class="pie-total">${number(total)}</text>
                    <text x="120" y="136" class="pie-total-label">sự kiện</text>
                    ${slices.map(slice => {
                        const names = slice.items.slice(0, 4).map(item => item.title).filter(Boolean).join(', ');
                        const more = slice.items.length > 4 ? `, +${slice.items.length - 4} sự kiện khác` : '';
                        const tooltipX = Math.min(Math.max(120 + Math.cos(slice.midAngle) * 112 - 82, 6), 76);
                        const tooltipY = Math.min(Math.max(120 + Math.sin(slice.midAngle) * 96 - 50, 6), 126);
                        return `<g class="pie-segment" tabindex="0" aria-label="${h(`${slice.name}: ${number(slice.count)} sự kiện, ${slice.percent.toLocaleString('vi-VN', { maximumFractionDigits: 1 })}%`)}">
                            ${slice.full
                                ? '<circle class="pie-hit-slice pie-hit-slice-full" cx="120" cy="120" r="92"></circle>'
                                : `<path class="pie-hit-slice" d="${slice.path}"></path>`}
                            <g class="pie-tooltip" transform="translate(${tooltipX.toFixed(1)} ${tooltipY.toFixed(1)})">
                                <foreignObject width="164" height="108">
                                    <div class="pie-tooltip-card" xmlns="http://www.w3.org/1999/xhtml">
                                        <strong>${h(slice.name)}</strong>
                                        <b>${number(slice.count)} sự kiện · ${slice.percent.toLocaleString('vi-VN', { maximumFractionDigits: 1 })}%</b>
                                        <span>${h((names || 'N/A') + more)}</span>
                                        <button type="button" data-report-faculty="${h(slice.name)}">Xem thêm</button>
                                    </div>
                                </foreignObject>
                            </g>
                        </g>`;
                    }).join('')}
                </svg>
                <div class="pie-legend">
                    ${slices.map(slice => `
                        <div class="pie-legend-item" title="${h(slice.items.map(item => item.title).filter(Boolean).join(', '))}">
                            <span class="pie-swatch" style="background:${slice.color}"></span>
                            <span><strong>${h(slice.name)}</strong><small>${number(slice.count)} sự kiện · ${slice.percent.toLocaleString('vi-VN', { maximumFractionDigits: 1 })}%</small></span>
                        </div>`).join('')}
                </div>
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

    function openForm({ title, fields, values = {}, submitText = 'Lưu', small = false, onSubmit, validate }) {
        const modal = document.getElementById('modalRoot');
        const errorTag = name => `<small class="field-error" data-field-error="${h(name)}" style="display:none;color:#dc2626;font-weight:600;margin-top:.25rem"></small>`;
        const fieldHtml = fields.map(field => {
            const value = values[field.name] ?? field.defaultValue ?? '';
            const full = field.full ? ' full' : '';
            if (field.type === 'image-picker') {
                const selectedImages = imageValues(value);
                const choices = imageChoices(selectedImages);
                return `<div class="field${full} image-picker-field" data-field-wrap="${h(field.name)}" data-image-picker="${h(field.name)}">
                    <label>${h(field.label)}</label>
                    <input type="hidden" name="${h(field.name)}" value="${h(selectedImages.join('\n'))}">
                    <div class="field-help">Chọn tối đa 8 ảnh, ảnh đầu tiên sẽ làm ảnh bìa. Bấm lại vào ảnh để bỏ chọn, ảnh trùng sẽ tự được bỏ.</div>
                    <div class="image-picker-grid">
                        ${choices.map(url => `<button class="image-pick ${selectedImages.includes(url) ? 'active' : ''}" type="button" data-image-choice="${h(url)}"><img src="${h(url)}" alt=""><span>${selectedImages.includes(url) ? 'Đã chọn' : 'Chọn'}</span></button>`).join('')}
                    </div>
                    <div class="image-url-row">
                        <input type="url" data-image-url-input placeholder="Dán link ảnh riêng rồi bấm thêm">
                        <button class="btn" type="button" data-image-url-add>${icon('plus', 'h-3.5 w-3.5')}Thêm ảnh</button>
                    </div>
                    <div class="selected-images" data-image-selected></div>
                    ${errorTag(field.name)}
                </div>`;
            }
            let control;
            if (field.type === 'textarea') {
                control = `<textarea name="${h(field.name)}">${h(value)}</textarea>`;
            } else if (field.type === 'select') {
                const options = (field.options || []).map(option => {
                    const selected = String(value) === String(option.value) ? 'selected' : '';
                    return `<option value="${h(option.value)}" ${selected}>${h(option.label)}</option>`;
                }).join('');
                control = `<select name="${h(field.name)}">${options}</select>`;
            } else {
                control = `<input name="${h(field.name)}" type="${h(field.type || 'text')}" value="${h(value)}">`;
            }
            return `<div class="field${full}" data-field-wrap="${h(field.name)}"><label>${h(field.label)}${field.required ? ' *' : ''}</label>${control}${errorTag(field.name)}</div>`;
        }).join('');

        modal.innerHTML = `
            <form class="modal ${small ? 'small' : ''}" id="modalForm" novalidate>
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
        bindImagePickers(modal);

        const form = document.getElementById('modalForm');
        const currentValues = () => {
            const data = {};
            fields.forEach(field => {
                const el = form.querySelector(`[name="${field.name}"]`);
                data[field.name] = el ? el.value : (values[field.name] ?? '');
            });
            return data;
        };
        const isVisible = (field, vals) => typeof field.visible !== 'function' || field.visible(vals);
        const applyVisibility = () => {
            const vals = currentValues();
            fields.forEach(field => {
                if (typeof field.visible !== 'function') return;
                const wrap = form.querySelector(`[data-field-wrap="${field.name}"]`);
                if (wrap) wrap.style.display = field.visible(vals) ? '' : 'none';
            });
        };
        const clearFieldError = name => {
            const node = form.querySelector(`[data-field-error="${name}"]`);
            if (node) { node.textContent = ''; node.style.display = 'none'; }
        };
        const showFieldErrors = errors => {
            fields.forEach(field => {
                const node = form.querySelector(`[data-field-error="${field.name}"]`);
                if (!node) return;
                const msg = errors[field.name];
                node.textContent = msg || '';
                node.style.display = msg ? 'block' : 'none';
            });
            const first = Object.keys(errors)[0];
            if (first) form.querySelector(`[name="${first}"]`)?.focus();
        };

        applyVisibility();
        form.addEventListener('input', event => { if (event.target?.name) clearFieldError(event.target.name); applyVisibility(); });
        form.addEventListener('change', applyVisibility);

        form.addEventListener('submit', async event => {
            event.preventDefault();
            const vals = currentValues();
            const errors = {};
            fields.forEach(field => {
                if (!isVisible(field, vals)) return;
                if (field.required) {
                    const v = vals[field.name];
                    if (v == null || String(v).trim() === '') {
                        errors[field.name] = field.type === 'select'
                            ? `Vui lòng chọn ${field.label}.`
                            : `Vui lòng nhập ${field.label}.`;
                    }
                }
            });
            if (typeof validate === 'function') {
                const custom = validate(vals, field => isVisible(field, vals)) || {};
                Object.keys(custom).forEach(key => { if (custom[key] && !errors[key]) errors[key] = custom[key]; });
            }
            showFieldErrors(errors);
            if (Object.keys(errors).length) return;

            const payload = {};
            fields.forEach(field => {
                if (!isVisible(field, vals)) return;
                const raw = vals[field.name];
                if (field.type === 'number') payload[field.name] = raw === '' || raw == null ? null : Number(raw);
                else if (field.type === 'image-picker') payload[field.name] = imageValues(form.querySelector(`[name="${field.name}"]`)?.value);
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

    function bindImagePickers(scope) {
        scope.querySelectorAll('[data-image-picker]').forEach(picker => {
            const hidden = picker.querySelector('input[type="hidden"]');
            const input = picker.querySelector('[data-image-url-input]');
            const selectedNode = picker.querySelector('[data-image-selected]');
            const buttons = () => Array.from(picker.querySelectorAll('[data-image-choice]'));
            const render = () => {
                const selected = imageValues(hidden.value);
                hidden.value = selected.join('\n');
                buttons().forEach(button => {
                    const active = selected.some(url => url.toLowerCase() === button.dataset.imageChoice.toLowerCase());
                    button.classList.toggle('active', active);
                    const label = button.querySelector('span');
                    if (label) label.textContent = active ? 'Đã chọn' : 'Chọn';
                });
                selectedNode.innerHTML = selected.length
                    ? selected.map((url, index) => `<span class="image-chip"><img src="${h(url)}" alt=""><strong>${index === 0 ? 'Bìa' : index + 1}</strong><button type="button" data-image-remove="${h(url)}" aria-label="Bỏ ảnh">${icon('x', 'h-3 w-3')}</button></span>`).join('')
                    : '<span class="field-help">Chưa chọn ảnh.</span>';
                selectedNode.querySelectorAll('[data-image-remove]').forEach(button => {
                    button.onclick = () => {
                        hidden.value = selected.filter(url => url !== button.dataset.imageRemove).join('\n');
                        render();
                    };
                });
                refreshIcons();
            };
            buttons().forEach(button => {
                button.onclick = () => {
                    const selected = imageValues(hidden.value);
                    const url = button.dataset.imageChoice;
                    const exists = selected.some(item => item.toLowerCase() === url.toLowerCase());
                    hidden.value = (exists ? selected.filter(item => item.toLowerCase() !== url.toLowerCase()) : selected.concat(url)).join('\n');
                    render();
                };
            });
            picker.querySelector('[data-image-url-add]')?.addEventListener('click', () => {
                const selected = imageValues(hidden.value);
                const url = (input.value || '').trim();
                if (!url) return;
                hidden.value = selected.concat(url).join('\n');
                input.value = '';
                render();
            });
            render();
        });
    }

    function showConfirm({ title = 'Xác nhận thao tác', message = '', confirmText = 'Đồng ý', cancelText = 'Hủy', danger = false } = {}) {
        return new Promise(resolve => {
            const modal = document.getElementById('modalRoot');
            modal.innerHTML = `
                <div class="modal small ${danger ? 'danger' : ''}" role="alertdialog" aria-modal="true">
                    <div class="modal-head">
                        <div>
                            <h2 class="modal-title">${h(title)}</h2>
                        </div>
                        <button class="icon-btn" type="button" data-cancel aria-label="Đóng">${icon('x')}</button>
                    </div>
                    <div class="modal-body">
                        <p style="margin:0;color:#334155;line-height:1.55">${h(message)}</p>
                    </div>
                    <div class="modal-actions">
                        <button class="btn" type="button" data-cancel>${h(cancelText)}</button>
                        <button class="btn ${danger ? 'danger' : 'primary'}" type="button" data-ok autofocus>${h(confirmText)}</button>
                    </div>
                </div>`;
            modal.classList.add('open');
            const close = result => {
                modal.classList.remove('open');
                modal.innerHTML = '';
                resolve(result);
            };
            modal.querySelectorAll('[data-cancel]').forEach(button => button.addEventListener('click', () => close(false)));
            modal.querySelector('[data-ok]').addEventListener('click', () => close(true));
            window.setTimeout(() => {
                const ok = modal.querySelector('[data-ok]');
                if (ok) ok.focus();
            }, 60);
            refreshIcons();
        });
    }

    async function confirmAction(message, action, options = {}) {
        const ok = await showConfirm({
            title: options.title || 'Xác nhận',
            message,
            confirmText: options.confirmText || 'Xác nhận',
            danger: options.danger !== false
        });
        if (!ok) return;
        try {
            await action();
        } catch (error) {
            toast(error.message || 'Thao tác thất bại.', 'error');
        }
    }

    function actionMenu(items = []) {
        if (!items.length) return '';
        const id = `menu-${Math.random().toString(36).slice(2, 8)}`;
        const buttons = items.map((item, index) => {
            if (item.divider) return '<div class="action-divider"></div>';
            return `<button class="action-item ${item.danger ? 'danger' : ''}" type="button" data-menu-index="${index}">
                ${item.icon ? icon(item.icon, 'h-3.5 w-3.5') : ''}<span>${h(item.label)}</span>
            </button>`;
        }).join('');
        return `
            <div class="action-menu" data-menu="${id}">
                <button class="icon-btn" type="button" data-menu-trigger="${id}" aria-haspopup="menu" aria-label="Hành động">${icon('more-horizontal')}</button>
                <div class="action-menu-pop" data-menu-pop="${id}" role="menu">${buttons}</div>
            </div>`;
    }

    function bindActionMenus(scope = document) {
        scope.querySelectorAll('[data-menu-trigger]').forEach(trigger => {
            if (trigger.dataset.bound === '1') return;
            trigger.dataset.bound = '1';
            trigger.addEventListener('click', event => {
                event.stopPropagation();
                const id = trigger.dataset.menuTrigger;
                const wrapper = trigger.closest('.action-menu');
                document.querySelectorAll('.action-menu.open').forEach(other => {
                    if (other !== wrapper) {
                        other.classList.remove('open');
                        other.closest('tr')?.classList.remove('menu-open');
                    }
                });
                wrapper.classList.toggle('open');
                wrapper.closest('tr')?.classList.toggle('menu-open', wrapper.classList.contains('open'));
            });
        });
        if (!bindActionMenus.global) {
            document.addEventListener('click', () => {
                document.querySelectorAll('.action-menu.open').forEach(menu => {
                    menu.classList.remove('open');
                    menu.closest('tr')?.classList.remove('menu-open');
                });
            });
            bindActionMenus.global = true;
        }
    }

    function openCommandPalette() {
        const root = document.getElementById('cmdkRoot');
        if (!root) return;
        const grouped = navItems.reduce((acc, item) => {
            (acc[item.group] = acc[item.group] || []).push(item);
            return acc;
        }, {});
        const items = navItems.map((item, index) => ({
            id: item.id,
            label: item.label,
            href: item.href,
            icon: item.icon,
            group: item.group,
            keywords: normalize(`${item.label} ${item.group} ${item.keywords || ''}`),
            originalIndex: index
        }));
        let activeIndex = 0;
        const renderList = (query = '') => {
            const filtered = items.filter(item => !query || matchesSearch(item.keywords, query));
            if (activeIndex >= filtered.length) activeIndex = Math.max(0, filtered.length - 1);
            const groups = filtered.reduce((acc, item) => {
                (acc[item.group] = acc[item.group] || []).push(item);
                return acc;
            }, {});
            let counter = 0;
            const html = Object.entries(groups).map(([group, list]) => {
                return `<div class="cmdk-group">${h(group)}</div>` + list.map(item => {
                    const isActive = counter === activeIndex ? ' active' : '';
                    const node = `<button class="cmdk-item${isActive}" type="button" data-cmd-index="${counter}" data-cmd-href="${h(item.href)}">
                        ${icon(item.icon, 'h-4 w-4')}<span>${h(item.label)}</span>
                        <span class="cmdk-sub">${h(item.group)}</span>
                    </button>`;
                    counter += 1;
                    return node;
                }).join('');
            }).join('');
            return { html: html || `<div class="empty" style="border:none;background:transparent;padding:1.5rem">Không tìm thấy.</div>`, count: filtered.length };
        };
        const update = query => {
            const list = document.getElementById('cmdkList');
            const result = renderList(query);
            list.innerHTML = result.html;
            refreshIcons();
            list.querySelectorAll('[data-cmd-index]').forEach(button => {
                button.addEventListener('mouseenter', () => {
                    activeIndex = Number(button.dataset.cmdIndex);
                    list.querySelectorAll('.cmdk-item').forEach(item => item.classList.remove('active'));
                    button.classList.add('active');
                });
                button.addEventListener('click', () => {
                    window.location.href = button.dataset.cmdHref;
                });
            });
            return result.count;
        };
        const close = () => {
            root.classList.remove('open');
            root.innerHTML = '';
            document.removeEventListener('keydown', onKeyDown);
        };
        const onKeyDown = event => {
            if (event.key === 'Escape') {
                event.preventDefault();
                close();
                return;
            }
            const list = document.getElementById('cmdkList');
            if (!list) return;
            const buttons = list.querySelectorAll('.cmdk-item');
            if (event.key === 'ArrowDown') {
                event.preventDefault();
                activeIndex = Math.min(buttons.length - 1, activeIndex + 1);
                buttons.forEach((button, idx) => button.classList.toggle('active', idx === activeIndex));
                buttons[activeIndex]?.scrollIntoView({ block: 'nearest' });
            } else if (event.key === 'ArrowUp') {
                event.preventDefault();
                activeIndex = Math.max(0, activeIndex - 1);
                buttons.forEach((button, idx) => button.classList.toggle('active', idx === activeIndex));
                buttons[activeIndex]?.scrollIntoView({ block: 'nearest' });
            } else if (event.key === 'Enter') {
                event.preventDefault();
                const target = buttons[activeIndex];
                if (target) window.location.href = target.dataset.cmdHref;
            }
        };
        root.innerHTML = `
            <div class="cmdk" role="dialog" aria-modal="true" aria-label="Command palette">
                <div class="cmdk-input">
                    ${icon('search', 'h-4 w-4')}
                    <input type="search" id="cmdkInput" placeholder="Nhập tên trang hoặc chức năng..." autocomplete="off">
                </div>
                <div class="cmdk-list" id="cmdkList"></div>
                <div class="cmdk-foot">
                    <span><kbd>↑</kbd> <kbd>↓</kbd> di chuyển</span>
                    <span><kbd>Enter</kbd> mở</span>
                    <span><kbd>Esc</kbd> đóng</span>
                </div>
            </div>`;
        root.classList.add('open');
        update('');
        const input = document.getElementById('cmdkInput');
        input.addEventListener('input', event => {
            activeIndex = 0;
            update(event.target.value);
        });
        root.addEventListener('click', event => {
            if (event.target === root) close();
        });
        document.addEventListener('keydown', onKeyDown);
        window.setTimeout(() => input.focus(), 40);
        refreshIcons();
    }

    function sortableTable(headers, rows, sortKey, options = {}) {
        const stateKey = options.stateKey;
        const onSort = options.onSort;
        const current = stateKey ? (state.filters[`${stateKey}__sort`] || { key: '', dir: 'asc' }) : { key: '', dir: 'asc' };
        if (!rows.length) return options.emptyHtml || emptyState({ icon: 'inbox', title: 'Chưa có dữ liệu' });
        const head = headers.map((header, index) => {
            const sortable = header.sortKey ? ` sortable ${current.key === header.sortKey ? current.dir : ''}` : '';
            const mark = header.sortKey
                ? `<span class="sort-mark">${current.key === header.sortKey ? (current.dir === 'asc' ? '▲' : '▼') : '⇅'}</span>`
                : '';
            return `<th class="${sortable}" ${header.sortKey ? `data-sort-key="${h(header.sortKey)}"` : ''}>${h(header.label)}${mark}</th>`;
        }).join('');
        const html = `
            <div class="table-panel">
                <div class="table-scroll">
                    <table>
                        <thead><tr>${head}</tr></thead>
                        <tbody>${rows.join('')}</tbody>
                    </table>
                </div>
            </div>`;
        if (stateKey && onSort) {
            window.setTimeout(() => {
                document.querySelectorAll('th.sortable[data-sort-key]').forEach(th => {
                    th.addEventListener('click', () => {
                        const key = th.dataset.sortKey;
                        const dir = current.key === key && current.dir === 'asc' ? 'desc' : 'asc';
                        state.filters[`${stateKey}__sort`] = { key, dir };
                        onSort(key, dir);
                    });
                });
            }, 0);
        }
        return html;
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
            const value = event.target.value;
            render(value, items);
            const restored = document.getElementById(id);
            if (restored) {
                restored.value = value;
                restored.focus();
                restored.setSelectionRange(value.length, value.length);
            }
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

    function pageItems(key, items, pageSize = 10) {
        const pages = Math.max(1, Math.ceil(items.length / pageSize));
        const pageKey = `${key}Page`;
        const page = Math.min(Math.max(Number(state.filters[pageKey] || 1), 1), pages);
        const start = (page - 1) * pageSize;
        const visible = items.slice(start, start + pageSize);
        state.filters[pageKey] = page;
        return { page, pages, visible, total: items.length };
    }

    function bindPagination(key, pager, render, args = []) {
        document.querySelectorAll(`[data-pagination="${key}"] [data-page-step]`).forEach(button => {
            button.onclick = () => {
                state.filters[`${key}Page`] = Math.min(Math.max(pager.page + Number(button.dataset.pageStep), 1), pager.pages);
                render(...args);
            };
        });
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
        const [overview, events, proposals] = await Promise.all([
            load('/overview', {}),
            load('/events', []),
            load('/proposals', [])
        ]);
        const stats = overview.stats || {};
        const reports = overview.reports || {};
        const logs = overview.activityLogs || [];
        const pending = proposals.filter(item => ['PENDING', 'REVISION'].includes(String(item.status).toUpperCase()));
        const upcoming = events.filter(item => new Date(item.startTime) >= new Date()).slice(0, 6);
        content(`
            <div class="metric-grid">
                ${metric('Tổng user', number(stats.totalUsers), `${number(stats.activeUsers)} active · ${number(stats.lockedUsers)} locked`, 'blue', 'users')}
                ${metric('Events', number(stats.totalEvents), `${number(stats.todayEvents)} hôm nay · ${number(stats.upcomingEvents)} sắp tới`, 'orange', 'calendar-days')}
                ${metric('Registrations', number(stats.totalRegistrations), `${number(stats.attendanceCount)} attendance`, 'teal', 'clipboard-check')}
                ${metric('Feedback', `${Number(stats.averageRating || reports.averageRating || 0).toLocaleString('vi-VN', { maximumFractionDigits: 1 })}/5`, `${number(stats.totalFeedback)} phản hồi`, 'green', 'star')}
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
                    <div class="panel-header">
                        <h2 class="panel-title">Cần xử lý</h2>
                        <a class="btn" href="proposals.html">Xem tất cả</a>
                    </div>
                    <div class="action-list">
                        <a class="action-card" href="proposals.html">
                            <span class="action-icon tone-blue">${icon('file-text')}</span>
                            <span><strong>Pending proposals</strong><b>${number(pending.length)}</b><small>Proposal chờ duyệt hoặc cần chỉnh sửa</small></span>
                            ${icon('chevron-right')}
                        </a>
                        <a class="action-card" href="registrations.html">
                            <span class="action-icon tone-orange">${icon('clock-3')}</span>
                            <span><strong>Waitlist</strong><b>${number(stats.waitlistRegistrations)}</b><small>Sinh viên đang trong hàng chờ</small></span>
                            ${icon('chevron-right')}
                        </a>
                        <a class="action-card" href="email.html">
                            <span class="action-icon tone-rose">${icon('mail-warning')}</span>
                            <span><strong>Email failed</strong><b>${number(stats.failedEmails)}</b><small>Lỗi gửi thông báo</small></span>
                            ${icon('chevron-right')}
                        </a>
                    </div>
                </section>
            </div>
            <div class="split-grid">
                <section class="panel">
                    <div class="panel-header">
                        <h2 class="panel-title">Upcoming Events</h2>
                        <a class="btn" href="events.html">${icon('calendar-days')}Quản lý event</a>
                    </div>
                    ${upcoming.length ? table(['Event', 'Khoa', 'Thời gian', 'Tỷ lệ lấp đầy'], upcoming.map(event => `
                        <tr>
                            <td><span class="event-cell"><img class="event-thumb" src="${h(eventImageUrl(event))}" alt=""><span><span class="cell-title">${h(event.title)}</span><span class="cell-sub">${h(event.location || 'N/A')}</span></span></span></td>
                            <td>${h(event.departmentName)}</td>
                            <td>${dateTime(event.startTime)}</td>
                            <td><div class="progress"><span style="width:${Math.min(100, Number(event.fillRate || 0))}%"></span></div><span class="cell-sub">${percent(event.fillRate)}</span></td>
                        </tr>`)) : emptyState({
                            icon: 'calendar-x-2',
                            title: 'Chưa có event sắp tới',
                            copy: 'Khi có proposal được duyệt và công bố thành event, danh sách sẽ xuất hiện ở đây.',
                            actions: `<a class="btn primary" href="proposals.html">${icon('clipboard-list')}Mở Đề xuất</a>`
                        })}
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
                { title: 'Feedback / Reports / Email', status: 'Analytics', tone: 'blue', copy: 'Phân tích rating, export báo cáo và theo dõi lịch sử gửi email.' }
            ])}
        `);
    }

    function renderTimeline(logs) {
        if (!logs.length) return emptyState({ icon: 'history', title: 'Chưa có hoạt động' });
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
            const filtered = logs.filter(log => !q || matchesSearch(`${log.activityType} ${log.description} ${log.userName} ${log.userEmail}`, q));
            const pager = pageItems('logs', filtered, 10);
            content(`
                <div class="metric-grid">
                    ${metric('Total logs', number(payload.totalItems || logs.length), 'Activity logs')}
                    ${metric('Point earned', number(logs.reduce((sum, log) => sum + Number(log.pointsEarned || 0), 0)), 'Tổng điểm trong trang')}
                    ${metric('Users involved', number(new Set(logs.map(log => log.userEmail).filter(Boolean)).size), 'User có hoạt động')}
                    ${metric('Types', number(new Set(logs.map(log => log.activityType).filter(Boolean)).size), 'Loại hoạt động')}
                </div>
                <div class="toolbar">
                    ${searchBox('logSearch', 'Tìm activity, user, mô tả...')}
                    ${pagination('logs', pager.page, pager.pages, pager.total, pager.visible.length)}
                </div>
                ${table(['Thời gian', 'Loại', 'User', 'Mô tả', 'Điểm'], pager.visible.map(log => `
                    <tr>
                        <td>${dateTime(log.createdAt)}</td>
                        <td>${badge(log.activityType || 'N/A', 'blue')}</td>
                        <td><span class="cell-title">${h(log.userName || 'N/A')}</span><span class="cell-sub">${h(log.userEmail || '')}</span></td>
                        <td>${h(log.description || '')}</td>
                        <td>${number(log.pointsEarned)}</td>
                    </tr>`))}
            `);
            const search = document.getElementById('logSearch');
            if (search) {
                search.value = q;
                search.addEventListener('input', event => {
                    state.filters.logsPage = 1;
                    render(event.target.value);
                });
            }
            bindPagination('logs', pager, render, [q]);
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
        const roles = await load('/roles', []);
        let users = [];
        let renderToken = 0;
        let searchTimer = null;
        const managerRole = roles.find(role => role.name === 'MANAGER') || roles.find(role => role.name === 'DEPARTMENT');
        const safeRoleOptions = roles
            .filter(role => role.name !== 'ADMIN')
            .map(role => ({ value: role.id, label: role.name }));
        const statusOptions = [{ value: 'true', label: 'ACTIVE' }, { value: 'false', label: 'LOCKED' }];
        const departmentPositionOptions = [
            { value: 'STAFF', label: 'Nhân sự khoa/Bộ môn' },
            { value: 'HEAD', label: 'Trưởng khoa/Bộ môn' }
        ];

        const roleNameById = id => {
            const role = roles.find(item => String(item.id) === String(id));
            return role && role.name ? role.name.toUpperCase() : '';
        };
        const isStudentRole = id => roleNameById(id) === 'STUDENT';
        const isStaffRole = id => ['MANAGER', 'DEPARTMENT'].includes(roleNameById(id));

        const openUserForm = (user = {}) => {
            const roleOptions = user.role === 'ADMIN'
                ? roles.map(role => ({ value: role.id, label: role.name }))
                : safeRoleOptions;
            return openForm({
            title: user.id ? 'Edit User' : 'Create User',
            fields: [
                { name: 'fullName', label: 'Họ tên', required: true },
                { name: 'email', label: 'Email', type: 'email', required: true },
                { name: 'phone', label: 'Số điện thoại', required: true },
                { name: 'roleId', label: 'Role', type: 'select', options: roleOptions, required: true },
                { name: 'departmentPosition', label: 'Chức vụ khoa/Bộ môn', type: 'select', options: departmentPositionOptions, visible: v => isStaffRole(v.roleId) },
                { name: 'active', label: 'Trạng thái', type: 'select', options: statusOptions },
                { name: 'password', label: user.id ? 'Mật khẩu mới' : 'Mật khẩu', type: 'password', required: !user.id },
                { name: 'major', label: 'Khoa/Major', required: true, visible: v => isStudentRole(v.roleId) || isStaffRole(v.roleId) },
                { name: 'semester', label: 'Kỳ/Năm', type: 'number', visible: v => isStudentRole(v.roleId) },
                { name: 'studentCode', label: 'Mã sinh viên', required: true, visible: v => isStudentRole(v.roleId) },
                { name: 'totalPoints', label: 'Điểm', type: 'number', visible: v => isStudentRole(v.roleId) }
            ],
            values: { ...user, roleId: user.roleId || managerRole?.id || safeRoleOptions[0]?.value || '', departmentPosition: user.departmentPosition || 'STAFF', active: user.active === false ? 'false' : 'true' },
            validate: vals => {
                const errors = {};
                if (vals.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(vals.email.trim())) {
                    errors.email = 'Email không hợp lệ.';
                }
                if (vals.phone && !/^[0-9]{8,15}$/.test(vals.phone.replace(/\s+/g, ''))) {
                    errors.phone = 'Số điện thoại chỉ gồm 8–15 chữ số.';
                }
                if (isStudentRole(vals.roleId)) {
                    const semester = Number(vals.semester);
                    if (vals.semester !== '' && vals.semester != null && (!Number.isFinite(semester) || semester < 1)) {
                        errors.semester = 'Kỳ/Năm phải là số ≥ 1.';
                    }
                }
                return errors;
            },
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
        };

        const detail = user => openDetail('User Detail', `
            ${detailGrid([
                ['Họ tên', user.fullName],
                ['Email', user.email],
                ['Phone', user.phone],
                ['Role', user.role],
                ['Chức vụ khoa/Bộ môn', user.departmentPositionLabel || user.departmentPosition || 'STAFF'],
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

        const userInitials = user => String(user.fullName || user.email || 'U')
            .split(/\s+/)
            .filter(Boolean)
            .slice(-2)
            .map(part => part[0])
            .join('')
            .toUpperCase();
        const userFaculty = user => facultyOfDepartment(user.major || user.departmentName || '');
        const uniqueOptions = values => [...new Set(values.filter(Boolean))].sort((a, b) => a.localeCompare(b, 'vi'));
        const roleFilterOptions = [{ value: 'all', label: 'Tất cả role' }]
            .concat(uniqueOptions(roles.map(role => role.name)).map(value => ({ value, label: value })));
        const facultyFilterOptions = [{ value: 'all', label: 'Tất cả khoa/bộ môn' }]
            .concat(academicStructure.map(item => ({ value: item.faculty, label: item.faculty })));
        const sortOptions = [
            { value: 'role', label: 'Sort: Role' },
            { value: 'faculty', label: 'Sort: Khoa/Bộ môn' },
            { value: 'name', label: 'Sort: Tên A-Z' },
            { value: 'points', label: 'Sort: Điểm cao' },
            { value: 'created', label: 'Sort: Mới tạo' }
        ];

        const fetchUsers = q => {
            const params = new URLSearchParams({
                page: String(Math.max(Number(state.filters.usersPage || 1), 1) - 1),
                size: '25',
                sort: state.filters.userSort || 'role'
            });
            const roleFilter = state.filters.userRole || 'all';
            const facultyFilter = state.filters.userFaculty || 'all';
            if (q) params.set('q', q);
            if (roleFilter !== 'all') params.set('role', roleFilter);
            if (facultyFilter !== 'all') params.set('faculty', facultyFilter);
            return load(`/users?${params.toString()}`, {
                items: [],
                page: 0,
                size: 25,
                totalItems: 0,
                totalPages: 1,
                metrics: {}
            });
        };

        const render = async (q = state.filters.userSearch || '', keepSearchFocus = false) => {
            const token = ++renderToken;
            state.filters.userSearch = q;
            const roleFilter = state.filters.userRole || 'all';
            const facultyFilter = state.filters.userFaculty || 'all';
            const sortKey = state.filters.userSort || 'role';
            const payload = await fetchUsers(q);
            if (token !== renderToken) return;
            users = payload.items || [];
            const metrics = payload.metrics || {};
            const filtered = users
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
            const pager = {
                page: Number(payload.page || 0) + 1,
                pages: Math.max(1, Number(payload.totalPages || 1)),
                total: Number(payload.totalItems || users.length),
                visible: filtered
            };
            state.filters.usersPage = pager.page;
            content(`
                <div class="user-stat-strip">
                    <div class="user-stat-item"><span>Users</span><strong>${number(metrics.totalUsers || pager.total)}</strong><small>${number(pager.visible.length)} trong trang</small></div>
                    <div class="user-stat-item tone-green"><span>Active</span><strong>${number(metrics.activeUsers)}</strong><small>Đang hoạt động</small></div>
                    <div class="user-stat-item tone-rose"><span>Locked</span><strong>${number(metrics.lockedUsers)}</strong><small>Tài khoản khóa</small></div>
                    <div class="user-stat-item tone-blue"><span>Roles</span><strong>${number(metrics.totalRoles || roles.length)}</strong><small>Vai trò đang có</small></div>
                </div>
                <section class="panel user-directory">
                    <div class="user-directory-head">
                        <div>
                            <h2 class="panel-title">User Directory</h2>
                            <p class="panel-note">Quản lý tài khoản, role, trạng thái và thao tác nhanh.</p>
                        </div>
                        ${pagination('users', pager.page, pager.pages, pager.total, pager.visible.length)}
                    </div>
                    <div class="user-directory-toolbar">
                        ${searchBox('userSearch', 'Tìm tên, email, role, khoa...')}
                        ${selectBox('userRoleFilter', roleFilterOptions)}
                        ${selectBox('userFacultyFilter', facultyFilterOptions)}
                        ${selectBox('userSort', sortOptions)}
                    </div>
                    ${table(['User', 'Role', 'Chức vụ', 'Khoa', 'Status', 'Điểm', 'Hành động'], pager.visible.map(user => `
                    <tr class="user-row">
                        <td>
                            <span class="user-cell">
                                <span class="user-avatar">${h(userInitials(user))}</span>
                                <span><span class="cell-title">${h(user.fullName)}</span><span class="cell-sub">${h(user.email)}</span></span>
                            </span>
                        </td>
                        <td>${badge(user.role || 'N/A', tone(user.role))}</td>
                        <td>${badge(user.departmentPositionLabel || user.departmentPosition || 'STAFF', user.departmentPosition === 'HEAD' ? 'green' : 'gray')}</td>
                        <td>${h(user.major || 'N/A')}<span class="cell-sub">${h(userFaculty(user))}${user.studentCode ? ' · ' + h(user.studentCode) : ''}</span></td>
                        <td>${badge(user.status, tone(user.status))}</td>
                        <td>${number(user.totalPoints)}</td>
                        <td><div class="row-actions">
                            <button class="icon-btn" data-detail="${user.id}" title="Xem chi tiết">${icon('eye')}</button>
                            <button class="icon-btn" data-edit="${user.id}" title="Chỉnh sửa">${icon('pencil')}</button>
                            <div class="action-menu" data-menu="user-${user.id}">
                                <button class="icon-btn" type="button" data-menu-trigger="user-${user.id}" aria-label="Thêm thao tác" title="Thêm thao tác">${icon('more-horizontal')}</button>
                                <div class="action-menu-pop" data-menu-pop="user-${user.id}" role="menu">
                                    <button class="action-item" type="button" data-lock="${user.id}">${icon(user.status === 'LOCKED' ? 'unlock' : 'lock', 'h-3.5 w-3.5')}<span>${user.status === 'LOCKED' ? 'Mở khóa tài khoản' : 'Khóa tài khoản'}</span></button>
                                    <button class="action-item" type="button" data-reset="${user.id}">${icon('key-round', 'h-3.5 w-3.5')}<span>Đặt lại mật khẩu</span></button>
                                    <div class="action-divider"></div>
                                    <button class="action-item danger" type="button" data-delete="${user.id}">${icon('trash-2', 'h-3.5 w-3.5')}<span>Xóa tài khoản</span></button>
                                </div>
                            </div>
                        </div></td>
                    </tr>`))}
                </section>
            `);
            const search = document.getElementById('userSearch');
            const roleSelect = document.getElementById('userRoleFilter');
            const facultySelect = document.getElementById('userFacultyFilter');
            const sortSelect = document.getElementById('userSort');
            if (search) search.value = q;
            if (roleSelect) roleSelect.value = roleFilter;
            if (facultySelect) facultySelect.value = facultyFilter;
            if (sortSelect) sortSelect.value = sortKey;
            if (search && keepSearchFocus) {
                search.focus();
                search.setSelectionRange(q.length, q.length);
            }
            if (search) search.addEventListener('input', event => {
                clearTimeout(searchTimer);
                searchTimer = setTimeout(() => {
                    state.filters.usersPage = 1;
                    render(event.target.value, true);
                }, 250);
            });
            if (roleSelect) roleSelect.addEventListener('change', event => {
                state.filters.userRole = event.target.value;
                state.filters.usersPage = 1;
                render();
            });
            if (facultySelect) facultySelect.addEventListener('change', event => {
                state.filters.userFaculty = event.target.value;
                state.filters.usersPage = 1;
                render();
            });
            if (sortSelect) sortSelect.addEventListener('change', event => {
                state.filters.userSort = event.target.value;
                state.filters.usersPage = 1;
                render();
            });
            bindPagination('users', pager, render);
            bindActionMenus();
            document.querySelectorAll('[data-detail]').forEach(button => button.onclick = () => detail(users.find(user => String(user.id) === button.dataset.detail)));
            document.querySelectorAll('[data-edit]').forEach(button => button.onclick = () => openUserForm(users.find(user => String(user.id) === button.dataset.edit)));
            document.querySelectorAll('[data-lock]').forEach(button => button.onclick = () => toggleUser(users.find(user => String(user.id) === button.dataset.lock)));
            document.querySelectorAll('[data-reset]').forEach(button => button.onclick = () => resetPassword(users.find(user => String(user.id) === button.dataset.reset)));
            document.querySelectorAll('[data-delete]').forEach(button => button.onclick = () => confirmAction('Xóa user này? Nếu có dữ liệu liên quan, hệ thống sẽ khóa thay vì xóa vật lý.', async () => {
                await api(`/users/${button.dataset.delete}`, { method: 'DELETE' });
                toast('Đã xử lý user.', 'success');
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
        const roleOptions = roles
            .filter(role => role.name !== 'ADMIN')
            .map(role => ({ value: role.id, label: role.name }));
        const rolePager = pageItems('roles', roles, 10);
        const maxRoleUsers = Math.max(1, ...roles.map(role => Number(role.userCount || 0)));
        const roleIcon = roleName => ({
            ADMIN: 'shield-check',
            COMMITTEE: 'clipboard-check',
            DEPARTMENT: 'building-2',
            MANAGER: 'briefcase-business',
            STUDENT: 'graduation-cap'
        }[String(roleName || '').toUpperCase()] || 'key-round');

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

        content(`
            <div class="metric-grid">
                ${metric('Roles', number(roles.length), 'Role List', 'blue', 'shield-check')}
                ${metric('Người dùng', number(users.length), 'User đang có role', 'teal', 'users')}
                ${metric('Admin', number(users.filter(user => user.role === 'ADMIN').length), 'Quản trị viên', 'orange', 'user-cog')}
                ${metric('Sinh viên', number(users.filter(user => user.role === 'STUDENT').length), 'Tài khoản sinh viên', 'green', 'graduation-cap')}
            </div>
            <section class="role-directory">
                <div class="role-directory-head">
                    <div>
                        <h2 class="panel-title">Danh sách phân quyền</h2>
                        <p class="panel-note">Hiển thị ${number(rolePager.visible.length)} / ${number(rolePager.total)} vai trò đang có trong hệ thống.</p>
                    </div>
                    <div class="toolbar">${pagination('roles', rolePager.page, rolePager.pages, rolePager.total, rolePager.visible.length)}</div>
                </div>
                <div class="role-grid">
                    ${rolePager.visible.map(role => {
                        const count = Number(role.userCount || 0);
                        const roleTone = tone(role.name);
                        const share = Math.max(4, Math.round((count / maxRoleUsers) * 100));
                        return `
                            <article class="role-card tone-${roleTone}">
                                <div class="role-card-top">
                                    <span class="role-mark">${icon(roleIcon(role.name), 'h-5 w-5')}</span>
                                    <div class="role-title-block">
                                        <h3>${h(role.name)}</h3>
                                        ${badge(role.name, roleTone)}
                                    </div>
                                    <div class="row-actions role-actions">
                                        <button class="icon-btn" data-edit-role="${role.id}" title="Chỉnh sửa role">${icon('pencil')}</button>
                                        <button class="icon-btn danger" data-delete-role="${role.id}" title="Xóa role">${icon('trash-2')}</button>
                                    </div>
                                </div>
                                <p class="role-description">${h(role.description || 'Chưa có mô tả cho vai trò này.')}</p>
                                <div class="role-card-bottom">
                                    <div>
                                        <span class="role-meta-label">Người dùng</span>
                                        <strong>${number(count)}</strong>
                                    </div>
                                    <div class="role-share" aria-label="${number(count)} người dùng">
                                        <span style="width:${share}%"></span>
                                    </div>
                                </div>
                            </article>`;
                    }).join('')}
                </div>
            </section>
        `);
        document.getElementById('addRole').onclick = () => openRoleForm();
        document.getElementById('assignRole').onclick = assignRole;
        bindPagination('roles', rolePager, renderRoles);
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
        const [departments, users, roles] = await Promise.all([load('/departments', []), load('/users', []), load('/roles', [])]);
        const managerRole = roles.find(role => role.name === 'MANAGER') || roles.find(role => role.name === 'DEPARTMENT');
        const managerCandidates = users.filter(user => user.role !== 'ADMIN');
        const managerFor = department =>
            users.find(user => String(user.id) === String(department.managerId))
            || users.find(user => ['MANAGER', 'DEPARTMENT'].includes(user.role) && user.departmentPosition === 'HEAD' && normalize(user.major) === normalize(department.name))
            || users.find(user => ['MANAGER', 'DEPARTMENT'].includes(user.role) && normalize(user.major) === normalize(department.name));
        const personInitials = person => String(person?.fullName || person?.email || 'U').trim().split(/\s+/).slice(0, 2).map(part => part[0] || '').join('').toUpperCase();
        const facultyInitials = name => {
            const parts = String(name || '').trim().split(/\s+/).filter(Boolean);
            if (parts.length >= 2) return parts.slice(0, 2).map(part => part[0]).join('').toUpperCase();
            return String(name || 'K').slice(0, 2).toUpperCase();
        };

        const facultyFilter = state.filters.departmentFaculty || 'all';
        const searchQuery = state.filters.departmentSearch || '';
        const facultyNames = [...new Set(departments.map(item => item.facultyName || facultyOfDepartment(item.name)))].sort((a, b) => a.localeCompare(b, 'vi'));
        const facultyFilterOptions = [{ value: 'all', label: 'Tất cả khoa' }].concat(facultyNames.map(name => ({ value: name, label: name })));

        const filteredDepartments = departments.filter(department => {
            const faculty = department.facultyName || facultyOfDepartment(department.name);
            if (facultyFilter !== 'all' && faculty !== facultyFilter) return false;
            if (searchQuery) {
                const manager = managerFor(department);
                const haystack = [department.name, faculty, department.description, manager?.fullName, manager?.email].join(' ');
                if (!matchesSearch(haystack, searchQuery)) return false;
            }
            return true;
        });

        const facultyRows = Object.entries(filteredDepartments.reduce((acc, department) => {
            const faculty = department.facultyName || facultyOfDepartment(department.name);
            if (!acc[faculty]) acc[faculty] = [];
            acc[faculty].push(department);
            return acc;
        }, {}))
            .map(([faculty, units]) => ({
                faculty,
                units: units.sort((a, b) => String(a.name || '').localeCompare(String(b.name || ''), 'vi')),
                events: units.reduce((sum, item) => sum + Number(item.eventCount || 0), 0),
                proposals: units.reduce((sum, item) => sum + Number(item.proposalCount || 0), 0),
                students: units.reduce((sum, item) => sum + Number(item.studentCount || 0), 0),
                managers: units.filter(item => managerFor(item)).length
            }))
            .sort((a, b) => a.faculty.localeCompare(b.faculty, 'vi'));

        const departmentPager = pageItems('departments', facultyRows, 4);
        const expandFaculties = !!searchQuery || facultyFilter !== 'all' || facultyRows.length <= 2;
        const totalFacultyCount = new Set(departments.map(item => item.facultyName || facultyOfDepartment(item.name))).size;
        const totalEvents = departments.reduce((sum, item) => sum + Number(item.eventCount || 0), 0);
        const totalStudents = departments.reduce((sum, item) => sum + Number(item.studentCount || 0), 0);
        const assignedManagers = departments.filter(item => managerFor(item)).length;

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
            fields: [{ name: 'managerId', label: 'Manager', type: 'select', options: managerCandidates.map(user => ({ value: user.id, label: `${user.fullName} - ${user.email}` })) }],
            values: { managerId: department.managerId || managerFor(department)?.id || '' },
            onSubmit: async payload => {
                const selected = users.find(user => String(user.id) === String(payload.managerId));
                if (!selected) throw new Error('Vui lòng chọn manager.');
                await api(`/users/${selected.id}`, {
                    method: 'PUT',
                    body: JSON.stringify({
                        ...selected,
                        roleId: managerRole?.id || selected.roleId,
                        departmentPosition: 'HEAD',
                        major: department.name,
                        active: true
                    })
                });
                toast('Đã gán manager cho bộ môn.');
                renderDepartments();
            }
        });

        const facultyCards = departmentPager.visible.map((group, index) => {
            const isOpen = expandFaculties || index === 0;
            return `
            <section class="dept-faculty-card dept-accent-${index % 5}${isOpen ? ' is-open' : ''}">
                <button type="button" class="dept-faculty-toggle" data-faculty-toggle="${h(group.faculty)}" aria-expanded="${isOpen ? 'true' : 'false'}">
                    <span class="dept-faculty-mark">${h(facultyInitials(group.faculty))}</span>
                    <span class="dept-faculty-copy">
                        <span class="dept-faculty-title">${h(group.faculty)}</span>
                        <span class="dept-faculty-meta">${number(group.units.length)} bộ môn · ${number(group.managers)}/${number(group.units.length)} đã có trưởng đơn vị</span>
                    </span>
                    <span class="dept-faculty-summary">
                        <span class="dept-summary-item"><strong>${number(group.events)}</strong> sự kiện</span>
                        <span class="dept-summary-item"><strong>${number(group.students)}</strong> sinh viên</span>
                    </span>
                    ${icon('chevron-down', 'dept-faculty-chevron h-4 w-4')}
                </button>
                <div class="dept-faculty-body">
                    <div class="dept-list-head" aria-hidden="true">
                        <span>Bộ môn / ngành</span>
                        <span>Trưởng đơn vị</span>
                        <span>Số liệu</span>
                        <span>Thao tác</span>
                    </div>
                    <div class="dept-list">
                        ${group.units.map(department => {
                            const manager = managerFor(department);
                            return `
                            <article class="dept-row">
                                <div class="dept-row-main">
                                    <h3 class="dept-unit-title">${h(department.name)}</h3>
                                    <p class="dept-unit-desc">${h(department.description || `Thuộc ${group.faculty}`)}</p>
                                </div>
                                <div class="dept-row-manager">
                                    ${manager
                                        ? `<span class="dept-manager-avatar">${h(personInitials(manager))}</span>
                                           <span class="dept-manager-copy">
                                               <span class="cell-title">${h(manager.fullName)}</span>
                                               <span class="cell-sub">${h(manager.email)}</span>
                                           </span>`
                                        : `<span class="dept-manager-empty">${icon('user-round-plus', 'h-4 w-4')}<span>Chưa gán</span></span>`}
                                </div>
                                <div class="dept-row-metrics">
                                    <span class="dept-metric"><em>Sự kiện</em><strong>${number(department.eventCount)}</strong></span>
                                    <span class="dept-metric"><em>Đề xuất</em><strong>${number(department.proposalCount)}</strong></span>
                                    <span class="dept-metric"><em>Sinh viên</em><strong>${number(department.studentCount)}</strong></span>
                                </div>
                                <div class="dept-row-actions row-actions">
                                    <button class="icon-btn" data-edit-department="${department.id}" title="Chỉnh sửa">${icon('pencil')}</button>
                                    <button class="icon-btn" data-manager="${department.id}" title="Gán trưởng đơn vị">${icon('user-check')}</button>
                                    <button class="icon-btn danger" data-delete-department="${department.id}" title="Xóa">${icon('trash-2')}</button>
                                </div>
                            </article>`;
                        }).join('')}
                    </div>
                </div>
            </section>`;
        }).join('');

        content(`
            <div class="dept-stat-strip">
                <div class="dept-stat-item tone-blue">
                    ${icon('building-2', 'h-4 w-4')}
                    <div><span>Khoa lớn</span><strong>${number(totalFacultyCount)}</strong><small>Nhóm tổ chức</small></div>
                </div>
                <div class="dept-stat-item tone-orange">
                    ${icon('layers-3', 'h-4 w-4')}
                    <div><span>Bộ môn / ngành</span><strong>${number(departments.length)}</strong><small>${number(filteredDepartments.length)} đang hiển thị</small></div>
                </div>
                <div class="dept-stat-item tone-green">
                    ${icon('user-check', 'h-4 w-4')}
                    <div><span>Trưởng đơn vị</span><strong>${number(assignedManagers)}</strong><small>/${number(departments.length)} đã gán</small></div>
                </div>
                <div class="dept-stat-item tone-purple">
                    ${icon('graduation-cap', 'h-4 w-4')}
                    <div><span>Sinh viên</span><strong>${number(totalStudents)}</strong><small>${number(totalEvents)} sự kiện</small></div>
                </div>
            </div>
            <section class="panel dept-directory">
                <div class="dept-directory-head">
                    <div>
                        <h2 class="panel-title">Danh sách khoa & bộ môn</h2>
                        <p class="panel-note">Bấm tên khoa để mở/đóng danh sách bộ môn bên trong.</p>
                    </div>
                    ${pagination('departments', departmentPager.page, departmentPager.pages, departmentPager.total, departmentPager.visible.length)}
                </div>
                <div class="dept-directory-toolbar">
                    <label class="dept-filter-label">Tìm kiếm</label>
                    ${searchBox('departmentSearch', 'Tên bộ môn, khoa, email trưởng đơn vị...')}
                    <label class="dept-filter-label">Lọc khoa</label>
                    ${selectBox('departmentFacultyFilter', facultyFilterOptions)}
                </div>
                ${facultyRows.length
                    ? `<div class="department-tree">${facultyCards}</div>`
                    : emptyState({
                        icon: 'building-2',
                        title: 'Không tìm thấy bộ môn phù hợp',
                        copy: 'Thử đổi bộ lọc khoa hoặc từ khóa tìm kiếm.'
                    })}
            </section>
        `);

        document.getElementById('addDepartment').onclick = () => openDepartmentForm();
        bindPagination('departments', departmentPager, renderDepartments);
        const search = document.getElementById('departmentSearch');
        const facultySelect = document.getElementById('departmentFacultyFilter');
        if (search) {
            search.value = searchQuery;
            let searchTimer;
            search.addEventListener('input', event => {
                clearTimeout(searchTimer);
                searchTimer = setTimeout(() => {
                    state.filters.departmentSearch = event.target.value;
                    state.filters.departmentsPage = 1;
                    renderDepartments();
                }, 250);
            });
        }
        if (facultySelect) {
            facultySelect.value = facultyFilter;
            facultySelect.addEventListener('change', event => {
                state.filters.departmentFaculty = event.target.value;
                state.filters.departmentsPage = 1;
                renderDepartments();
            });
        }
        document.querySelectorAll('[data-faculty-toggle]').forEach(button => {
            button.addEventListener('click', () => {
                const card = button.closest('.dept-faculty-card');
                const opening = !card?.classList.contains('is-open');
                card?.classList.toggle('is-open', opening);
                button.setAttribute('aria-expanded', opening ? 'true' : 'false');
            });
        });
        document.querySelectorAll('[data-manager]').forEach(button => button.onclick = () => assignManager(departments.find(item => String(item.id) === button.dataset.manager)));
        document.querySelectorAll('[data-edit-department]').forEach(button => button.onclick = () => openDepartmentForm(departments.find(item => String(item.id) === button.dataset.editDepartment)));
        document.querySelectorAll('[data-delete-department]').forEach(button => button.onclick = () => confirmAction('Xóa bộ môn này?', async () => {
            await api(`/departments/${button.dataset.deleteDepartment}`, { method: 'DELETE' });
            toast('Đã xóa bộ môn.');
            renderDepartments();
        }));
    }

    async function renderProposals() {
        state.page = 'proposals';
        shell();
        const proposals = await load('/proposals', []);
        const statusCounts = summarizeStatuses(proposals);
        const statusOptions = ['PENDING', 'REVISION', 'REJECTED'].map(value => ({ value, label: value }));
        const needsReview = proposals.filter(item => ['PENDING', 'REVISION'].includes(String(item.status).toUpperCase()));
        const pager = pageItems('proposals', proposals, 10);

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
                ${metric('Auto publish', 'Bật', 'Duyệt xong tự lên sự kiện')}
                ${metric('Rejected', number(statusCounts.REJECTED), 'Không tiếp tục')}
            </div>
            <div class="toolbar">${pagination('proposals', pager.page, pager.pages, pager.total, pager.visible.length)}</div>
            ${table(['Proposal', 'Khoa', 'Ngày đề xuất', 'Status', 'Actions'], pager.visible.map(proposal => {
                return `<tr>
                    <td><span class="cell-title">${h(proposal.title)}</span><span class="cell-sub">${h(proposal.description || '')}</span></td>
                    <td>${h(proposal.departmentName)}</td>
                    <td>${dateTime(proposal.proposedDate)}</td>
                    <td>${badge(proposal.status, tone(proposal.status))}</td>
                    <td><div class="row-actions">
                        <button class="icon-btn" data-proposal-detail="${proposal.id}" title="Xem chi tiết">${icon('eye')}</button>
                        <div class="action-menu" data-menu="proposal-${proposal.id}">
                            <button class="icon-btn" type="button" data-menu-trigger="proposal-${proposal.id}" aria-label="Thêm thao tác" title="Thêm thao tác">${icon('more-horizontal')}</button>
                            <div class="action-menu-pop" data-menu-pop="proposal-${proposal.id}" role="menu">
                                <button class="action-item" type="button" data-proposal-status="${proposal.id}">${icon('list-checks', 'h-3.5 w-3.5')}<span>Đổi trạng thái</span></button>
                                <div class="action-divider"></div>
                                <button class="action-item danger" type="button" data-proposal-delete="${proposal.id}">${icon('trash-2', 'h-3.5 w-3.5')}<span>Xóa proposal</span></button>
                            </div>
                        </div>
                    </div></td>
                </tr>`;
            }))}
        `);
        bindActionMenus();
        bindPagination('proposals', pager, renderProposals);
        document.querySelectorAll('[data-proposal-detail]').forEach(button => button.onclick = () => {
            const proposal = proposals.find(item => String(item.id) === button.dataset.proposalDetail);
            openDetail('Proposal Detail', `${detailGrid([
                ['Title', proposal.title],
                ['Department', proposal.departmentName],
                ['Status', proposal.status],
                ['Proposed date', dateTime(proposal.proposedDate)],
                ['Organizer', proposal.organizer || 'N/A'],
                ['Speakers', proposal.speakers || 'N/A'],
                ['Created', dateTime(proposal.createdAt)],
                ['Note', proposal.note || 'N/A']
            ])}<div class="panel" style="margin-top:1rem"><p class="panel-note">${h(proposal.description || '')}</p></div>`);
        });
        document.querySelectorAll('[data-proposal-status]').forEach(button => button.onclick = () => updateStatus(proposals.find(item => String(item.id) === button.dataset.proposalStatus)));
        document.querySelectorAll('[data-proposal-delete]').forEach(button => button.onclick = () => confirmAction('Xóa proposal này?', async () => {
            await api(`/proposals/${button.dataset.proposalDelete}`, { method: 'DELETE' });
            toast('Đã xóa proposal.');
            renderProposals();
        }));
    }

    async function renderEvents() {
        state.page = 'events';
        const scope = currentEventScope();
        const rerender = () => renderEvents();
        shell();
        const [rawEvents, departments] = await Promise.all([load('/events', []), load('/departments', [])]);
        const events = [...rawEvents]
            .filter(item => {
                if (scope === 'active') return item.status !== 'COMPLETED';
                if (scope === 'completed') return item.status === 'COMPLETED';
                return true;
            })
            .sort(compareEventsByTime);
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
                { name: 'organizer', label: 'Người phụ trách' },
                { name: 'speakers', label: 'Diễn giả', type: 'textarea', full: true },
                { name: 'imageUrls', label: 'Ảnh sự kiện', type: 'image-picker', full: true },
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
                rerender();
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
                rerender();
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
                rerender();
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
            organizer: event.organizer || '',
            speakers: event.speakers || '',
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
                rerender();
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
                    options: rawEvents.map(item => ({ value: item.id, label: item.title }))
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
                rerender();
            }
        });

        const toggleFeatured = event => {
            featured[event.id] = !featured[event.id];
            localSet('featuredEvents', featured);
            toast(featured[event.id] ? 'Đã đưa vào Featured Events.' : 'Đã bỏ khỏi Featured Events.');
            rerender();
        };
        const pager = pageItems('events', events, 10);
        const activeCount = rawEvents.filter(item => item.status !== 'COMPLETED').length;
        const completedCount = rawEvents.filter(item => item.status === 'COMPLETED').length;
        const publishedInView = events.filter(item => item.status === 'PUBLISHED').length;
        const attendedInView = events.filter(item => Number(item.attendanceCount || 0) > 0).length;

        content(`
            <div class="metric-grid">
                ${metric(scope === 'completed' ? 'Đã kết thúc' : scope === 'active' ? 'Đang diễn ra' : 'Tất cả', number(events.length), scope === 'all' ? `${number(activeCount)} đang diễn ra · ${number(completedCount)} đã kết thúc` : scope === 'completed' ? 'Sự kiện đã qua ngày kết thúc' : 'Sự kiện chưa kết thúc')}
                ${metric(scope === 'completed' ? 'Có điểm danh' : 'Published', number(scope === 'completed' ? attendedInView : publishedInView), scope === 'completed' ? 'Có người tham dự' : 'Đang public')}
                ${metric('Committee', number(events.filter(item => committeeForEvent(item)).length), 'Có người phụ trách')}
                ${metric('Budget', money(events.reduce((sum, item) => sum + Number(item.budget || 0), 0)), 'Tổng ngân sách')}
            </div>
            <div class="toolbar">${pagination('events', pager.page, pager.pages, pager.total, pager.visible.length)}</div>
            ${table(['Event', 'Khoa', 'Committee', 'Thời gian', 'Capacity', 'Ngân sách', 'Status', 'Actions'], pager.visible.map(event => `
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
                    <td><div class="progress"><span style="width:${Math.min(100, Number(event.fillRate || 0))}%"></span></div><span class="cell-sub">${number(event.registeredCount ?? event.registrationCount)} / ${number(event.capacity)}${Number(event.waitlistCount || 0) > 0 ? ` · +${number(event.waitlistCount)} chờ` : ''}</span></td>
                    <td>${money(event.budget)}</td>
                    <td>${badge(event.status, tone(event.status))}${featured[event.id] ? ` ${badge('Featured', 'orange')}` : ''}</td>
                    <td><div class="row-actions">
                        <button class="icon-btn" data-event-detail="${event.id}" title="Xem chi tiết">${icon('eye')}</button>
                        <button class="icon-btn" data-event-edit="${event.id}" title="Chỉnh sửa event">${icon('pencil')}</button>
                        <div class="action-menu" data-menu="event-${event.id}">
                            <button class="icon-btn" type="button" data-menu-trigger="event-${event.id}" aria-label="Thêm thao tác" title="Thêm thao tác">${icon('more-horizontal')}</button>
                            <div class="action-menu-pop" data-menu-pop="event-${event.id}" role="menu">
                                <button class="action-item" type="button" data-event-committee="${event.id}">${icon('users-round', 'h-3.5 w-3.5')}<span>Gán hội đồng</span></button>
                                <button class="action-item" type="button" data-event-budget="${event.id}">${icon('coins', 'h-3.5 w-3.5')}<span>Thêm ngân sách</span></button>
                                <button class="action-item" type="button" data-event-capacity="${event.id}">${icon('gauge', 'h-3.5 w-3.5')}<span>Đổi sức chứa</span></button>
                                <button class="action-item" type="button" data-event-status="${event.id}">${icon('refresh-cw', 'h-3.5 w-3.5')}<span>Đổi trạng thái</span></button>
                                <button class="action-item" type="button" data-event-featured="${event.id}">${icon('star', 'h-3.5 w-3.5')}<span>${featured[event.id] ? 'Bỏ Featured' : 'Đưa vào Featured'}</span></button>
                                <div class="action-divider"></div>
                                <button class="action-item danger" type="button" data-event-delete="${event.id}">${icon('trash-2', 'h-3.5 w-3.5')}<span>Xóa / huỷ event</span></button>
                            </div>
                        </div>
                    </div></td>
                </tr>`))}
        `);
        bindActionMenus();
        bindPagination('events', pager, rerender);
        document.querySelectorAll('[data-event-detail]').forEach(button => button.onclick = () => {
            const event = events.find(item => String(item.id) === button.dataset.eventDetail);
            const imagePreview = `<div class="event-preview"><img src="${h(eventImageUrl(event))}" alt=""></div>`;
            openDetail('Event Detail', `${imagePreview}${detailGrid([
                ['Title', event.title],
                ['Department', event.departmentName],
                ['Committee', committeeForEvent(event)?.name || 'Chưa phân'],
                ['Location', event.location],
                ['Organizer', event.organizer || 'N/A'],
                ['Speakers', event.speakers || 'N/A'],
                ['Status', event.status],
                ['Capacity', number(event.capacity)],
                ['Budget', money(event.budget)],
                ['Đã nhận chỗ', `${number(event.registeredCount ?? event.registrationCount)} / ${number(event.capacity)}`],
                ['Waitlist', number(event.waitlistCount)],
                ['Tổng đăng ký', number(event.registrationCount)],
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
            rerender();
        }));
    }

    function getCommittees() {
        return localGet('committees', [
            { id: 'ops', name: 'Event Operations Committee', status: 'ACTIVE', members: [] },
            { id: 'academic', name: 'Academic Review Committee', status: 'ACTIVE', members: [] },
            { id: 'student-life', name: 'Student Life Committee', status: 'REVIEW', members: [] }
        ]);
    }

    async function renderRegistrations() {
        state.page = 'registrations';
        shell(`<button class="btn primary" id="addRegistration">${icon('user-plus')}Thêm người tham dự</button>`);
        const [registrations, events] = await Promise.all([load('/registrations', []), load('/events', [])]);
        const statusCounts = summarizeStatuses(registrations);
        const eventOptions = events
            .slice()
            .sort(compareEventsByTime)
            .map(event => ({ value: event.id, label: `${event.title} - ${dateTime(event.startTime)}` }));

        const addRegistration = () => openForm({
            title: 'Thêm người tham dự sự kiện',
            submitText: 'Thêm & gửi email',
            fields: [
                { name: 'eventId', label: 'Sự kiện', type: 'select', options: eventOptions, required: true, full: true },
                { name: 'fullName', label: 'Họ và tên', required: true },
                { name: 'email', label: 'Email nhận thông báo', type: 'email', required: true },
                { name: 'studentCode', label: 'MSSV', required: true },
                { name: 'major', label: 'Ngành/Khoa', required: true },
                { name: 'semester', label: 'Kỳ/Năm học', type: 'number', defaultValue: 1 },
                { name: 'status', label: 'Trạng thái', type: 'select', options: ['REGISTERED', 'WAITLIST'].map(value => ({ value, label: value })) },
                { name: 'note', label: 'Ghi chú', type: 'textarea', full: true }
            ],
            values: { status: 'REGISTERED' },
            onSubmit: async payload => {
                const result = await api('/registrations', { method: 'POST', body: JSON.stringify(payload) });
                toast(result.emailStatus === 'FAILED' ? 'Đã thêm người tham dự nhưng gửi email thất bại.' : 'Đã thêm người tham dự và gửi email.', result.emailStatus === 'FAILED' ? 'error' : 'success');
                renderRegistrations();
            }
        });

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

        const render = (q = '', keepSearchFocus = false) => {
            const filtered = registrations.filter(item => !q || matchesSearch(`${item.eventTitle} ${item.studentName} ${item.studentEmail} ${item.studentCode} ${item.studentMajor} ${item.status} ${item.attendanceStatus}`, q));
            const pageSize = 10;
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
                if (keepSearchFocus) {
                    search.focus();
                    search.setSelectionRange(q.length, q.length);
                }
                search.addEventListener('input', event => {
                    state.filters.registrationPage = 1;
                    render(event.target.value, true);
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
            document.getElementById('addRegistration').onclick = addRegistration;
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
            const filtered = feedback.filter(item => !q || matchesSearch(`${item.eventTitle} ${item.studentName} ${item.studentEmail} ${item.comment}`, q));
            const feedbackPager = pageItems('feedback', filtered, 10);
            content(`
                <div class="metric-grid">
                    ${metric('Feedback', number(feedback.length), 'Feedback List')}
                    ${metric('Average rating', `${avg.toLocaleString('vi-VN', { maximumFractionDigits: 1 })}/5`, 'Rating Analysis')}
                    ${metric('Low ratings', number(feedback.filter(item => Number(item.rating) <= 2).length), 'Cần xử lý')}
                    ${metric('Events reviewed', number(new Set(feedback.map(item => item.eventId)).size), 'Event có feedback')}
                </div>
                <div class="split-grid">
                    <section class="panel">
                        <div class="toolbar">${searchBox('feedbackSearch', 'Tìm event, sinh viên, comment...')}${pagination('feedback', feedbackPager.page, feedbackPager.pages, feedbackPager.total, feedbackPager.visible.length)}</div>
                        ${table(['Event', 'Student', 'Rating', 'Comment', 'Actions'], feedbackPager.visible.map(item => `
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
            bindPagination('feedback', feedbackPager, render, [q]);
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
        shell();
        const [reports, events] = await Promise.all([
            load('/reports', {}),
            load('/events', [])
        ]);
        const defaultRange = defaultReportRange(events);
        const reportFrom = state.filters.reportFrom || defaultRange.from;
        const reportTo = state.filters.reportTo || defaultRange.to;
        const fromDate = parseDateInput(reportFrom);
        const toDate = parseDateInput(reportTo, true);
        const rangeIsValid = fromDate && toDate && fromDate <= toDate;
        const filteredEvents = rangeIsValid
            ? events.filter(event => {
                const date = reportEventDate(event);
                return date && date >= fromDate && date <= toDate;
            })
            : [];
        const trend = rangeIsValid ? buildEventTrend(filteredEvents, fromDate, toDate) : [];
        const faculties = buildEventFaculties(filteredEvents);
        const hasChartData = filteredEvents.length > 0;
        content(`
            <div class="metric-grid">
                ${metric('Registration rate', percent(reports.registrationRate), `${number(reports.elapsedRegistrations)} / ${number(reports.elapsedCapacity)} capacity`, 'blue', 'ticket-check')}
                ${metric('Attendance rate', percent(reports.attendanceRate), `${number(reports.elapsedAttendance)} check-in`, 'teal', 'clipboard-check')}
                ${metric('Average rating', `${Number(reports.averageRating || 0).toLocaleString('vi-VN', { maximumFractionDigits: 1 })}/5`, 'Event đã diễn ra', 'orange', 'star')}
                ${metric('Student participation', number(reports.totalRegistrations), `${number(reports.studentUsers)} sinh viên`, 'green', 'graduation-cap')}
            </div>
            <section class="panel report-stat-panel">
                <div class="panel-header report-stat-header">
                    <div>
                        <h2 class="panel-title">Thống kê sự kiện</h2>
                        <p class="panel-note">Xu hướng số lượng sự kiện và cơ cấu sự kiện theo ngành.</p>
                    </div>
                    <div class="report-filter" role="group" aria-label="Lọc khoảng thời gian thống kê">
                        <label class="date-control"><span>Từ ngày</span><input id="reportFrom" type="date" value="${h(reportFrom)}"></label>
                        <label class="date-control"><span>Đến ngày</span><input id="reportTo" type="date" value="${h(reportTo)}"></label>
                        <button class="btn primary" id="applyReportRange">${icon('check')}Áp dụng</button>
                        <button class="btn" id="resetReportRange">${icon('rotate-ccw')}Đặt lại</button>
                        <button class="btn" id="exportReports">${icon('download')}Xuất CSV</button>
                    </div>
                </div>
                ${rangeIsValid ? '' : '<div class="empty">Ngày bắt đầu không được lớn hơn ngày kết thúc và không được để trống.</div>'}
                ${rangeIsValid && !hasChartData ? '<div class="empty">Không có dữ liệu trong khoảng thời gian đã chọn.</div>' : ''}
                <div class="event-stat-grid">
                    <article class="chart-card">
                        <div class="chart-card-head">
                            <h3>Xu hướng số lượng sự kiện</h3>
                        </div>
                        ${eventLineChart(hasChartData ? trend : [])}
                    </article>
                    <article class="chart-card">
                        <div class="chart-card-head">
                            <h3>Cơ cấu sự kiện theo ngành</h3>
                            <span>${number(filteredEvents.length)} sự kiện</span>
                        </div>
                        ${eventPieChart(faculties)}
                    </article>
                </div>
            </section>
            <div class="split-grid">
                <section class="panel">
                    <div class="panel-header">
                        <h2 class="panel-title">Attendance Reports</h2>
                        <a class="btn" href="registrations.html">Chi tiết</a>
                    </div>
                    ${table(['Event', 'Registered', 'Attendance', 'Fill'], filteredEvents.slice(0, 10).map(event => `
                        <tr>
                            <td><span class="event-cell"><img class="event-thumb" src="${h(eventImageUrl(event))}" alt=""><span><span class="cell-title">${h(event.title)}</span><span class="cell-sub">${h(event.departmentName)}</span></span></span></td>
                            <td>${number(event.registrationCount)}</td>
                            <td>${number(event.attendanceCount)}</td>
                            <td><div class="progress"><span style="width:${Math.min(100, Number(event.fillRate || 0))}%"></span></div><span class="cell-sub">${percent(event.fillRate)}</span></td>
                        </tr>`))}
                </section>
                <section class="panel">
                    <h2 class="panel-title">Sự kiện theo ngành</h2>
                    ${table(['Ngành', 'Events'], faculties.map(group => `
                        <tr><td>${h(group.name)}</td><td>${number(group.count)}</td></tr>`))}
                </section>
            </div>
            <section class="panel">
                <h2 class="panel-title">Công thức</h2>
                <p class="panel-note">${h(reports.formula?.registrationRate || '')}</p>
                <p class="panel-note">${h(reports.formula?.attendanceRate || '')}</p>
                <p class="panel-note">${h(reports.formula?.averageRating || '')}</p>
            </section>
        `);
        document.getElementById('applyReportRange')?.addEventListener('click', () => {
            const from = document.getElementById('reportFrom')?.value || '';
            const to = document.getElementById('reportTo')?.value || '';
            const parsedFrom = parseDateInput(from);
            const parsedTo = parseDateInput(to, true);
            if (!parsedFrom || !parsedTo) {
                toast('Vui lòng chọn đầy đủ Từ ngày và Đến ngày.', 'error');
                return;
            }
            if (parsedFrom > parsedTo) {
                toast('Ngày bắt đầu không được lớn hơn ngày kết thúc.', 'error');
                return;
            }
            state.filters.reportFrom = from;
            state.filters.reportTo = to;
            renderReports();
        });
        document.getElementById('resetReportRange')?.addEventListener('click', () => {
            delete state.filters.reportFrom;
            delete state.filters.reportTo;
            renderReports();
        });
        document.querySelectorAll('[data-report-faculty]').forEach(button => {
            button.addEventListener('click', event => {
                event.preventDefault();
                event.stopPropagation();
                const group = faculties.find(item => item.name === button.dataset.reportFaculty);
                if (!group) return;
                openDetail(`Sự kiện ${group.name}`, table(['Event', 'Thời gian', 'Đăng ký', 'Tham dự'], group.items.map(item => `
                    <tr>
                        <td><span class="cell-title">${h(item.title)}</span><span class="cell-sub">${h(item.departmentName || item.location || '')}</span></td>
                        <td>${dateTime(item.startTime)}</td>
                        <td>${number(item.registrationCount)}</td>
                        <td>${number(item.attendanceCount)}</td>
                    </tr>`), 'Không có sự kiện trong khoảng thời gian đã chọn.'));
            });
        });
        document.getElementById('exportReports')?.addEventListener('click', () => {
            exportCsv('aems-reports.csv', [
                ['Metric', 'Value'],
                ['Registration rate', percent(reports.registrationRate)],
                ['Attendance rate', percent(reports.attendanceRate)],
                ['Average rating', `${Number(reports.averageRating || 0).toLocaleString('vi-VN', { maximumFractionDigits: 1 })}/5`],
                ['Student participation', number(reports.totalRegistrations)],
                [],
                ['Từ ngày', reportFrom],
                ['Đến ngày', reportTo],
                ['Số sự kiện trong khoảng', filteredEvents.length],
                [],
                ['Mốc thời gian', 'Số sự kiện', 'Đăng ký', 'Tham dự'],
                ...trend.map(item => [item.label, item.events, item.registrations, item.attendance]),
                [],
                ['Ngành', 'Số sự kiện', 'Tỷ lệ', 'Sự kiện tiêu biểu'],
                ...faculties.map(item => [
                    item.name,
                    item.count,
                    `${(item.count / Math.max(filteredEvents.length, 1) * 100).toLocaleString('vi-VN', { maximumFractionDigits: 1 })}%`,
                    item.items.slice(0, 4).map(event => event.title).join(', ')
                ])
            ]);
        });
    }

    async function renderEmail() {
        state.page = 'email';
        shell(`<button class="btn primary" id="sendNotification">${icon('send')}Gửi email</button>`);
        const payload = await load('/email-logs?page=0&size=80', { items: [] });
        const logs = payload.items || [];
        const emailPager = pageItems('email', logs, 10);

        const sendForm = () => openForm({
            title: 'Send Notifications',
            fields: [
                { name: 'toEmail', label: 'Email nhận', type: 'email', required: true },
                { name: 'subject', label: 'Subject', required: true },
                { name: 'content', label: 'Nội dung', type: 'textarea', full: true, required: true }
            ],
            onSubmit: async payload => {
                const result = await api('/email-logs/send', { method: 'POST', body: JSON.stringify(payload) });
                toast(result.status === 'FAILED' ? (result.message || 'Không gửi được email thật.') : 'Đã gửi email thật và lưu lịch sử.', result.status === 'FAILED' ? 'error' : 'success');
                renderEmail();
            }
        });

        content(`
            <div class="metric-grid metric-grid-three">
                ${metric('Email History', number(payload.totalItems || logs.length), 'Lịch sử email')}
                ${metric('Sent', number(logs.filter(log => log.status === 'SENT').length), 'Gửi thành công')}
                ${metric('Failed', number(logs.filter(log => log.status === 'FAILED').length), 'Gửi lỗi')}
            </div>
            <section class="panel">
                    <div class="panel-header"><h2 class="panel-title">Email History</h2></div>
                    <div class="toolbar">${pagination('email', emailPager.page, emailPager.pages, emailPager.total, emailPager.visible.length)}</div>
                    ${table(['Email', 'Subject', 'Status', 'Sent At'], emailPager.visible.map(log => `
                        <tr>
                            <td>${h(log.toEmail)}</td>
                            <td>${h(log.subject)}<span class="cell-sub">${h(log.eventTitle || '')}</span></td>
                            <td>${badge(log.status, tone(log.status))}</td>
                            <td>${dateTime(log.sentAt)}</td>
                        </tr>`))}
            </section>
        `);
        document.getElementById('sendNotification').onclick = sendForm;
        bindPagination('email', emailPager, renderEmail);
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
        registrations: renderRegistrations,
        feedback: renderFeedback
    };

    function bindGlobalHotkeys() {
        document.addEventListener('keydown', event => {
            const isCmdK = (event.ctrlKey || event.metaKey) && (event.key === 'k' || event.key === 'K');
            if (isCmdK) {
                event.preventDefault();
                const root = document.getElementById('cmdkRoot');
                if (root && root.classList.contains('open')) {
                    root.classList.remove('open');
                    root.innerHTML = '';
                } else {
                    openCommandPalette();
                }
                return;
            }
            if (event.key === 'Escape') {
                const modal = document.getElementById('modalRoot');
                if (modal && modal.classList.contains('open')) {
                    modal.classList.remove('open');
                    modal.innerHTML = '';
                }
            }
            if ((event.ctrlKey || event.metaKey) && event.key === '/') {
                event.preventDefault();
                const search = document.querySelector('input[type="search"]');
                if (search) search.focus();
            }
        });
    }

    function init() {
        bindGlobalHotkeys();
        window.addEventListener('popstate', () => {
            const page = pageFromHref(window.location.pathname) || 'overview';
            if (!handlers[page]) return;
            state.page = '';
            navigate(page);
        });
        if (!handlers[state.page]) state.page = 'overview';
        (handlers[state.page] || renderOverview)().catch(error => {
            console.error(error);
            shell();
            content(`<div class="error"><strong>Không thể tải dữ liệu</strong><br>${h(error.message || error)}</div>`);
        });
    }

    return { init, openCommandPalette, showConfirm };
})();

document.addEventListener('DOMContentLoaded', Admin.init);
