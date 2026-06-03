(function initDashboardSummary() {
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

    function showError(message) {
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
        setText('dash-stat-records', Number(summary.totalRecords || 0).toLocaleString());

        if (summary.latestUpdated) {
            setText('dash-stat-last', formatServerDate(summary.latestUpdated));
            var hint = summary.latestTableName ? summary.latestTableName + ' · most recent' : '';
            setText('dash-stat-last-table', hint);
        } else {
            setText('dash-stat-last', '—');
            setText('dash-stat-last-table', summary.tableCount ? 'No timestamp on tables' : 'No main tables configured');
        }

        if (summary.unavailableCount > 0) {
            var err = document.getElementById('dash-summary-error');
            if (err) {
                err.textContent = summary.unavailableCount + ' table(s) could not be counted — open Table Status for details.';
                err.hidden = false;
            }
        }

        var badge = document.getElementById('dash-summary-updated');
        if (badge) {
            var label = summary.cached ? 'Cached · ' : 'Refreshed ';
            badge.textContent = label + formatLocaleDate(new Date());
        }
    }

    function loadSummary() {
        fetch('/api/user/dashboardSummary', {
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
                setText('dash-stat-records', '—');
                setText('dash-stat-last', '—');
                setText('dash-stat-last-table', '');
                showError(err && err.message ? err.message : 'Could not load overview.');
            });
    }

    function scheduleLoad() {
        if ('requestIdleCallback' in window) {
            requestIdleCallback(loadSummary, { timeout: 2000 });
        } else {
            setTimeout(loadSummary, 150);
        }
    }

    if (document.readyState === 'complete') {
        scheduleLoad();
    } else {
        window.addEventListener('load', scheduleLoad, { once: true });
    }
})();
