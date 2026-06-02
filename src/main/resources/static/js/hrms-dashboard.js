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

    function formatTableLabel(name) {
        if (!name) {
            return '';
        }
        return String(name)
            .replace(/_master$/i, '')
            .replace(/_temp$/i, '')
            .toUpperCase();
    }

    function parseUpdated(value) {
        if (!value) {
            return null;
        }
        var match = /^(\d{4})-(\d{2})-(\d{2})[ T](\d{2}):(\d{2}):(\d{2})/.exec(String(value));
        if (match) {
            return new Date(
                +match[1],
                +match[2] - 1,
                +match[3],
                +match[4],
                +match[5],
                +match[6]
            );
        }
        var parsed = new Date(value);
        return isNaN(parsed.getTime()) ? null : parsed;
    }

    function formatLocaleDate(date) {
        return date.toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' });
    }

    $.ajax({
        type: 'GET',
        url: '/api/user/tableStatus',
        dataType: 'json',
        cache: false,
        timeout: 120000,
        success: function (data) {
            var rows = data && data.result ? data.result : [];
            var totalRecords = 0;
            var unavailableCount = 0;
            var latestDate = null;
            var latestTable = '';

            rows.forEach(function (row) {
                var count = row.recordCount != null ? row.recordCount : row.record_count;
                if (count != null && count >= 0) {
                    totalRecords += count;
                } else if (count != null && count < 0) {
                    unavailableCount += 1;
                }

                var updated = row.lastUpdated || row.last_updated;
                var displayName = row.displayName || row.display_name;
                if (!displayName) {
                    displayName = formatTableLabel(row.tableName || row.table_name || '');
                }
                var parsed = parseUpdated(updated);
                if (parsed && (!latestDate || parsed > latestDate)) {
                    latestDate = parsed;
                    latestTable = displayName;
                }
            });

            setText('dash-stat-tables', rows.length.toLocaleString());
            setText('dash-stat-records', totalRecords.toLocaleString());

            if (latestDate) {
                setText('dash-stat-last', formatLocaleDate(latestDate));
                setText('dash-stat-last-table', latestTable ? latestTable + ' · most recent' : '');
            } else {
                setText('dash-stat-last', '—');
                setText('dash-stat-last-table', rows.length ? 'No timestamp on tables' : 'No main tables configured');
            }

            if (unavailableCount > 0) {
                var err = document.getElementById('dash-summary-error');
                if (err) {
                    err.textContent = unavailableCount + ' table(s) could not be counted — open Table Status for details.';
                    err.hidden = false;
                }
            }

            var badge = document.getElementById('dash-summary-updated');
            if (badge) {
                badge.textContent = 'Refreshed ' + formatLocaleDate(new Date());
            }
        },
        error: function (xhr) {
            setText('dash-stat-tables', '—');
            setText('dash-stat-records', '—');
            setText('dash-stat-last', '—');
            setText('dash-stat-last-table', '');
            var detail = 'Could not load overview.';
            if (xhr && xhr.responseJSON && xhr.responseJSON.errorMsg) {
                detail = xhr.responseJSON.errorMsg;
            }
            showError(detail);
        }
    });
})();
