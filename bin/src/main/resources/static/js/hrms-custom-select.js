/**
 * Replaces native <select> appearance with a themed listbox while keeping
 * the original select in the DOM for jQuery / inline handlers.
 */
(function (global) {
    var openWrap = null;

    function getSelectedOption(select) {
        var i = select.selectedIndex;
        return i >= 0 ? select.options[i] : null;
    }

    function updateTrigger(select, triggerText) {
        var opt = getSelectedOption(select);
        var text = opt ? opt.text : '';
        var isPlaceholder = !select.value || (opt && opt.disabled);
        triggerText.textContent = text;
        triggerText.classList.toggle('is-placeholder', isPlaceholder);
    }

    function resetPanelLayout(panel) {
        if (!panel) return;
        panel.classList.remove('is-floating');
        panel.style.position = '';
        panel.style.left = '';
        panel.style.top = '';
        panel.style.bottom = '';
        panel.style.width = '';
        panel.style.maxHeight = '';
        panel.style.zIndex = '';
    }

    function positionFloatingPanel(wrap, panel) {
        var trigger = wrap.querySelector('.hrms-select-trigger');
        if (!trigger) return;
        var rect = trigger.getBoundingClientRect();
        var gap = 6;
        var spaceBelow = window.innerHeight - rect.bottom - gap - 8;
        var spaceAbove = rect.top - gap - 8;
        var maxHeight = Math.min(16 * 16, Math.max(spaceBelow, spaceAbove, 160));

        panel.classList.add('is-floating');
        panel.style.position = 'fixed';
        panel.style.left = Math.max(8, rect.left) + 'px';
        panel.style.width = rect.width + 'px';
        panel.style.zIndex = '10200';

        if (spaceBelow >= 160 || spaceBelow >= spaceAbove) {
            panel.style.top = (rect.bottom + gap) + 'px';
            panel.style.bottom = '';
            panel.style.maxHeight = Math.min(maxHeight, spaceBelow) + 'px';
        } else {
            panel.style.top = '';
            panel.style.bottom = (window.innerHeight - rect.top + gap) + 'px';
            panel.style.maxHeight = Math.min(maxHeight, spaceAbove) + 'px';
        }
    }

    function closePanel(wrap) {
        if (!wrap) return;
        wrap.classList.remove('is-open');
        var trigger = wrap.querySelector('.hrms-select-trigger');
        if (trigger) trigger.setAttribute('aria-expanded', 'false');
        resetPanelLayout(wrap.querySelector('.hrms-select-panel'));
        if (openWrap === wrap) openWrap = null;
    }

    function repositionOpenPanel() {
        if (!openWrap) return;
        var panel = openWrap.querySelector('.hrms-select-panel');
        if (panel) positionFloatingPanel(openWrap, panel);
    }

    function closeAllExcept(wrap) {
        document.querySelectorAll('.hrms-select-wrap.is-open').forEach(function (w) {
            if (w !== wrap) closePanel(w);
        });
    }

    function closeAll() {
        document.querySelectorAll('.hrms-select-wrap.is-open').forEach(closePanel);
    }

    function buildOptions(select, panel, wrap, triggerText) {
        panel.innerHTML = '';
        Array.from(select.options).forEach(function (opt) {
            var btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'hrms-select-option';
            btn.setAttribute('role', 'option');
            btn.dataset.value = opt.value;
            btn.textContent = opt.text;
            if (opt.disabled) {
                btn.disabled = true;
                btn.classList.add('is-disabled');
            }
            if (opt.value === select.value) btn.classList.add('is-selected');
            if (!opt.value) btn.classList.add('is-placeholder');

            btn.addEventListener('click', function () {
                if (opt.disabled) return;
                select.value = opt.value;
                select.dispatchEvent(new Event('change', { bubbles: true }));
                panel.querySelectorAll('.hrms-select-option').forEach(function (el) {
                    el.classList.remove('is-selected');
                });
                btn.classList.add('is-selected');
                updateTrigger(select, triggerText);
                closePanel(wrap);
            });
            panel.appendChild(btn);
        });
    }

    function destroyWrap(wrap) {
        if (!wrap) return;
        closePanel(wrap);
        wrap.querySelectorAll('.hrms-select-trigger, .hrms-select-panel').forEach(function (el) {
            el.remove();
        });
        if (wrap._hrmsSelectObserver) {
            wrap._hrmsSelectObserver.disconnect();
            delete wrap._hrmsSelectObserver;
        }
        delete wrap.dataset.hrmsCustomized;
        var select = wrap.querySelector('select.hrms-select');
        if (select) {
            select.classList.remove('hrms-select-native');
            select.tabIndex = 0;
        }
        if (!wrap.querySelector('.hrms-select-chevron')) {
            var chevron = document.createElement('span');
            chevron.className = 'material-symbols-outlined hrms-select-chevron';
            chevron.textContent = 'expand_more';
            wrap.appendChild(chevron);
        }
    }

    function refreshWrap(wrap) {
        if (!wrap) return;
        var select = wrap.querySelector('select.hrms-select');
        if (!select) return;
        if (wrap.dataset.hrmsCustomized !== '1') {
            initWrap(wrap);
            return;
        }
        var panel = wrap.querySelector('.hrms-select-panel');
        var triggerText = wrap.querySelector('.hrms-select-trigger-text');
        if (panel && triggerText) {
            buildOptions(select, panel, wrap, triggerText);
            updateTrigger(select, triggerText);
        }
    }

    function initWrap(wrap) {
        var select = wrap.querySelector('select.hrms-select');
        if (!select) return;
        if (wrap.dataset.hrmsCustomized === '1') {
            refreshWrap(wrap);
            return;
        }
        if (wrap.querySelector('.hrms-select-trigger')) {
            destroyWrap(wrap);
        }

        wrap.dataset.hrmsCustomized = '1';

        var chevron = wrap.querySelector('.hrms-select-chevron');
        if (chevron) chevron.remove();

        select.classList.add('hrms-select-native');
        select.tabIndex = -1;

        var trigger = document.createElement('button');
        trigger.type = 'button';
        trigger.className = 'hrms-select-trigger';
        trigger.setAttribute('aria-haspopup', 'listbox');
        trigger.setAttribute('aria-expanded', 'false');

        var triggerText = document.createElement('span');
        triggerText.className = 'hrms-select-trigger-text';
        var triggerChevron = document.createElement('span');
        triggerChevron.className = 'material-symbols-outlined hrms-select-trigger-chevron';
        triggerChevron.textContent = 'expand_more';
        trigger.appendChild(triggerText);
        trigger.appendChild(triggerChevron);

        var panel = document.createElement('div');
        panel.className = 'hrms-select-panel';
        panel.setAttribute('role', 'listbox');

        wrap.appendChild(trigger);
        wrap.appendChild(panel);

        function refresh() {
            buildOptions(select, panel, wrap, triggerText);
            updateTrigger(select, triggerText);
            trigger.disabled = select.disabled;
        }

        trigger.addEventListener('click', function (e) {
            e.stopPropagation();
            if (select.disabled) return;
            var willOpen = !wrap.classList.contains('is-open');
            closeAllExcept(wrap);
            if (willOpen) {
                refresh();
                wrap.classList.add('is-open');
                trigger.setAttribute('aria-expanded', 'true');
                openWrap = wrap;
                positionFloatingPanel(wrap, panel);
            } else {
                closePanel(wrap);
            }
        });

        select.addEventListener('change', function () {
            updateTrigger(select, triggerText);
            panel.querySelectorAll('.hrms-select-option').forEach(function (el) {
                el.classList.toggle('is-selected', el.dataset.value === select.value);
            });
        });

        wrap._hrmsSelectObserver = new MutationObserver(refresh);
        wrap._hrmsSelectObserver.observe(select, {
            childList: true,
            subtree: true,
            attributes: true,
            attributeFilter: ['selected', 'disabled']
        });

        refresh();
    }

    document.addEventListener('click', function () {
        if (openWrap) closePanel(openWrap);
    });

    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape' && openWrap) closePanel(openWrap);
    });

    window.addEventListener('resize', repositionOpenPanel);
    window.addEventListener('scroll', repositionOpenPanel, true);

    function initAll(root) {
        (root || document).querySelectorAll('.hrms-select-wrap').forEach(initWrap);
    }

    global.HrmsCustomSelect = {
        initAll: initAll,
        refreshWrap: refreshWrap,
        destroyWrap: destroyWrap,
        closeAll: closeAll
    };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', function () { initAll(); });
    } else {
        initAll();
    }
})(window);
