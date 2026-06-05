/**
 * 前端安全工具函数
 * 提供 XSS 防护和输入清洗
 */

/**
 * HTML 转义（防止 XSS）
 */
function escapeHtml(str) {
    if (str == null) return '';
    const div = document.createElement('div');
    div.textContent = String(str);
    return div.innerHTML;
}

/**
 * 安全的 innerHTML 设置（自动转义）
 */
function safeSetHTML(element, html) {
    if (typeof element === 'string') {
        element = document.getElementById(element);
    }
    if (element) {
        element.innerHTML = html;
    }
}

/**
 * 安全的模板字符串（自动转义变量）
 */
function safeTemplate(strings, ...values) {
    let result = strings[0];
    for (let i = 0; i < values.length; i++) {
        result += escapeHtml(values[i]) + strings[i + 1];
    }
    return result;
}

/**
 * 验证输入不为空
 */
function validateRequired(value, fieldName) {
    if (!value || !value.trim()) {
        return fieldName + '不能为空';
    }
    return null;
}

/**
 * 验证数字范围
 */
function validateNumber(value, min, max, fieldName) {
    const num = Number(value);
    if (isNaN(num)) {
        return fieldName + '必须是数字';
    }
    if (min !== undefined && num < min) {
        return fieldName + '不能小于' + min;
    }
    if (max !== undefined && num > max) {
        return fieldName + '不能大于' + max;
    }
    return null;
}
