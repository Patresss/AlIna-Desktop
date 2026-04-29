    /* ══════════════════════════════════════════════════
       AlIna Chat — JavaScript Engine (2026 Edition)
       ══════════════════════════════════════════════════ */

    // ── Welcome screen ──────────────────────────────
    function showWelcomeScreen() {
        var chatContainer = document.getElementById('chat-container');
        if (!chatContainer) return;
        if (chatContainer.children.length > 0) return;

        var welcome = document.createElement('div');
        welcome.className = 'welcome-screen';
        welcome.id = 'welcome-screen';

        welcome.innerHTML =
            '<div class="welcome-logo">AI</div>' +
            '<div class="welcome-title">AlIna</div>' +
            '<div class="welcome-subtitle" id="welcome-subtitle">Type a message to start, or press <kbd>/</kbd> for quick actions.</div>';

        chatContainer.appendChild(welcome);
    }

    function updateWelcomeSubtitle(text) {
        var el = document.getElementById('welcome-subtitle');
        if (el) el.textContent = text;
    }

    function removeWelcomeScreen() {
        var welcome = document.getElementById('welcome-screen');
        if (welcome) {
            welcome.style.opacity = '0';
            welcome.style.transform = 'translateY(-10px)';
            welcome.style.transition = 'opacity 0.2s ease, transform 0.2s ease';
            setTimeout(function() { welcome.remove(); }, 200);
        }
    }

    // Show welcome screen on initial load
    document.addEventListener('DOMContentLoaded', function() {
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
        var fab = document.getElementById('scroll-to-bottom-fab');
        if (fab) return; // already initialized

        fab = document.createElement('div');
        fab.className = 'scroll-to-bottom';
        fab.id = 'scroll-to-bottom-fab';
        fab.innerHTML = '\u25BE';
        fab.onclick = function() {
            window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
        };
        document.body.appendChild(fab);

        window.addEventListener('scroll', function() {
            var scrollBottom = document.documentElement.scrollHeight - window.innerHeight - window.scrollY;
            if (scrollBottom > 300) {
                fab.classList.add('visible');
            } else {
                fab.classList.remove('visible');
            }
        });
    }

    // ── Code block enhancement ──────────────────────
    function enhanceCodeBlocks(container) {
        if (!container) return;
        var preBlocks = container.querySelectorAll('pre');
        Array.prototype.forEach.call(preBlocks, function(pre) {
            if (pre.dataset.enhanced === 'true') return;
            pre.dataset.enhanced = 'true';

            var codeEl = pre.querySelector('code');
            if (!codeEl) return;

            // Detect language from class
            var language = 'code';
            var classList = codeEl.className || '';
            var langMatch = classList.match(/language-(\w+)/);
            if (langMatch) {
                language = langMatch[1];
            }

            // Create header
            var header = document.createElement('div');
            header.className = 'code-block-header';

            var langLabel = document.createElement('span');
            langLabel.className = 'code-block-language';
            langLabel.textContent = language;
            header.appendChild(langLabel);

            var copyBtn = document.createElement('button');
            copyBtn.className = 'code-copy-btn';
            copyBtn.innerHTML = '\u2398 Copy';
            copyBtn.onclick = function(e) {
                e.stopPropagation();
                var text = codeEl.textContent || codeEl.innerText || '';
                copyToClipboard(text, copyBtn);
            };
            header.appendChild(copyBtn);

            pre.insertBefore(header, pre.firstChild);
        });
    }

    function copyToClipboard(text, buttonEl) {
        // WebView lacks a secure context, so navigator.clipboard never works.
        // Always use the Java bridge which copies via JavaFX Clipboard API.
        if (window.alinaBrowserBridge && window.alinaBrowserBridge.handleCopyText) {
            window.alinaBrowserBridge.handleCopyText(text);
            showCopiedFeedback(buttonEl);
        }
    }

    function showCopiedFeedback(buttonEl) {
        if (!buttonEl) return;
        buttonEl.classList.add('copied');
        setTimeout(function() {
            buttonEl.classList.remove('copied');
        }, 1200);
    }

    // ── Message action buttons ──────────────────────
    function addMessageActions(messageDiv) {
        if (!messageDiv || messageDiv.querySelector('.message-actions')) return;
        if (messageDiv.dataset && messageDiv.dataset.transient === 'true') return;

        var actions = document.createElement('div');
        actions.className = 'message-actions';

        var copyBtn = document.createElement('button');
        copyBtn.className = 'message-action-btn';
        copyBtn.innerHTML = '\u2398';
        copyBtn.title = 'Copy message';
        copyBtn.onclick = function(e) {
            e.stopPropagation();
            // Get text content, excluding action buttons
            var clone = messageDiv.cloneNode(true);
            var actionsClone = clone.querySelector('.message-actions');
            if (actionsClone) actionsClone.remove();
            var footerClone = clone.querySelector('.message-footer');
            if (footerClone) footerClone.remove();
            var text = clone.textContent || clone.innerText || '';
            copyToClipboard(text.trim(), copyBtn);
        };
        actions.appendChild(copyBtn);

        messageDiv.appendChild(actions);
    }

    // ── Core chat functions ─────────────────────────

    function addHtmlContent(htmlContent, messageType, notificationStyle) {
        removeWelcomeScreen();

        var div = document.createElement('div');
        div.className = 'chat-message ' + messageType + ' ' + notificationStyle;
        div.innerHTML = htmlContent;

        var chatContainer = document.getElementById('chat-container');
        chatContainer.appendChild(div);

        // Enhance code blocks and add action buttons
        enhanceCodeBlocks(div);
        addMessageActions(div);

        // Force synchronous reflow to fix WebKit border rendering bug
        void div.offsetHeight;
    }

    function showLoader() {
        removeWelcomeScreen();
        document.getElementById('loader').classList.add('active');
        document.getElementById('loader').classList.remove('user-message');
        window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
    }

    function showLoaderForUserMessage() {
        showLoader();
        document.getElementById('loader').classList.add('user-message');
    }

    function hideLoader() {
        document.getElementById('loader').classList.remove('active');
        document.getElementById('loader').classList.remove('user-message');
    }

    function showAssistantActivity(label, detail) {
        var chatContainer = document.getElementById('chat-container');
        if (!chatContainer) {
            return;
        }
        var activity = document.getElementById('assistant-activity-message');
        if (!activity) {
            activity = document.createElement('div');
            activity.className = 'chat-message assistant activity-message';
            activity.id = 'assistant-activity-message';
            activity.dataset.transient = 'true';
            activity.dataset.count = '0';
            activity.dataset.expanded = 'false';

            var shell = document.createElement('div');
            shell.className = 'activity-shell';

            var summary = document.createElement('div');
            summary.className = 'activity-summary';

            var summaryMain = document.createElement('div');
            summaryMain.className = 'activity-summary-main';

            var summaryText = document.createElement('span');
            summaryText.className = 'activity-summary-text';
            summaryText.id = 'assistant-activity-summary-text';
            summaryText.textContent = 'Tools \u00b7 ' + stripActivityPrefix(label);

            var summaryBadge = document.createElement('span');
            summaryBadge.className = 'activity-summary-badge';
            summaryBadge.id = 'assistant-activity-summary-badge';
            summaryBadge.textContent = '1';

            var toggleButton = document.createElement('button');
            toggleButton.className = 'activity-toggle';
            toggleButton.id = 'assistant-activity-toggle';
            toggleButton.type = 'button';
            toggleButton.textContent = '\u25b8';
            toggleButton.setAttribute('aria-label', 'Show details');
            toggleButton.onclick = function() {
                var shell = this.parentElement ? this.parentElement.parentElement : null;
                var activityEl = shell ? shell.parentElement : null;
                var bodyEl = shell ? shell.querySelector('.activity-body') : null;
                if (!activityEl || !bodyEl) return;
                var expanded = activityEl.dataset.expanded === 'true';
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
            };

            summaryMain.appendChild(summaryText);
            summaryMain.appendChild(summaryBadge);
            summary.appendChild(summaryMain);
            summary.appendChild(toggleButton);

            var body = document.createElement('div');
            body.className = 'activity-body';
            body.id = 'assistant-activity-body';

            shell.appendChild(summary);
            shell.appendChild(body);
            activity.appendChild(shell);
            chatContainer.appendChild(activity);
        }

        var bodyNode = document.getElementById('assistant-activity-body');
        var lastEntry = bodyNode ? bodyNode.lastElementChild : null;
        var shortLabel = stripActivityPrefix(label);
        if (lastEntry && lastEntry.dataset && lastEntry.dataset.label === label) {
            var count = parseInt(lastEntry.dataset.count ? lastEntry.dataset.count : '1', 10) + 1;
            lastEntry.dataset.count = String(count);
            var nameEl = lastEntry.querySelector('.activity-entry-name');
            if (nameEl) {
                nameEl.textContent = shortLabel + ' \u00d7' + count;
            }
            if (detail && detail.trim()) {
                var detailEl = lastEntry.querySelector('.activity-entry-detail');
                if (detailEl) {
                    detailEl.textContent = detail;
                }
            }
        } else if (bodyNode) {
            var entry = document.createElement('div');
            entry.className = 'activity-entry';
            entry.dataset.label = label;
            entry.dataset.count = '1';

            var nameSpan = document.createElement('span');
            nameSpan.className = 'activity-entry-name';
            nameSpan.textContent = shortLabel;
            entry.appendChild(nameSpan);

            if (detail && detail.trim()) {
                var detailSpan = document.createElement('span');
                detailSpan.className = 'activity-entry-detail';
                detailSpan.textContent = detail;
                entry.appendChild(detailSpan);
            }

            bodyNode.appendChild(entry);
        }

        if (activity.dataset) {
            var totalCount = parseInt(activity.dataset.count ? activity.dataset.count : '0', 10) + 1;
            activity.dataset.count = String(totalCount);
            activity.dataset.lastEntry = label;
        }

        var summaryTextNode = document.getElementById('assistant-activity-summary-text');
        if (summaryTextNode) {
            summaryTextNode.textContent = buildAssistantActivitySummary(activity.dataset.count, label);
        }
        var summaryBadgeNode = document.getElementById('assistant-activity-summary-badge');
        if (summaryBadgeNode && activity.dataset) {
            summaryBadgeNode.textContent = activity.dataset.count;
        }
        window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
    }

    function finalizeAssistantActivity() {
        var activity = document.getElementById('assistant-activity-message');
        if (!activity) {
            return;
        }
        activity.removeAttribute('id');
        var childIds = ['assistant-activity-body', 'assistant-activity-toggle',
                        'assistant-activity-summary-text', 'assistant-activity-summary-badge'];
        childIds.forEach(function(cid) {
            var el = document.getElementById(cid);
            if (el) el.removeAttribute('id');
        });
        if (activity.dataset) {
            delete activity.dataset.lastEntry;
        }
    }

    function clearAssistantActivity() {
        var activity = document.getElementById('assistant-activity-message');
        if (!activity) {
            return;
        }
        activity.remove();
    }

    function stripActivityPrefix(label) {
        return label.replace(/^OpenCode:\s*/i, '').replace(/^Skill:\s*/i, '').replace(/^MCP:\s*/i, '');
    }

    function buildAssistantActivitySummary(count, lastLabel) {
        var parsedCount = parseInt(count ? count : '0', 10);
        var safeCount = Number.isNaN(parsedCount) ? 0 : parsedCount;
        var shortLabel = stripActivityPrefix(lastLabel);
        if (safeCount <= 1) {
            return 'Tools \u00b7 ' + shortLabel;
        }
        return 'Tools \u00b7 latest: ' + shortLabel;
    }

    function toggleAssistantActivity() {
        var activity = document.getElementById('assistant-activity-message');
        if (!activity || !activity.dataset) {
            return;
        }
        var body = document.getElementById('assistant-activity-body');
        var toggle = document.getElementById('assistant-activity-toggle');
        if (!body || !toggle) {
            return;
        }
        var expanded = activity.dataset.expanded === 'true';
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
        var chatContainer = document.getElementById('chat-container');
        if (!chatContainer) {
            return;
        }
        var card = document.getElementById('assistant-reasoning-message');
        if (!card) {
            card = document.createElement('div');
            card.className = 'chat-message assistant reasoning-message';
            card.id = 'assistant-reasoning-message';
            card.dataset.transient = 'true';

            var details = document.createElement('details');
            details.className = 'reasoning-details';
            details.open = true;

            var summary = document.createElement('summary');
            summary.className = 'reasoning-summary';
            summary.textContent = title;

            var body = document.createElement('div');
            body.className = 'reasoning-body';
            body.id = 'assistant-reasoning-body';

            details.appendChild(summary);
            details.appendChild(body);
            card.appendChild(details);
            chatContainer.appendChild(card);
        }
        var bodyNode = document.getElementById('assistant-reasoning-body');
        if (bodyNode) {
            bodyNode.innerHTML = htmlContent;
        }
        window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
    }

    function showAssistantCommentary(title, htmlContent) {
        var chatContainer = document.getElementById('chat-container');
        if (!chatContainer) {
            return;
        }
        var card = document.getElementById('assistant-commentary-message');
        if (!card) {
            card = document.createElement('div');
            card.className = 'chat-message assistant commentary-message';
            card.id = 'assistant-commentary-message';
            card.dataset.transient = 'true';

            var details = document.createElement('details');
            details.className = 'commentary-details';
            details.open = true;

            var summary = document.createElement('summary');
            summary.className = 'commentary-summary';
            summary.textContent = title;

            var body = document.createElement('div');
            body.className = 'commentary-body';
            body.id = 'assistant-commentary-body';

            details.appendChild(summary);
            details.appendChild(body);
            card.appendChild(details);
            chatContainer.appendChild(card);
        }
        var bodyNode = document.getElementById('assistant-commentary-body');
        if (bodyNode) {
            bodyNode.innerHTML = htmlContent;
        }
        window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
    }

    function finalizeAssistantReasoning() {
        var card = document.getElementById('assistant-reasoning-message');
        if (!card) {
            return;
        }
        card.removeAttribute('id');
        var body = document.getElementById('assistant-reasoning-body');
        if (body) {
            body.removeAttribute('id');
        }
    }

    function finalizeAssistantCommentary() {
        var card = document.getElementById('assistant-commentary-message');
        if (!card) {
            return;
        }
        card.removeAttribute('id');
        var body = document.getElementById('assistant-commentary-body');
        if (body) {
            body.removeAttribute('id');
        }
    }

    function clearAssistantReasoning() {
        var card = document.getElementById('assistant-reasoning-message');
        if (!card) {
            return;
        }
        card.remove();
    }

    function clearAssistantCommentary() {
        var card = document.getElementById('assistant-commentary-message');
        if (!card) {
            return;
        }
        card.remove();
    }

    function attachProcessPanelToLastAssistantMessage(summaryText, reasoningTitle, reasoningHtml, commentaryTitle, commentaryHtml, toolsHtml) {
        var chatContainer = document.getElementById('chat-container');
        if (!chatContainer || !chatContainer.children) {
            return;
        }

        var target = null;
        for (var i = chatContainer.children.length - 1; i >= 0; i--) {
            var node = chatContainer.children[i];
            if (!node || !node.classList) {
                continue;
            }
            if (node.id === 'streaming-message') {
                target = node;
                break;
            }
            if (node.classList.contains('chat-message')
                && node.classList.contains('assistant')
                && (!node.dataset || node.dataset.transient !== 'true')) {
                target = node;
                break;
            }
        }

        if (!target) {
            return;
        }

        var existing = target.querySelector('.assistant-process');
        if (existing) {
            existing.remove();
        }

        var hasReasoning = reasoningHtml && reasoningHtml.trim() !== '';
        var hasCommentary = commentaryHtml && commentaryHtml.trim() !== '';
        var hasTools = toolsHtml && toolsHtml.trim() !== '';
        if (!hasReasoning && !hasCommentary && !hasTools) {
            return;
        }

        var shell = document.createElement('div');
        shell.className = 'assistant-process';

        var toggle = document.createElement('button');
        toggle.type = 'button';
        toggle.className = 'assistant-process-toggle';

        var summary = document.createElement('span');
        summary.className = 'assistant-process-summary';
        summary.textContent = summaryText ? summaryText : 'Process';

        var chevron = document.createElement('span');
        chevron.className = 'assistant-process-chevron';
        chevron.textContent = '\u25b8';

        toggle.appendChild(summary);
        toggle.appendChild(chevron);

        var body = document.createElement('div');
        body.className = 'assistant-process-body';

        function appendSection(title, html) {
            if (!html || html.trim() === '') {
                return;
            }
            var section = document.createElement('div');
            section.className = 'assistant-process-section';

            var titleNode = document.createElement('div');
            titleNode.className = 'assistant-process-title';
            titleNode.textContent = title;

            var contentNode = document.createElement('div');
            contentNode.className = 'assistant-process-content';
            contentNode.innerHTML = html;

            section.appendChild(titleNode);
            section.appendChild(contentNode);
            body.appendChild(section);
        }

        appendSection(reasoningTitle, reasoningHtml);
        appendSection(commentaryTitle, commentaryHtml);
        appendSection('Tools', toolsHtml);

        toggle.onclick = function() {
            var expanded = body.classList.contains('open');
            if (expanded) {
                body.classList.remove('open');
                chevron.textContent = '\u25b8';
            } else {
                body.classList.add('open');
                chevron.textContent = '\u25be';
            }
        };

        shell.appendChild(toggle);
        shell.appendChild(body);
        target.appendChild(shell);
    }

    function showAssistantPermissionRequest(requestId, title, message, approveLabel, approveAlwaysLabel, denyLabel) {
        var chatContainer = document.getElementById('chat-container');
        if (!chatContainer || !requestId) {
            return;
        }

        var messageId = 'assistant-permission-' + requestId;
        var card = document.getElementById(messageId);
        if (!card) {
            card = document.createElement('div');
            card.className = 'chat-message assistant permission-message';
            card.id = messageId;
            card.dataset.transient = 'true';

            var shell = document.createElement('div');
            shell.className = 'permission-shell';

            var header = document.createElement('div');
            header.className = 'permission-header';

            var titleNode = document.createElement('div');
            titleNode.className = 'permission-title';
            titleNode.textContent = title;
            header.appendChild(titleNode);

            var badge = document.createElement('div');
            badge.className = 'permission-badge';
            badge.textContent = 'Approval';
            header.appendChild(badge);
            shell.appendChild(header);

            var body = document.createElement('div');
            body.className = 'permission-message-body';
            body.textContent = message;
            shell.appendChild(body);

            var actions = document.createElement('div');
            actions.className = 'permission-actions';

            function createButton(label, cssClass, actionName) {
                var button = document.createElement('button');
                button.type = 'button';
                button.className = cssClass;
                button.textContent = label;
                button.onclick = function() {
                    markAssistantPermissionRequestPending(requestId, label + '...');
                    if (window.alinaBrowserBridge && window.alinaBrowserBridge.handlePermissionAction) {
                        window.alinaBrowserBridge.handlePermissionAction(requestId, actionName);
                    } else if (window.alert) {
                        window.alert('__ALINA_PERMISSION__|' + requestId + '|' + actionName);
                    }
                };
                return button;
            }

            actions.appendChild(createButton(approveLabel, 'primary', 'APPROVE_ONCE'));
            actions.appendChild(createButton(approveAlwaysLabel, 'always', 'APPROVE_ALWAYS'));
            actions.appendChild(createButton(denyLabel, 'deny', 'DENY'));
            shell.appendChild(actions);

            var status = document.createElement('div');
            status.className = 'permission-status';
            status.id = messageId + '-status';
            shell.appendChild(status);

            card.appendChild(shell);

            chatContainer.appendChild(card);
        }

        window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
    }

    function markAssistantPermissionRequestPending(requestId, statusLabel) {
        var card = document.getElementById('assistant-permission-' + requestId);
        if (!card) {
            return;
        }
        var buttons = card.querySelectorAll('button');
        Array.prototype.forEach.call(buttons, function(button) {
            button.disabled = true;
        });
        var status = document.getElementById('assistant-permission-' + requestId + '-status');
        if (status) {
            status.textContent = statusLabel ? statusLabel : '';
        }
        window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
    }

    function resolveAssistantPermissionRequest(requestId, statusLabel) {
        var card = document.getElementById('assistant-permission-' + requestId);
        if (!card) {
            return;
        }
        var buttons = card.querySelectorAll('button');
        Array.prototype.forEach.call(buttons, function(button) {
            button.disabled = true;
        });
        var status = document.getElementById('assistant-permission-' + requestId + '-status');
        if (status) {
            status.textContent = statusLabel ? statusLabel : '';
        }
        window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
    }

    // ── Streaming messages ──────────────────────────

    function startStreamingAssistantMessage() {
        removeWelcomeScreen();
        var chatContainer = document.getElementById('chat-container');
        var streamingDiv = null;

        if (arguments.length > 0 && arguments[0] === true) {
            var target = document.getElementById('regenerate-target');
            if (target) {
                streamingDiv = target;
            }
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
        var streamingDiv = document.getElementById('streaming-message');
        if (streamingDiv) {
            // Use textContent to preserve markdown syntax during streaming
            if (streamingDiv.textContent === undefined) {
                streamingDiv.innerText += escapedToken;
            } else {
                streamingDiv.textContent += escapedToken;
            }
            window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
        }
    }

    function updateStreamingMessageWithHtml(htmlContent) {
        var streamingDiv = document.getElementById('streaming-message');
        if (streamingDiv) {
            // Update with processed HTML content in real-time
            streamingDiv.innerHTML = htmlContent;
            // Enhance code blocks in real-time
            enhanceCodeBlocks(streamingDiv);
            // Force synchronous reflow to fix WebKit border rendering bug
            void streamingDiv.offsetHeight;
            window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
        }
    }

    function finishStreamingMessage() {
        var streamingDiv = document.getElementById('streaming-message');
        if (streamingDiv) {
            // Remove the streaming ID so it becomes a regular message
            streamingDiv.removeAttribute('id');
            // Mark this element so attachMessageFooter can find it even if
            // new notification messages are appended before the footer arrives
            streamingDiv.dataset.awaitingFooter = 'true';
            // Enhance code blocks and add message actions
            enhanceCodeBlocks(streamingDiv);
            addMessageActions(streamingDiv);
            // Force synchronous reflow to fix WebKit border rendering bug
            void streamingDiv.offsetHeight;
            // Scroll to bottom one final time
            window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
        }
    }

    function finishStreamingMessageWithMarkdown(processedHtml) {
        var streamingDiv = document.getElementById('streaming-message');
        if (streamingDiv) {
            // Replace raw content with processed markdown HTML
            streamingDiv.innerHTML = processedHtml;
            // Remove the streaming ID so it becomes a regular message
            streamingDiv.removeAttribute('id');
            // Mark this element so attachMessageFooter can find it
            streamingDiv.dataset.awaitingFooter = 'true';
            // Enhance code blocks and add message actions
            enhanceCodeBlocks(streamingDiv);
            addMessageActions(streamingDiv);
            // Force synchronous reflow to fix WebKit border rendering bug
            void streamingDiv.offsetHeight;
            // Scroll to bottom one final time
            window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
        }
    }

    function scrollToBottom() {
        window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
    }

    function attachMessageFooter(footerText) {
        var chatContainer = document.getElementById('chat-container');
        if (!chatContainer || !chatContainer.children) return;

        var target = null;
        for (var i = chatContainer.children.length - 1; i >= 0; i--) {
            var node = chatContainer.children[i];
            if (!node || !node.classList) continue;
            if (node.dataset && node.dataset.awaitingFooter === 'true') {
                target = node;
                break;
            }
        }

        if (!target) {
            for (var j = chatContainer.children.length - 1; j >= 0; j--) {
                var fallbackNode = chatContainer.children[j];
                if (!fallbackNode || !fallbackNode.classList) continue;
                if (fallbackNode.classList.contains('chat-message')
                    && fallbackNode.classList.contains('assistant')
                    && (!fallbackNode.dataset || fallbackNode.dataset.transient !== 'true')) {
                    target = fallbackNode;
                    break;
                }
            }
        }

        if (!target) return;

        delete target.dataset.awaitingFooter;

        var existing = target.querySelector('.message-footer');
        if (existing) existing.remove();
        var footer = document.createElement('div');
        footer.className = 'message-footer';
        footer.textContent = footerText;
        target.appendChild(footer);
    }

    // ── Regeneration ────────────────────────────────

    function prepareRegenerationTarget() {
        var chatContainer = document.getElementById('chat-container');
        if (!chatContainer || !chatContainer.children) {
            return false;
        }

        for (var i = chatContainer.children.length - 1; i >= 0; i--) {
            var node = chatContainer.children[i];
            if (!node || !node.classList) {
                continue;
            }
            if (node.classList.contains('chat-message')
                && node.classList.contains('assistant')
                && (!node.dataset || node.dataset.transient !== 'true')) {
                if (node.dataset) {
                    node.dataset.prevHtml = node.innerHTML;
                }
                node.id = 'regenerate-target';
                return true;
            }
        }
        return false;
    }

    function restoreRegenerationTarget() {
        var node = document.getElementById('streaming-message');
        if (!node) {
            node = document.getElementById('regenerate-target');
        }
        if (!node) {
            return;
        }

        if (node.dataset && node.dataset.prevHtml !== undefined) {
            node.innerHTML = node.dataset.prevHtml;
            delete node.dataset.prevHtml;
        }
        node.removeAttribute('id');
        window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
    }

    function discardRegenerationBackup() {
        var node = document.getElementById('streaming-message');
        if (!node) {
            node = document.getElementById('regenerate-target');
        }
        if (!node) {
            return;
        }
        if (node.dataset && node.dataset.prevHtml !== undefined) {
            delete node.dataset.prevHtml;
        }
    }

    // ── Todo list ───────────────────────────────────

    function showTodoList(jsonString, title) {
        var items;
        try {
            items = JSON.parse(jsonString);
        } catch (e) {
            return;
        }
        if (!items || items.length === 0) {
            return;
        }

        var container = document.getElementById('todo-sticky-container');
        if (!container) {
            return;
        }

        container.dataset.todoJson = jsonString;
        container.dataset.todoTitle = title;

        var card = document.getElementById('assistant-todo-sticky');
        if (!card) {
            card = document.createElement('div');
            card.className = 'todo-sticky';
            card.id = 'assistant-todo-sticky';
            container.appendChild(card);
        }

        card.innerHTML = buildTodoHtml(items, title);
        window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
    }

    function buildTodoHtml(items, title) {
        var completed = 0;
        var total = items.length;
        for (var i = 0; i < items.length; i++) {
            if (items[i].status === 'completed') completed++;
        }

        var html = '';

        html += '<div class="todo-header">';
        html += '<div class="todo-title">' + escapeHtmlTodo(title) + '</div>';
        html += '<div class="todo-progress-info">' + completed + '/' + total + '</div>';
        html += '</div>';

        var percent = total > 0 ? Math.round((completed / total) * 100) : 0;
        html += '<div class="todo-progress-bar-track">';
        html += '<div class="todo-progress-bar-fill" style="width:' + percent + '%"></div>';
        html += '</div>';

        html += '<div class="todo-items">';
        for (var j = 0; j < items.length; j++) {
            var item = items[j];
            var statusClass = 'todo-status-' + (item.status || 'pending');
            var priorityClass = 'todo-priority-' + (item.priority || 'medium');
            var icon = getTodoStatusIcon(item.status);

            html += '<div class="todo-item ' + statusClass + ' ' + priorityClass + '">';
            html += '<span class="todo-item-icon">' + icon + '</span>';
            html += '<span class="todo-item-content">' + escapeHtmlTodo(item.content) + '</span>';
            html += '</div>';
        }
        html += '</div>';

        return html;
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
        var container = document.getElementById('todo-sticky-container');
        if (!container) {
            return;
        }
        var card = document.getElementById('assistant-todo-sticky');
        if (!card) {
            return;
        }
        var jsonString = container.dataset.todoJson;
        var title = container.dataset.todoTitle || 'Todo';
        card.remove();
        delete container.dataset.todoJson;
        delete container.dataset.todoTitle;

        var items;
        try {
            items = JSON.parse(jsonString);
        } catch (e) {
            return;
        }
        if (!items || items.length === 0) {
            return;
        }

        var chatContainer = document.getElementById('chat-container');
        if (!chatContainer || !chatContainer.children) {
            return;
        }
        var target = null;
        for (var i = chatContainer.children.length - 1; i >= 0; i--) {
            var node = chatContainer.children[i];
            if (!node || !node.classList) continue;
            if (node.id === 'streaming-message') {
                target = node;
                break;
            }
            if (node.classList.contains('chat-message')
                && node.classList.contains('assistant')
                && (!node.dataset || node.dataset.transient !== 'true')) {
                target = node;
                break;
            }
        }
        if (!target) {
            return;
        }

        var existing = target.querySelector('.todo-finalized');
        if (existing) existing.remove();

        var completed = 0;
        for (var k = 0; k < items.length; k++) {
            if (items[k].status === 'completed') completed++;
        }

        var shell = document.createElement('div');
        shell.className = 'todo-finalized';

        var toggle = document.createElement('button');
        toggle.type = 'button';
        toggle.className = 'todo-finalized-toggle';

        var summary = document.createElement('span');
        summary.className = 'todo-finalized-summary';
        summary.textContent = title + ' (' + completed + '/' + items.length + ')';

        var chevron = document.createElement('span');
        chevron.className = 'todo-finalized-chevron';
        chevron.textContent = '\u25b8';

        toggle.appendChild(summary);
        toggle.appendChild(chevron);

        var body = document.createElement('div');
        body.className = 'todo-finalized-body';
        body.innerHTML = buildTodoHtml(items, title);

        toggle.onclick = function() {
            var expanded = body.classList.contains('open');
            if (expanded) {
                body.classList.remove('open');
                chevron.style.transform = 'rotate(0deg)';
            } else {
                body.classList.add('open');
                chevron.style.transform = 'rotate(90deg)';
            }
        };

        shell.appendChild(toggle);
        shell.appendChild(body);
        target.appendChild(shell);
    }

    function clearTodoList() {
        var container = document.getElementById('todo-sticky-container');
        if (container) {
            container.innerHTML = '';
            delete container.dataset.todoJson;
            delete container.dataset.todoTitle;
        }
    }

    // ── Link interception ───────────────────────────
    document.addEventListener('click', function(event) {
        var target = event.target;
        while (target && target !== document) {
            if (target.tagName && target.tagName.toLowerCase() === 'a') {
                var href = target.getAttribute('href') || target.href;
                if (href && (href.indexOf('http://') === 0 || href.indexOf('https://') === 0)) {
                    event.preventDefault();
                    event.stopPropagation();
                    if (window.alinaBrowserBridge && window.alinaBrowserBridge.handleOpenUrl) {
                        window.alinaBrowserBridge.handleOpenUrl(href);
                    } else if (window.alert) {
                        window.alert('__ALINA_OPEN_URL__|' + href);
                    }
                }
                return;
            }
            target = target.parentElement;
        }
    }, true);


