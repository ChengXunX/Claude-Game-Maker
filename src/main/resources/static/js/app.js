// ChengXun Game Maker - Frontend JavaScript

// Auto-hide alerts after 5 seconds
document.addEventListener('DOMContentLoaded', function() {
    const alerts = document.querySelectorAll('.alert-dismissible');
    alerts.forEach(function(alert) {
        setTimeout(function() {
            const bsAlert = new bootstrap.Alert(alert);
            bsAlert.close();
        }, 5000);
    });
});

// Confirm actions
function confirmAction(message) {
    return confirm(message || '确定要执行此操作吗？');
}

// ===== 主题切换功能 =====

/**
 * 初始化主题系统
 * 从localStorage读取保存的主题偏好，如果没有则使用默认暗色主题
 * 在DOM加载完成后调用，确保body元素已存在
 */
function initTheme() {
    const savedTheme = localStorage.getItem('theme') || 'dark';
    applyTheme(savedTheme);

    // 确保body元素有正确的类名
    const body = document.getElementById('page-body');
    if (body) {
        if (savedTheme === 'light') {
            body.classList.remove('bg-dark', 'text-light');
            body.classList.add('bg-light', 'text-dark');
        } else {
            body.classList.remove('bg-light', 'text-dark');
            body.classList.add('bg-dark', 'text-light');
        }
    }
}

/**
 * 应用主题到页面
 * @param {string} theme - 主题名称 ('dark' 或 'light')
 */
function applyTheme(theme) {
    // 设置data-theme属性
    document.documentElement.setAttribute('data-theme', theme);

    // 更新body类名（Bootstrap兼容）
    if (theme === 'light') {
        document.body.classList.remove('bg-dark', 'text-light');
        document.body.classList.add('bg-light', 'text-dark');
    } else {
        document.body.classList.remove('bg-light', 'text-dark');
        document.body.classList.add('bg-dark', 'text-light');
    }

    // 更新图标
    updateThemeIcon(theme);

    // 保存到localStorage
    localStorage.setItem('theme', theme);
}

/**
 * 切换主题（暗色 <-> 亮色）
 */
function toggleTheme() {
    const currentTheme = localStorage.getItem('theme') || 'dark';
    const newTheme = currentTheme === 'dark' ? 'light' : 'dark';

    // 添加切换动画效果
    const icon = document.getElementById('theme-icon');
    if (icon) {
        icon.style.transform = 'rotate(360deg)';
        setTimeout(() => {
            icon.style.transform = '';
        }, 300);
    }

    applyTheme(newTheme);
}

/**
 * 更新主题切换按钮图标
 * @param {string} theme - 当前主题
 */
function updateThemeIcon(theme) {
    const icon = document.getElementById('theme-icon');
    if (icon) {
        if (theme === 'dark') {
            icon.className = 'bi bi-moon-stars-fill';
        } else {
            icon.className = 'bi bi-sun-fill';
        }
    }
}

// 页面加载时初始化主题
document.addEventListener('DOMContentLoaded', function() {
    initTheme();
});
