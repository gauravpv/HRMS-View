(function initDashboard() {
    var tablesEl = document.getElementById('dash-stat-tables');
    if (!tablesEl) {
        return;
    }

    function setText(id, text) {
        var el = document.getElementById(id);
        if (el) {
            el.textContent = text;
        }
    }

    function showSummaryError(message) {
        var err = document.getElementById('dash-summary-error');
        if (err) {
            err.textContent = message;
            err.hidden = false;
        }
        var badge = document.getElementById('dash-summary-updated');
        if (badge) {
            badge.textContent = 'Unavailable';
        }
    }

    function formatLocaleDate(date) {
        return date.toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' });
    }

    function formatServerDate(value) {
        if (!value) {
            return '—';
        }
        var match = /^(\d{4})-(\d{2})-(\d{2})[ T](\d{2}):(\d{2}):(\d{2})/.exec(String(value));
        if (match) {
            var parsed = new Date(+match[1], +match[2] - 1, +match[3], +match[4], +match[5], +match[6]);
            if (!isNaN(parsed.getTime())) {
                return formatLocaleDate(parsed);
            }
        }
        return value;
    }

    function applySummary(summary) {
        if (!summary) {
            return;
        }
        setText('dash-stat-tables', Number(summary.tableCount || 0).toLocaleString());

        if (summary.lastActivitySummary) {
            setText('dash-stat-last-activity', summary.lastActivitySummary);
            setText('dash-stat-last-activity-time', summary.lastActivityAt
                ? formatServerDate(summary.lastActivityAt)
                : 'See audit log for full history');
        } else {
            setText('dash-stat-last-activity', 'No activity yet');
            setText('dash-stat-last-activity-time', 'Sign-in, uploads, and moves appear in the audit log');
        }

        var badge = document.getElementById('dash-summary-updated');
        if (badge) {
            badge.textContent = 'Updated ' + formatLocaleDate(new Date());
        }
    }

    function loadSummary() {
        return fetch('/api/user/dashboardSummary', {
            credentials: 'same-origin',
            headers: { Accept: 'application/json' }
        })
            .then(function (response) {
                if (!response.ok) {
                    return response.json().then(function (body) {
                        throw new Error(body && body.errorMsg ? body.errorMsg : 'Request failed');
                    });
                }
                return response.json();
            })
            .then(function (data) {
                applySummary(data && data.result ? data.result : null);
            })
            .catch(function (err) {
                setText('dash-stat-tables', '—');
                setText('dash-stat-last-activity', '—');
                setText('dash-stat-last-activity-time', '');
                showSummaryError(err && err.message ? err.message : 'Could not load overview.');
            });
    }

    function itemClassForAction(action) {
        if (action === 'LOGIN') return 'hrms-audit-feed__item--login';
        if (action === 'LOGOUT') return 'hrms-audit-feed__item--logout';
        if (action === 'UPLOAD') return 'hrms-audit-feed__item--upload';
        if (action === 'MOVE_TO_MASTER' || action === 'MOVE_TO_MAIN') return 'hrms-audit-feed__item--move';
        return '';
    }

    function renderAuditItem(row) {
        var li = document.createElement('li');
        li.className = 'hrms-audit-feed__item ' + itemClassForAction(row.action);

        var iconWrap = document.createElement('span');
        iconWrap.className = 'hrms-audit-feed__icon';
        iconWrap.setAttribute('aria-hidden', 'true');
        var icon = document.createElement('span');
        icon.className = 'material-symbols-outlined';
        icon.textContent = row.icon || 'history';
        iconWrap.appendChild(icon);

        var body = document.createElement('div');
        body.className = 'hrms-audit-feed__body';

        var line = document.createElement('div');
        line.className = 'hrms-audit-feed__line hrms-audit-feed__summary';
        line.textContent = row.summary || (row.user + ' — ' + (row.actionLabel || row.action));
        body.appendChild(line);

        if (row.loggedAt) {
            var meta = document.createElement('div');
            meta.className = 'hrms-audit-feed__meta';
            meta.textContent = formatServerDate(row.loggedAt);
            body.appendChild(meta);
        }

        li.appendChild(iconWrap);
        li.appendChild(body);
        return li;
    }

    function setAuditLoading(loading) {
        var el = document.getElementById('dash-audit-loading');
        if (el) {
            el.hidden = !loading;
        }
    }

    function loadAuditFeed() {
        var list = document.getElementById('dash-audit-list');
        var empty = document.getElementById('dash-audit-empty');
        var errEl = document.getElementById('dash-audit-error');
        if (!list) {
            return Promise.resolve();
        }

        setAuditLoading(true);
        if (errEl) {
            errEl.hidden = true;
            errEl.textContent = '';
        }
        if (empty) {
            empty.hidden = true;
        }
        list.hidden = true;
        list.innerHTML = '';

        return fetch('/api/user/activityLogs?limit=100', {
            credentials: 'same-origin',
            headers: { Accept: 'application/json' }
        })
            .then(function (response) {
                if (!response.ok) {
                    return response.json().then(function (body) {
                        throw new Error(body && body.errorMsg ? body.errorMsg : 'Request failed');
                    });
                }
                return response.json();
            })
            .then(function (data) {
                var rows = data && data.result ? data.result : [];
                setAuditLoading(false);
                if (!rows.length) {
                    if (empty) {
                        empty.hidden = false;
                    }
                    return;
                }
                rows.forEach(function (row) {
                    list.appendChild(renderAuditItem(row));
                });
                list.hidden = false;
            })
            .catch(function (err) {
                setAuditLoading(false);
                if (errEl) {
                    errEl.textContent = err && err.message ? err.message : 'Could not load audit log.';
                    errEl.hidden = false;
                }
            });
    }

    function startDashboard() {
        if ('requestIdleCallback' in window) {
            requestIdleCallback(function () {
                loadSummary();
                loadAuditFeed();
            }, { timeout: 2000 });
        } else {
            setTimeout(function () {
                loadSummary();
                loadAuditFeed();
            }, 150);
        }
    }

    var refreshBtn = document.getElementById('dash-audit-refresh');
    if (refreshBtn) {
        refreshBtn.addEventListener('click', loadAuditFeed);
    }

    if (document.readyState === 'complete') {
        startDashboard();
    } else {
        window.addEventListener('load', startDashboard, { once: true });
    }
})();
