// ChengXun Game Maker - Markdown Editor Component

/**
 * Markdown编辑器组件
 * 支持三种视图模式：
 * 1. split - 左侧代码/右侧预览（默认）
 * 2. preview - 全部预览
 * 3. code - 全部代码
 *
 * 使用方法：
 * <div class="markdown-editor" data-name="content" data-rows="10">初始内容</div>
 * <script>MarkdownEditor.initAll();</script>
 */
class MarkdownEditor {
    /**
     * 构造函数
     * @param {HTMLElement} container - 编辑器容器元素
     * @param {Object} options - 配置选项
     */
    constructor(container, options = {}) {
        this.container = container;
        this.options = {
            name: options.name || 'content',
            rows: options.rows || 10,
            placeholder: options.placeholder || '输入Markdown内容...',
            required: options.required || false,
            readOnly: options.readOnly || false,
            initialContent: options.initialContent || '',
            onChange: options.onChange || null
        };

        this.mode = 'split'; // split, preview, code
        this.editor = null;
        this.preview = null;
        this.textarea = null;

        this.init();
    }

    /**
     * 初始化编辑器
     */
    init() {
        this.createEditorHTML();
        this.bindEvents();
        this.renderPreview();
    }

    /**
     * 创建编辑器HTML结构
     */
    createEditorHTML() {
        // 清空容器
        this.container.innerHTML = '';

        // 创建工具栏
        const toolbar = document.createElement('div');
        toolbar.className = 'markdown-toolbar';
        toolbar.innerHTML = `
            <div class="btn-group btn-group-sm" role="group">
                <button type="button" class="btn btn-outline-secondary active" data-mode="split" title="分屏模式">
                    <i class="bi bi-layout-split"></i> 分屏
                </button>
                <button type="button" class="btn btn-outline-secondary" data-mode="preview" title="预览模式">
                    <i class="bi bi-eye"></i> 预览
                </button>
                <button type="button" class="btn btn-outline-secondary" data-mode="code" title="代码模式">
                    <i class="bi bi-code"></i> 代码
                </button>
            </div>
            <div class="btn-group btn-group-sm ms-2" role="group">
                <button type="button" class="btn btn-outline-secondary markdown-btn" data-action="bold" title="粗体">
                    <i class="bi bi-type-bold"></i>
                </button>
                <button type="button" class="btn btn-outline-secondary markdown-btn" data-action="italic" title="斜体">
                    <i class="bi bi-type-italic"></i>
                </button>
                <button type="button" class="btn btn-outline-secondary markdown-btn" data-action="heading" title="标题">
                    <i class="bi bi-type-h1"></i>
                </button>
                <button type="button" class="btn btn-outline-secondary markdown-btn" data-action="link" title="链接">
                    <i class="bi bi-link"></i>
                </button>
                <button type="button" class="btn btn-outline-secondary markdown-btn" data-action="code" title="代码块">
                    <i class="bi bi-code-square"></i>
                </button>
                <button type="button" class="btn btn-outline-secondary markdown-btn" data-action="list" title="列表">
                    <i class="bi bi-list-ul"></i>
                </button>
            </div>
        `;

        // 创建编辑区域
        const editorArea = document.createElement('div');
        editorArea.className = 'markdown-editor-area';

        // 代码编辑区
        const codePanel = document.createElement('div');
        codePanel.className = 'markdown-code-panel';
        codePanel.innerHTML = `
            <textarea class="form-control bg-dark text-light border-secondary markdown-textarea"
                      name="${this.options.name}"
                      rows="${this.options.rows}"
                      placeholder="${this.options.placeholder}"
                      ${this.options.required ? 'required' : ''}
                      ${this.options.readOnly ? 'readonly' : ''}>${this.escapeHtml(this.options.initialContent)}</textarea>
        `;

        // 预览区
        const previewPanel = document.createElement('div');
        previewPanel.className = 'markdown-preview-panel';
        previewPanel.innerHTML = '<div class="markdown-preview"></div>';

        editorArea.appendChild(codePanel);
        editorArea.appendChild(previewPanel);

        // 组装
        this.container.appendChild(toolbar);
        this.container.appendChild(editorArea);

        // 保存引用
        this.textarea = codePanel.querySelector('textarea');
        this.preview = previewPanel.querySelector('.markdown-preview');
        this.toolbar = toolbar;
    }

