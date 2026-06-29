(function initTableStatusPage() {
    var statusTable = null;
    var lastLoadedAt = null;

    function setEmptyState(visible, title, detail) {
        var el = document.getElementById('table-status-empty');
        if (!el) return;
        el.classList.toggle('is-visible', visible);
        if (title) {
            var heading = el.querySelector('.font-headline-sm');
            if (heading) heading.textContent = title;
        }
        if (detail) {
            var desc = el.querySelector('.hrms-empty-detail');
            if (desc) desc.textContent = detail;
        }
    }

    function updateLoadedBadge() {
        var badge = document.getElementById('table-status-updated');
        if (!badge || !lastLoadedAt) return;
        badge.textContent = 'Updated ' + lastLoadedAt.toLocaleString(undefined, {
            dateStyle: 'medium',
            timeStyle: 'short'
        });
    }

    function formatTableDisplayName(name) {
        if (typeof hrmsFormatTableDisplayName === 'function') {
            return hrmsFormatTableDisplayName(name);
        }
        if (!name) return '';
        return String(name).replace(/_master$/i, '').replace(/_temp$/i, '').toUpperCase();
    }

    function normalizeRow(row) {
        if (!row) return null;
        var rawName = row.displayName || row.display_name || row.tableName || row.table_name || '';
        return {
            displayName: formatTableDisplayName(rawName),
            tableName: row.tableName || row.table_name || '',
            recordCount: row.recordCount != null ? row.recordCount : row.record_count,
            lastUpdated: row.lastUpdated || row.last_updated || ''
        };
    }

    function formatCount(value) {
        if (value == null || value < 0) {
            return 'Unavailable';
        }
        return Number(value).toLocaleString();
    }

    function renderStatusTable(rows) {
        var table = $('#tableStatusData');
        if ($.fn.DataTable.isDataTable(table)) {
            table.DataTable().clear().destroy();
            statusTable = null;
        }
        table.find('thead tr').empty();
        table.find('tbody').empty();

        var data = (rows || []).map(normalizeRow).filter(Boolean);

        statusTable = table.DataTable({
            data: data,
            columns: [
                {
                    title: 'Table',
                    data: 'displayName',
                    defaultContent: ''
                },
                {
                    title: 'Records',
                    data: 'recordCount',
                    className: 'dt-center hrms-table-status-count',
                    render: function (value, type) {
                        if (type === 'sort' || type === 'type') {
                            return value == null || value < 0 ? -1 : value;
                        }
                        return '<span class="hrms-table-status-count__value">' + formatCount(value) + '</span>';
                    }
                },
                {
                    title: 'Last updated',
                    data: 'lastUpdated',
                    className: 'dt-center hrms-table-status-updated',
                    render: function (value, type) {
                        if (type === 'sort' || type === 'type') {
                            return value || '';
                        }
                        var text = value ? value : '—';
                        return '<span class="hrms-table-status-updated__value">' + text + '</span>';
                    }
                }
            ],
            order: [[0, 'asc']],
            paging: false,
            deferRender: true,
            autoWidth: false,
            dom: '<"hrms-dt-toolbar"f>rt',
            initComplete: function () {
                var $table = $(this.api().table().node());
                if (!$table.parent().hasClass('hrms-dt-table-area')) {
                    $table.wrap('<div class="hrms-dt-table-area custom-scrollbar"></div>');
                }
            },
            language: {
                search: '',
                searchPlaceholder: 'Filter tables…',
                zeroRecords: 'No matching tables'
            }
        });

        table.addClass('hrms-data-table display compact stripe');
        $('#table-status-shell').css('visibility', 'visible');
        setEmptyState(false);
    }

    function loadTableStatus(forceRefresh) {
        setEmptyState(true, 'Loading table status…', 'Fetching counts from the database.');
        $('#table-status-shell').css('visibility', 'hidden');

        var url = '/api/user/tableStatus';
        if (forceRefresh) {
            url += '?refresh=true';
        }

        $.ajax({
            type: 'GET',
            url: url,
            dataType: 'json',
            cache: false,
            timeout: 120000,
            success: function (data) {
                try {
                    if (data && data.result && data.result.length) {
                        lastLoadedAt = new Date();
                        updateLoadedBadge();
                        renderStatusTable(data.result);
                    } else {
                        setEmptyState(true, 'No main tables found', 'No rows with table_type main_table in table_details.');
                        $('#table-status-shell').css('visibility', 'hidden');
                        var badge = document.getElementById('table-status-updated');
                        if (badge) badge.textContent = 'No data';
                    }
                } catch (e) {
                    console.error('Table status render failed', e);
                    setEmptyState(true, 'Could not display table status', e.message || 'Grid failed to initialize.');
                    $('#table-status-shell').css('visibility', 'hidden');
                }
            },
            error: function (xhr) {
                var detail = hrmsAjaxErrorMessage(xhr);
                if (xhr && xhr.responseJSON && xhr.responseJSON.errorMsg) {
                    detail = xhr.responseJSON.errorMsg;
                }
                setEmptyState(true, 'Unable to load table status', detail);
                $('#table-status-shell').css('visibility', 'hidden');
                var badge = document.getElementById('table-status-updated');
                if (badge) badge.textContent = 'Load failed';
            }
        });
    }

    $('#table-status-refresh').on('click', function () {
        loadTableStatus(true);
    });

    loadTableStatus(false);
})();
