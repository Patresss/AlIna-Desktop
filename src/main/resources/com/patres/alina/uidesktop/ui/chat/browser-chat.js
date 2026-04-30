    /* ══════════════════════════════════════════════════
       AlIna Chat — JavaScript Engine (2026 Edition)
       ══════════════════════════════════════════════════ */

    // ── DOM helper ──────────────────────────────────
    /**
     * Tiny DOM builder:  h('div', { className: 'foo', onclick: fn }, child1, child2)
     * Children can be strings (→ textNode) or DOM nodes.
     */
    function h(tag, attrs, ...children) {
        const el = document.createElement(tag);
        if (attrs) {
            for (const [k, v] of Object.entries(attrs)) {
                if (k.startsWith('on') && typeof v === 'function') {
                    el.addEventListener(k.slice(2), v);
                } else if (k === 'dataset') {
                    Object.assign(el.dataset, v);
                } else if (k === 'aria') {
                    for (const [ak, av] of Object.entries(v)) {
                        el.setAttribute(`aria-${ak}`, av);
                    }
                } else {
                    el[k] = v;
                }
            }
        }
        for (const child of children) {
            if (child == null) continue;
            el.append(typeof child === 'string' ? document.createTextNode(child) : child);
        }
        return el;
    }

    // ── Utility: getElementById shorthand ───────────
    const $ = (id) => document.getElementById(id);

    // ── Welcome screen ──────────────────────────────
    function showWelcomeScreen() {
        const chatContainer = $('chat-container');
        if (!chatContainer || chatContainer.children.length > 0) return;

        const welcome = h('div', { className: 'welcome-screen', id: 'welcome-screen' });

        // Header: logo + greeting + subtitle
        const header = h('div', { className: 'welcome-header' },
            h('div', { className: 'welcome-logo' },
                h('span', { className: 'welcome-logo-accent' }, 'A'),
                h('span', {}, 'l'),
                h('span', { className: 'welcome-logo-accent' }, 'I'),
                h('span', {}, 'na')
            ),
            h('div', { className: 'welcome-greeting', id: 'welcome-greeting' }),
            h('div', { className: 'welcome-subtitle', id: 'welcome-subtitle' },
                'Type a message to start, or press ',
                h('kbd', {}, '/'),
                ' for quick actions.'
            )
        );
        welcome.appendChild(header);

        // Placeholder containers for async sections
        welcome.appendChild(h('div', { className: 'welcome-sections', id: 'welcome-sections' }));

        chatContainer.appendChild(welcome);
    }

    function populateWelcomeData(greetingText, commandsJson, commandsLabel) {
        // Greeting
        const greetingEl = $('welcome-greeting');
        if (greetingEl && greetingText) {
            greetingEl.textContent = greetingText;
        }

        const sections = $('welcome-sections');
        if (!sections) return;
        sections.innerHTML = '';

        let commands = [];
        try { commands = JSON.parse(commandsJson); } catch { /* ignore */ }

        // Commands as chips
        if (commands.length > 0) {
            const chips = h('div', { className: 'welcome-section welcome-commands' });
            chips.appendChild(h('div', { className: 'welcome-section-label' }, commandsLabel || 'Commands'));
            const chipRow = h('div', { className: 'welcome-chips' });
            for (const cmd of commands) {
                const chip = h('button', {
                    type: 'button',
                    className: 'welcome-chip',
                    onclick: () => {
                        if (window.alinaBrowserBridge?.handleSelectCommand) {
                            window.alinaBrowserBridge.handleSelectCommand(cmd.id);
                        }
                    }
                },
                    h('span', { className: 'welcome-chip-name' }, cmd.name)
                );
                if (cmd.description) {
                    chip.title = cmd.description;
                }
                chipRow.appendChild(chip);
            }
            chips.appendChild(chipRow);
            sections.appendChild(chips);
        }

        // Fade in sections
        requestAnimationFrame(() => sections.classList.add('loaded'));
    }

    function updateWelcomeSubtitle(text) {
        const el = $('welcome-subtitle');
        if (el) el.textContent = text;
    }

    function removeWelcomeScreen() {
        const welcome = $('welcome-screen');
        if (welcome) {
            Object.assign(welcome.style, {
                opacity: '0',
                transform: 'translateY(-10px)',
                transition: 'opacity 0.2s ease, transform 0.2s ease'
            });
            setTimeout(() => welcome.remove(), 200);
        }
    }

    // Show welcome screen on initial load
    document.addEventListener('DOMContentLoaded', () => {
        showWelcomeScreen();
        initScrollToBottom();
    });
    // Fallback for immediate execution (WebView may not fire DOMContentLoaded)
    if (document.readyState === 'complete' || document.readyState === 'interactive') {
        showWelcomeScreen();
        initScrollToBottom();
    }

    // ── Scroll to bottom FAB ────────────────────────
    function initScrollToBottom() {
        if ($('scroll-to-bottom-fab')) return; // already initialized

        const fab = h('div', {
            className: 'scroll-to-bottom',
            id: 'scroll-to-bottom-fab',
            role: 'button',
            tabIndex: 0,
            innerHTML: '\u25BE',
            onclick: () => window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' }),
            onkeydown: (e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
                }
            }
        });
        fab.setAttribute('aria-label', 'Scroll to bottom');
        document.body.appendChild(fab);

        window.addEventListener('scroll', () => {
            const scrollBottom = document.documentElement.scrollHeight - window.innerHeight - window.scrollY;
            fab.classList.toggle('visible', scrollBottom > 300);
        });
    }

    // ── Code block enhancement ──────────────────────
    function enhanceCodeBlocks(container) {
        if (!container) return;

        for (const pre of container.querySelectorAll('pre')) {
            if (pre.dataset.enhanced === 'true') continue;
            pre.dataset.enhanced = 'true';

            const codeEl = pre.querySelector('code');
            if (!codeEl) continue;

            // Detect language from class
            const langMatch = (codeEl.className || '').match(/language-(\w+)/);
            const language = langMatch ? langMatch[1] : 'code';

            const copyBtn = h('button', {
                className: 'code-copy-btn',
                innerHTML: '\u2398 Copy',
                onclick: (e) => {
                    e.stopPropagation();
                    copyToClipboard(codeEl.textContent || codeEl.innerText || '', copyBtn);
                }
            });
            copyBtn.setAttribute('aria-label', `Copy ${language} code`);

            const header = h('div', { className: 'code-block-header' },
                h('span', { className: 'code-block-language', textContent: language }),
                copyBtn
            );

            pre.insertBefore(header, pre.firstChild);
        }
    }

    function copyToClipboard(text, buttonEl) {
        // WebView lacks a secure context, so navigator.clipboard never works.
        // Always use the Java bridge which copies via JavaFX Clipboard API.
        if (window.alinaBrowserBridge?.handleCopyText) {
            window.alinaBrowserBridge.handleCopyText(text);
            showCopiedFeedback(buttonEl);
        }
    }

    function showCopiedFeedback(buttonEl) {
        if (!buttonEl) return;
        buttonEl.classList.add('copied');
        setTimeout(() => buttonEl.classList.remove('copied'), 1200);
    }

    // ── Message action buttons ──────────────────────
    function addMessageActions(messageDiv) {
        if (!messageDiv || messageDiv.querySelector('.message-actions')) return;
        if (messageDiv.dataset?.transient === 'true') return;

        const copyBtn = h('button', {
            className: 'message-action-btn',
            innerHTML: '\u2398',
            title: 'Copy message',
            onclick: (e) => {
                e.stopPropagation();
                // Get text content, excluding action buttons
                const clone = messageDiv.cloneNode(true);
                clone.querySelector('.message-actions')?.remove();
                clone.querySelector('.message-footer')?.remove();
                copyToClipboard((clone.textContent || clone.innerText || '').trim(), copyBtn);
            }
        });
        copyBtn.setAttribute('aria-label', 'Copy message');

        messageDiv.appendChild(h('div', { className: 'message-actions' }, copyBtn));
    }

    // ── Core chat functions ─────────────────────────

    function addHtmlContent(htmlContent, messageType, notificationStyle) {
        removeWelcomeScreen();

        const div = h('div', { className: `chat-message ${messageType} ${notificationStyle}` });
        div.innerHTML = htmlContent;

        $('chat-container').appendChild(div);

        // Enhance code blocks and add action buttons
        enhanceCodeBlocks(div);
        addMessageActions(div);

        // Force synchronous reflow to fix WebKit border rendering bug
        void div.offsetHeight;
    }

    function showLoader() {
        removeWelcomeScreen();
        const loader = $('loader');
        loader.classList.add('active');
        loader.classList.remove('user-message');
        window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
    }

    function showLoaderForUserMessage() {
        showLoader();
        $('loader').classList.add('user-message');
    }

    function hideLoader() {
        const loader = $('loader');
        loader.classList.remove('active');
        loader.classList.remove('user-message');
    }

    function showAssistantActivity(label, detail) {
        const chatContainer = $('chat-container');
        if (!chatContainer) return;

        let activity = $('assistant-activity-message');
        if (!activity) {
            const summaryText = h('span', {
                className: 'activity-summary-text',
                id: 'assistant-activity-summary-text',
                textContent: `Tools \u00b7 ${stripActivityPrefix(label)}`
            });

            const summaryBadge = h('span', {
                className: 'activity-summary-badge',
                id: 'assistant-activity-summary-badge',
                textContent: '1'
            });

            const toggleButton = h('button', {
                className: 'activity-toggle',
                id: 'assistant-activity-toggle',
                type: 'button',
                textContent: '\u25b8',
                aria: { label: 'Show details' },
                onclick(e) {
                    const shell = this.parentElement?.parentElement;
                    const activityEl = shell?.parentElement;
                    const bodyEl = shell?.querySelector('.activity-body');
                    if (!activityEl || !bodyEl) return;
                    const expanded = activityEl.dataset.expanded === 'true';
                    if (expanded) {
                        bodyEl.classList.remove('open');
                        activityEl.dataset.expanded = 'false';
                        this.textContent = '\u25b8';
                        this.setAttribute('aria-label', 'Show details');
                    } else {
                        bodyEl.classList.add('open');
                        activityEl.dataset.expanded = 'true';
                        this.textContent = '\u25be';
                        this.setAttribute('aria-label', 'Hide details');
                    }
                }
            });

            const summaryMain = h('div', { className: 'activity-summary-main' }, summaryText, summaryBadge);
            const summary = h('div', { className: 'activity-summary' }, summaryMain, toggleButton);
            const body = h('div', { className: 'activity-body', id: 'assistant-activity-body' });
            const shell = h('div', { className: 'activity-shell' }, summary, body);

            activity = h('div', {
                className: 'chat-message assistant activity-message',
                id: 'assistant-activity-message',
                dataset: { transient: 'true', count: '0', expanded: 'false' }
            }, shell);

            chatContainer.appendChild(activity);
        }

        const bodyNode = $('assistant-activity-body');
        const lastEntry = bodyNode?.lastElementChild;
        const shortLabel = stripActivityPrefix(label);

        if (lastEntry?.dataset?.label === label) {
            const count = parseInt(lastEntry.dataset.count || '1', 10) + 1;
            lastEntry.dataset.count = String(count);
            const nameEl = lastEntry.querySelector('.activity-entry-name');
            if (nameEl) nameEl.textContent = `${shortLabel} \u00d7${count}`;
            if (detail?.trim()) {
                const detailEl = lastEntry.querySelector('.activity-entry-detail');
                if (detailEl) detailEl.textContent = detail;
            }
        } else if (bodyNode) {
            const entry = h('div', {
                className: 'activity-entry',
                dataset: { label, count: '1' }
            },
                h('span', { className: 'activity-entry-name', textContent: shortLabel })
            );

            if (detail?.trim()) {
                entry.appendChild(h('span', { className: 'activity-entry-detail', textContent: detail }));
            }

            bodyNode.appendChild(entry);
        }

        if (activity.dataset) {
            const totalCount = parseInt(activity.dataset.count || '0', 10) + 1;
            activity.dataset.count = String(totalCount);
            activity.dataset.lastEntry = label;
        }

        const summaryTextNode = $('assistant-activity-summary-text');
        if (summaryTextNode) {
            summaryTextNode.textContent = buildAssistantActivitySummary(activity.dataset.count, label);
        }
        const summaryBadgeNode = $('assistant-activity-summary-badge');
        if (summaryBadgeNode && activity.dataset) {
            summaryBadgeNode.textContent = activity.dataset.count;
        }
        window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
    }

    function finalizeAssistantActivity() {
        const activity = $('assistant-activity-message');
        if (!activity) return;

        activity.removeAttribute('id');
        for (const cid of ['assistant-activity-body', 'assistant-activity-toggle',
                            'assistant-activity-summary-text', 'assistant-activity-summary-badge']) {
            $(cid)?.removeAttribute('id');
        }
        if (activity.dataset) {
            delete activity.dataset.lastEntry;
        }
    }

    function clearAssistantActivity() {
        $('assistant-activity-message')?.remove();
    }

    function stripActivityPrefix(label) {
        return label.replace(/^OpenCode:\s*/i, '').replace(/^Skill:\s*/i, '').replace(/^MCP:\s*/i, '');
    }

    function buildAssistantActivitySummary(count, lastLabel) {
        const safeCount = parseInt(count || '0', 10) || 0;
        const shortLabel = stripActivityPrefix(lastLabel);
        return safeCount <= 1
            ? `Tools \u00b7 ${shortLabel}`
            : `Tools \u00b7 latest: ${shortLabel}`;
    }

    function toggleAssistantActivity() {
        const activity = $('assistant-activity-message');
        if (!activity?.dataset) return;

        const body = $('assistant-activity-body');
        const toggle = $('assistant-activity-toggle');
        if (!body || !toggle) return;

        const expanded = activity.dataset.expanded === 'true';
        if (expanded) {
            body.classList.remove('open');
            activity.dataset.expanded = 'false';
            toggle.textContent = '\u25b8';
            toggle.setAttribute('aria-label', 'Show details');
        } else {
            body.classList.add('open');
            activity.dataset.expanded = 'true';
            toggle.textContent = '\u25be';
            toggle.setAttribute('aria-label', 'Hide details');
        }
    }

    function showAssistantReasoning(title, htmlContent) {
        const chatContainer = $('chat-container');
        if (!chatContainer) return;

        let card = $('assistant-reasoning-message');
        if (!card) {
            const body = h('div', { className: 'reasoning-body', id: 'assistant-reasoning-body' });
            const details = h('details', { className: 'reasoning-details', open: true },
                h('summary', { className: 'reasoning-summary', textContent: title }),
                body
            );
            card = h('div', {
                className: 'chat-message assistant reasoning-message',
                id: 'assistant-reasoning-message',
                dataset: { transient: 'true' }
            }, details);
            chatContainer.appendChild(card);
        }
        const bodyNode = $('assistant-reasoning-body');
        if (bodyNode) bodyNode.innerHTML = htmlContent;
        window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
    }

    function showAssistantCommentary(title, htmlContent) {
        const chatContainer = $('chat-container');
        if (!chatContainer) return;

        let card = $('assistant-commentary-message');
        if (!card) {
            const body = h('div', { className: 'commentary-body', id: 'assistant-commentary-body' });
            const details = h('details', { className: 'commentary-details', open: true },
                h('summary', { className: 'commentary-summary', textContent: title }),
                body
            );
            card = h('div', {
                className: 'chat-message assistant commentary-message',
                id: 'assistant-commentary-message',
                dataset: { transient: 'true' }
            }, details);
            chatContainer.appendChild(card);
        }
        const bodyNode = $('assistant-commentary-body');
        if (bodyNode) bodyNode.innerHTML = htmlContent;
        window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
    }

    function finalizeAssistantReasoning() {
        const card = $('assistant-reasoning-message');
        if (!card) return;
        card.removeAttribute('id');
        $('assistant-reasoning-body')?.removeAttribute('id');
    }

    function finalizeAssistantCommentary() {
        const card = $('assistant-commentary-message');
        if (!card) return;
        card.removeAttribute('id');
        $('assistant-commentary-body')?.removeAttribute('id');
    }

    function clearAssistantReasoning() {
        $('assistant-reasoning-message')?.remove();
    }

    function clearAssistantCommentary() {
        $('assistant-commentary-message')?.remove();
    }

    function attachProcessPanelToLastAssistantMessage(summaryText, reasoningTitle, reasoningHtml, commentaryTitle, commentaryHtml, toolsHtml) {
        const chatContainer = $('chat-container');
        if (!chatContainer?.children) return;

        // Find target: streaming message or last non-transient assistant message
        let target = null;
        for (let i = chatContainer.children.length - 1; i >= 0; i--) {
            const node = chatContainer.children[i];
            if (!node?.classList) continue;
            if (node.id === 'streaming-message') { target = node; break; }
            if (node.classList.contains('chat-message')
                && node.classList.contains('assistant')
                && node.dataset?.transient !== 'true') {
                target = node;
                break;
            }
        }
        if (!target) return;

        target.querySelector('.assistant-process')?.remove();

        const hasReasoning = reasoningHtml?.trim();
        const hasCommentary = commentaryHtml?.trim();
        const hasTools = toolsHtml?.trim();
        if (!hasReasoning && !hasCommentary && !hasTools) return;

        const body = h('div', { className: 'assistant-process-body' });
        const chevron = h('span', { className: 'assistant-process-chevron', textContent: '\u25b8' });

        const appendSection = (title, html) => {
            if (!html?.trim()) return;
            const contentNode = h('div', { className: 'assistant-process-content' });
            contentNode.innerHTML = html;
            body.appendChild(h('div', { className: 'assistant-process-section' },
                h('div', { className: 'assistant-process-title', textContent: title }),
                contentNode
            ));
        };

        appendSection(reasoningTitle, reasoningHtml);
        appendSection(commentaryTitle, commentaryHtml);
        appendSection('Tools', toolsHtml);

        const toggle = h('button', {
            type: 'button',
            className: 'assistant-process-toggle',
            onclick: () => {
                const expanded = body.classList.toggle('open');
                chevron.textContent = expanded ? '\u25be' : '\u25b8';
            }
        },
            h('span', { className: 'assistant-process-summary', textContent: summaryText || 'Process' }),
            chevron
        );

        target.appendChild(h('div', { className: 'assistant-process' }, toggle, body));
    }

    function showAssistantPermissionRequest(requestId, title, message, approveLabel, approveAlwaysLabel, denyLabel) {
        const chatContainer = $('chat-container');
        if (!chatContainer || !requestId) return;

        const messageId = `assistant-permission-${requestId}`;
        if ($(messageId)) {
            window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
            return;
        }

        const createButton = (label, cssClass, actionName) => {
            return h('button', {
                type: 'button',
                className: cssClass,
                textContent: label,
                onclick: () => {
                    markAssistantPermissionRequestPending(requestId, `${label}...`);
                    if (window.alinaBrowserBridge?.handlePermissionAction) {
                        window.alinaBrowserBridge.handlePermissionAction(requestId, actionName);
                    }
                }
            });
        };

        const card = h('div', {
            className: 'chat-message assistant permission-message',
            id: messageId,
            dataset: { transient: 'true' }
        },
            h('div', { className: 'permission-shell' },
                h('div', { className: 'permission-header' },
                    h('div', { className: 'permission-title', textContent: title }),
                    h('div', { className: 'permission-badge', textContent: 'Approval' })
                ),
                h('div', { className: 'permission-message-body', textContent: message }),
                h('div', { className: 'permission-actions' },
                    createButton(approveLabel, 'primary', 'APPROVE_ONCE'),
                    createButton(approveAlwaysLabel, 'always', 'APPROVE_ALWAYS'),
                    createButton(denyLabel, 'deny', 'DENY')
                ),
                h('div', { className: 'permission-status', id: `${messageId}-status` })
            )
        );

        chatContainer.appendChild(card);
        window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
    }

    function markAssistantPermissionRequestPending(requestId, statusLabel) {
        const card = $(`assistant-permission-${requestId}`);
        if (!card) return;
        for (const btn of card.querySelectorAll('button')) btn.disabled = true;
        const status = $(`assistant-permission-${requestId}-status`);
        if (status) status.textContent = statusLabel || '';
        window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
    }

    function resolveAssistantPermissionRequest(requestId, statusLabel) {
        const card = $(`assistant-permission-${requestId}`);
        if (!card) return;
        for (const btn of card.querySelectorAll('button')) btn.disabled = true;
        const status = $(`assistant-permission-${requestId}-status`);
        if (status) status.textContent = statusLabel || '';
        window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
    }

    // ── Streaming messages ──────────────────────────

    function startStreamingAssistantMessage() {
        removeWelcomeScreen();
        const chatContainer = $('chat-container');
        let streamingDiv = null;

        if (arguments.length > 0 && arguments[0] === true) {
            const target = $('regenerate-target');
            if (target) streamingDiv = target;
        }

        if (!streamingDiv) {
            streamingDiv = document.createElement('div');
            chatContainer.appendChild(streamingDiv);
        }

        streamingDiv.className = 'chat-message assistant';
        streamingDiv.id = 'streaming-message';
        streamingDiv.innerHTML = '';

        window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
    }

    function appendToStreamingMessage(escapedToken) {
        const streamingDiv = $('streaming-message');
        if (streamingDiv) {
            // Use textContent to preserve markdown syntax during streaming
            streamingDiv.textContent += escapedToken;
            window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
        }
    }

    function updateStreamingMessageWithHtml(htmlContent) {
        const streamingDiv = $('streaming-message');
        if (streamingDiv) {
            streamingDiv.innerHTML = htmlContent;
            enhanceCodeBlocks(streamingDiv);
            void streamingDiv.offsetHeight;
            window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
        }
    }

    function finishStreamingMessage() {
        const streamingDiv = $('streaming-message');
        if (streamingDiv) {
            streamingDiv.removeAttribute('id');
            streamingDiv.dataset.awaitingFooter = 'true';
            enhanceCodeBlocks(streamingDiv);
            addMessageActions(streamingDiv);
            void streamingDiv.offsetHeight;
            window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
        }
    }

    function finishStreamingMessageWithMarkdown(processedHtml) {
        const streamingDiv = $('streaming-message');
        if (streamingDiv) {
            streamingDiv.innerHTML = processedHtml;
            streamingDiv.removeAttribute('id');
            streamingDiv.dataset.awaitingFooter = 'true';
            enhanceCodeBlocks(streamingDiv);
            addMessageActions(streamingDiv);
            void streamingDiv.offsetHeight;
            window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
        }
    }

    function scrollToBottom() {
        window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
    }

    function attachMessageFooter(footerText) {
        const chatContainer = $('chat-container');
        if (!chatContainer?.children) return;

        // First try: message awaiting footer
        let target = null;
        for (let i = chatContainer.children.length - 1; i >= 0; i--) {
            const node = chatContainer.children[i];
            if (node?.dataset?.awaitingFooter === 'true') { target = node; break; }
        }

        // Fallback: last non-transient assistant message
        if (!target) {
            for (let j = chatContainer.children.length - 1; j >= 0; j--) {
                const node = chatContainer.children[j];
                if (!node?.classList) continue;
                if (node.classList.contains('chat-message')
                    && node.classList.contains('assistant')
                    && node.dataset?.transient !== 'true') {
                    target = node;
                    break;
                }
            }
        }

        if (!target) return;
        delete target.dataset.awaitingFooter;

        target.querySelector('.message-footer')?.remove();
        target.appendChild(h('div', { className: 'message-footer', textContent: footerText }));
    }

    // ── Regeneration ────────────────────────────────

    function prepareRegenerationTarget() {
        const chatContainer = $('chat-container');
        if (!chatContainer?.children) return false;

        for (let i = chatContainer.children.length - 1; i >= 0; i--) {
            const node = chatContainer.children[i];
            if (!node?.classList) continue;
            if (node.classList.contains('chat-message')
                && node.classList.contains('assistant')
                && node.dataset?.transient !== 'true') {
                node.dataset.prevHtml = node.innerHTML;
                node.id = 'regenerate-target';
                return true;
            }
        }
        return false;
    }

    function restoreRegenerationTarget() {
        let node = $('streaming-message') || $('regenerate-target');
        if (!node) return;

        if (node.dataset?.prevHtml !== undefined) {
            node.innerHTML = node.dataset.prevHtml;
            delete node.dataset.prevHtml;
        }
        node.removeAttribute('id');
        window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
    }

    function discardRegenerationBackup() {
        const node = $('streaming-message') || $('regenerate-target');
        if (node?.dataset?.prevHtml !== undefined) {
            delete node.dataset.prevHtml;
        }
    }

    // ── Todo list ───────────────────────────────────

    function showTodoList(jsonString, title) {
        let items;
        try { items = JSON.parse(jsonString); } catch { return; }
        if (!items?.length) return;

        const container = $('todo-sticky-container');
        if (!container) return;

        container.dataset.todoJson = jsonString;
        container.dataset.todoTitle = title;

        let card = $('assistant-todo-sticky');
        if (!card) {
            card = h('div', { className: 'todo-sticky', id: 'assistant-todo-sticky' });
            container.appendChild(card);
        }

        card.innerHTML = buildTodoHtml(items, title);
        window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
    }

    function buildTodoHtml(items, title) {
        const completed = items.filter(it => it.status === 'completed').length;
        const total = items.length;
        const percent = total > 0 ? Math.round((completed / total) * 100) : 0;

        return `<div class="todo-header">` +
            `<div class="todo-title">${escapeHtmlTodo(title)}</div>` +
            `<div class="todo-progress-info">${completed}/${total}</div>` +
            `</div>` +
            `<div class="todo-progress-bar-track">` +
            `<div class="todo-progress-bar-fill" style="width:${percent}%"></div>` +
            `</div>` +
            `<div class="todo-items">` +
            items.map(item => {
                const statusClass = `todo-status-${item.status || 'pending'}`;
                const priorityClass = `todo-priority-${item.priority || 'medium'}`;
                return `<div class="todo-item ${statusClass} ${priorityClass}">` +
                    `<span class="todo-item-icon">${getTodoStatusIcon(item.status)}</span>` +
                    `<span class="todo-item-content">${escapeHtmlTodo(item.content)}</span>` +
                    `</div>`;
            }).join('') +
            `</div>`;
    }

    function getTodoStatusIcon(status) {
        switch (status) {
            case 'completed':
                return '<svg viewBox="0 0 16 16" fill="none"><circle cx="8" cy="8" r="7" stroke="currentColor" stroke-width="1.5" fill="currentColor" fill-opacity="0.15"/><path d="M5 8l2 2 4-4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>';
            case 'in_progress':
                return '<svg viewBox="0 0 16 16" fill="none"><circle cx="8" cy="8" r="7" stroke="currentColor" stroke-width="1.5" fill="currentColor" fill-opacity="0.1"/><circle cx="8" cy="8" r="2.5" fill="currentColor"/></svg>';
            case 'cancelled':
                return '<svg viewBox="0 0 16 16" fill="none"><circle cx="8" cy="8" r="7" stroke="currentColor" stroke-width="1.5" fill="currentColor" fill-opacity="0.1"/><path d="M5.5 5.5l5 5M10.5 5.5l-5 5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>';
            default:
                return '<svg viewBox="0 0 16 16" fill="none"><circle cx="8" cy="8" r="7" stroke="currentColor" stroke-width="1.5"/></svg>';
        }
    }

    function escapeHtmlTodo(text) {
        if (!text) return '';
        return text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    }

    function finalizeTodoList() {
        const container = $('todo-sticky-container');
        if (!container) return;
        const card = $('assistant-todo-sticky');
        if (!card) return;

        const jsonString = container.dataset.todoJson;
        const title = container.dataset.todoTitle || 'Todo';
        card.remove();
        delete container.dataset.todoJson;
        delete container.dataset.todoTitle;

        let items;
        try { items = JSON.parse(jsonString); } catch { return; }
        if (!items?.length) return;

        // Find target message
        const chatContainer = $('chat-container');
        if (!chatContainer?.children) return;

        let target = null;
        for (let i = chatContainer.children.length - 1; i >= 0; i--) {
            const node = chatContainer.children[i];
            if (!node?.classList) continue;
            if (node.id === 'streaming-message') { target = node; break; }
            if (node.classList.contains('chat-message')
                && node.classList.contains('assistant')
                && node.dataset?.transient !== 'true') {
                target = node;
                break;
            }
        }
        if (!target) return;

        target.querySelector('.todo-finalized')?.remove();

        const completed = items.filter(it => it.status === 'completed').length;

        const chevron = h('span', { className: 'todo-finalized-chevron', textContent: '\u25b8' });
        const body = h('div', { className: 'todo-finalized-body' });
        body.innerHTML = buildTodoHtml(items, title);

        const toggle = h('button', {
            type: 'button',
            className: 'todo-finalized-toggle',
            onclick: () => {
                const expanded = body.classList.toggle('open');
                chevron.style.transform = expanded ? 'rotate(90deg)' : 'rotate(0deg)';
            }
        },
            h('span', { className: 'todo-finalized-summary', textContent: `${title} (${completed}/${items.length})` }),
            chevron
        );

        target.appendChild(h('div', { className: 'todo-finalized' }, toggle, body));
    }

    function clearTodoList() {
        const container = $('todo-sticky-container');
        if (container) {
            container.innerHTML = '';
            delete container.dataset.todoJson;
            delete container.dataset.todoTitle;
        }
    }

    // ── Link interception ───────────────────────────
    function isExternalUrl(href) {
        if (!href) return false;
        if (href.startsWith('http://') || href.startsWith('https://')) return true;
        // Support custom URI schemes (e.g. obsidian://, vscode://, slack://)
        // but exclude internal browser schemes
        const internalSchemes = ['about:', 'data:', 'javascript:', 'blob:'];
        if (internalSchemes.some(s => href.startsWith(s))) return false;
        return /^[a-zA-Z][a-zA-Z0-9+\-.]*:\/\//.test(href);
    }

    document.addEventListener('click', (event) => {
        let target = event.target;
        while (target && target !== document) {
            if (target.tagName?.toLowerCase() === 'a') {
                const href = target.getAttribute('href') || target.href;
                if (isExternalUrl(href)) {
                    event.preventDefault();
                    event.stopPropagation();
                    if (window.alinaBrowserBridge?.handleOpenUrl) {
                        window.alinaBrowserBridge.handleOpenUrl(href);
                    }
                }
                return;
            }
            target = target.parentElement;
        }
    }, true);
