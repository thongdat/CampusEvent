// ========== COMPONENT LOADER ========== 
/**
 * loadComponent(elementId, filePath)
 * Tải component HTML từ file và chèn vào element
 * 
 * Usage:
 * <div id="header"></div>
 * <script>
 *   loadComponent('header', 'components/header.html');
 * </script>
 */

async function loadComponent(elementId, filePath) {
    try {
        const response = await fetch(filePath);
        if (!response.ok) throw new Error(`Failed to load ${filePath}`);
        const html = await response.text();
        document.getElementById(elementId).innerHTML = html;
        
        // Gọi initialization function nếu tồn tại
        if (window[`init${elementId.charAt(0).toUpperCase() + elementId.slice(1)}`]) {
            window[`init${elementId.charAt(0).toUpperCase() + elementId.slice(1)}`]();
        }
    } catch (error) {
        console.error('Error loading component:', error);
    }
}

// ========== HEADER INITIALIZATION ========== 
function initHeader() {
    const userProfileBtn = document.getElementById('userProfileBtn');
    const userDropdown = document.getElementById('userDropdown');

    if (userProfileBtn && userDropdown) {
        userProfileBtn.addEventListener('click', function() {
            userDropdown.classList.toggle('show');
        });

        // Close dropdown when clicking outside
        document.addEventListener('click', function(event) {
            if (!userProfileBtn.contains(event.target) && !userDropdown.contains(event.target)) {
                userDropdown.classList.remove('show');
            }
        });
    }

    // Search functionality
    const searchInput = document.querySelector('.search-input');
    const searchBtn = document.querySelector('.search-btn');

    if (searchBtn) {
        searchBtn.addEventListener('click', function() {
            const query = searchInput.value;
            console.log('Searching for:', query);
            // Thêm logic search tại đây
        });
    }

    if (searchInput) {
        searchInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                const query = searchInput.value;
                console.log('Searching for:', query);
            }
        });
    }
}

// ========== SIDEBAR INITIALIZATION ========== 
function initSidebar() {
    const sidebarToggle = document.getElementById('sidebarToggle');
    const sidebar = document.querySelector('.sidebar');
    const menuItems = document.querySelectorAll('.menu-item');

    // Toggle sidebar on mobile
    if (sidebarToggle) {
        sidebarToggle.addEventListener('click', function() {
            if (sidebar) {
                sidebar.classList.toggle('show');
            }
        });
    }

    // Close sidebar when clicking on menu item
    menuItems.forEach(item => {
        item.addEventListener('click', function() {
            if (window.innerWidth <= 768 && sidebar) {
                sidebar.classList.remove('show');
            }

            // Set active state
            menuItems.forEach(i => i.classList.remove('active'));
            this.classList.add('active');
        });
    });

    // Set active menu item based on current page
    const currentPage = getCurrentPage();
    menuItems.forEach(item => {
        if (item.getAttribute('data-page') === currentPage) {
            item.classList.add('active');
        }
    });
}

// ========== FOOTER INITIALIZATION ========== 
function initFooter() {
    // Footer functionality if needed
    const footerYear = new Date().getFullYear();
    const copyrightElements = document.querySelectorAll('.footer-copyright p');
    
    copyrightElements.forEach(el => {
        el.textContent = `© ${footerYear} Event Management System. Tất cả quyền được bảo lưu.`;
    });
}

// ========== UTILITY FUNCTIONS ========== 

/**
 * Lấy tên trang hiện tại
 */
function getCurrentPage() {
    const path = window.location.pathname;
    const fileName = path.split('/').pop();
    
    if (fileName.includes('dashboard')) return 'dashboard';
    if (fileName.includes('events')) return 'events';
    if (fileName.includes('my-registrations') || fileName.includes('registrations')) return 'registrations';
    if (fileName.includes('my-tickets') || fileName.includes('tickets')) return 'tickets';
    if (fileName.includes('statistics') || fileName.includes('stats')) return 'stats';
    if (fileName.includes('admin-events')) return 'admin-events';
    if (fileName.includes('admin-users')) return 'admin-users';
    if (fileName.includes('admin-feedback')) return 'admin-feedback';
    
    return 'dashboard';
}

let logoutInProgress = false;

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
            background: #eff6ff;
            color: #2563eb;
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
                <span class="logout-notice-icon" aria-hidden="true">
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
                        <path d="M20 6 9 17l-5-5" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/>
                    </svg>
                </span>
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
    if (window.location.protocol === 'file:') {
        return window.location.pathname.includes('/admin-screen/') ? '../login.html' : 'login.html';
    }
    return window.location.pathname.startsWith('/api/') ? '/api/login.html' : '/login.html';
}

/**
 * Logout user
 */
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

/**
 * Initialize all interactive elements
 */
function initializeInteractiveElements() {
    // Logout buttons
    const logoutButtons = document.querySelectorAll('.btn-logout');
    logoutButtons.forEach(btn => {
        btn.addEventListener('click', logout);
    });

    // Responsive sidebar on resize
    window.addEventListener('resize', function() {
        if (window.innerWidth > 768) {
            const sidebar = document.querySelector('.sidebar');
            if (sidebar) {
                sidebar.classList.remove('show');
            }
        }
    });

    // Set user name from localStorage if available
    const userName = localStorage.getItem('userName') || 'Nguyễn Văn A';
    const userNameElements = document.querySelectorAll('.user-name, .sidebar-user-name');
    userNameElements.forEach(el => {
        if (el.classList.contains('user-name')) {
            el.textContent = userName;
        } else {
            el.textContent = userName;
        }
    });

    // Set user avatar
    const userAvatar = localStorage.getItem('userAvatar') || 'https://via.placeholder.com/32';
    const avatarElements = document.querySelectorAll('.avatar, .sidebar-avatar');
    avatarElements.forEach(el => {
        el.src = userAvatar;
    });

    // Set user role
    const userRole = localStorage.getItem('userRole') || 'Sinh viên';
    const roleElements = document.querySelectorAll('.sidebar-user-role');
    roleElements.forEach(el => {
        el.textContent = userRole;
    });

    // Notification badge
    const notificationBadges = document.querySelectorAll('.notification-btn .badge');
    notificationBadges.forEach(badge => {
        // Có thể lấy từ API
        badge.textContent = 3;
    });

    // Message badge
    const messageBadges = document.querySelectorAll('.message-btn .badge');
    messageBadges.forEach(badge => {
        badge.textContent = 2;
    });
}

// ========== COMPONENT LOADER FUNCTION ========== 
/**
 * Load tất cả components cần thiết
 * Usage: loadAllComponents() tại trang HTML
 */
async function loadAllComponents() {
    try {
        // Load header
        await loadComponent('header', 'components/header.html');
        initHeader();

        // Load sidebar
        await loadComponent('sidebar', 'components/sidebar.html');
        initSidebar();

        // Load footer
        await loadComponent('footer', 'components/footer.html');
        initFooter();

        // Initialize interactive elements
        initializeInteractiveElements();

        console.log('✅ All components loaded successfully!');
    } catch (error) {
        console.error('❌ Error loading components:', error);
    }
}

// ========== DOM CONTENT LOADED ========== 
document.addEventListener('DOMContentLoaded', function() {
    // Auto-load components if data-load-components attribute is present
    if (document.documentElement.hasAttribute('data-load-components')) {
        loadAllComponents();
    }
});

// ========== EXPORT FOR USE ========== 
window.ComponentManager = {
    loadComponent,
    loadAllComponents,
    logout,
    getCurrentPage
};