    /**
     * 绑定事件
     */
    bindEvents() {
        // 视图模式切换
        this.toolbar.querySelectorAll('[data-mode]').forEach(btn => {
            btn.addEventListener('click', (e) => {
                this.setMode(e.currentTarget.dataset.mode);
            });
        });

        // Markdown快捷按钮
        this.toolbar.querySelectorAll('.markdown-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                this.handleMarkdownAction(e.currentTarget.dataset.action);
            });
        });

        // 文本输入事件
        this.textarea.addEventListener('input', () => {
            this.renderPreview();
            if (this.options.onChange) {
                this.options.onChange(this.textarea.value);
            }
        });

        // Tab键支持
        this.textarea.addEventListener('keydown', (e) => {
            if (e.key === 'Tab') {
                e.preventDefault();
                const start = this.textarea.selectionStart;
                const end = this.textarea.selectionEnd;
                this.textarea.value = this.textarea.value.substring(0, start) + '    ' + this.textarea.value.substring(end);
                this.textarea.selectionStart = this.textarea.selectionEnd = start + 4;
                this.renderPreview();
            }
        });
    }

    /**
     * 设置视图模式
     * @param {string} mode - 视图模式 (split, preview, code)
     */
    setMode(mode) {
        this.mode = mode;

        // 更新工具栏按钮状态
        this.toolbar.querySelectorAll('[data-mode]').forEach(btn => {
            btn.classList.toggle('active', btn.dataset.mode === mode);
        });

        // 更新编辑区域样式
        const editorArea = this.container.querySelector('.markdown-editor-area');
        editorArea.className = `markdown-editor-area mode-${mode}`;

        // 如果切换到预览模式，更新预览
        if (mode === 'preview' || mode === 'split') {
            this.renderPreview();
        }
    }

    /**
     * 处理Markdown快捷操作
     * @param {string} action - 操作类型
     */
    handleMarkdownAction(action) {
        const start = this.textarea.selectionStart;
        const end = this.textarea.selectionEnd;
        const selectedText = this.textarea.value.substring(start, end);

        let replacement = '';
        let cursorOffset = 0;

        switch (action) {
            case 'bold':
                replacement = `**${selectedText || '粗体文本'}**`;
                cursorOffset = selectedText ? replacement.length : 2;
                break;
            case 'italic':
                replacement = `*${selectedText || '斜体文本'}*`;
                cursorOffset = selectedText ? replacement.length : 1;
                break;
            case 'heading':
                replacement = `## ${selectedText || '标题'}`;
                cursorOffset = replacement.length;
                break;
            case 'link':
                replacement = `[${selectedText || '链接文本'}](url)`;
                cursorOffset = selectedText ? replacement.length - 1 : 1;
                break;
            case 'code':
                if (selectedText.includes('\n')) {
                    replacement = `\`\`\`\n${selectedText}\n\`\`\``;
                } else {
                    replacement = `\`${selectedText || '代码'}\``;
                }
                cursorOffset = selectedText ? replacement.length : 1;
                break;
            case 'list':
                replacement = selectedText.split('\n').map(line => `- ${line}`).join('\n');
                cursorOffset = replacement.length;
                break;
        }

        this.textarea.value = this.textarea.value.substring(0, start) + replacement + this.textarea.value.substring(end);
        this.textarea.selectionStart = start + cursorOffset;
        this.textarea.selectionEnd = start + cursorOffset;
        this.textarea.focus();
        this.renderPreview();
    }

    /**
     * 渲染Markdown预览
     */
    renderPreview() {
        if (typeof marked === 'undefined') {
            this.preview.innerHTML = '<div class="text-danger">Markdown库未加载</div>';
            return;
        }

        const content = this.textarea.value;
        if (!content.trim()) {
            this.preview.innerHTML = '<div class="text-muted">预览区域</div>';
            return;
        }

        try {
            this.preview.innerHTML = marked.parse(content);
        } catch (e) {
            this.preview.innerHTML = `<div class="text-danger">渲染错误: ${e.message}</div>`;
        }
    }

    /**
     * 获取编辑器内容
     * @returns {string} 编辑器内容
     */
    getValue() {
        return this.textarea.value;
    }

    /**
     * 设置编辑器内容
     * @param {string} content - 内容
     */
    setValue(content) {
        this.textarea.value = content;
        this.renderPreview();
    }

    /**
     * HTML转义
     * @param {string} text - 原始文本
     * @returns {string} 转义后的文本
     */
    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    /**
     * 初始化页面上所有的Markdown编辑器
     */
    static initAll() {
        document.querySelectorAll('.markdown-editor').forEach(container => {
            const options = {
                name: container.dataset.name || 'content',
                rows: parseInt(container.dataset.rows) || 10,
                placeholder: container.dataset.placeholder || '输入Markdown内容...',
                required: container.dataset.required === 'true',
                readOnly: container.dataset.readonly === 'true',
                initialContent: container.textContent.trim()
            };
            new MarkdownEditor(container, options);
        });
    }
}

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', () => {
    MarkdownEditor.initAll();
});
