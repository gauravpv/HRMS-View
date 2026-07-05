var historyTableData = [];

function hrmsCoerceApiRows(result) {
    if (!result) {
        return [];
    }
    if (typeof result === 'string') {
        try {
            result = JSON.parse(result);
        } catch (e) {
            return [];
        }
    }
    if (Array.isArray(result)) {
        return result;
    }
    if (typeof result === 'object') {
        return Object.keys(result)
            .filter(function (k) { return /^\d+$/.test(k); })
            .sort(function (a, b) { return Number(a) - Number(b); })
            .map(function (k) { return result[k]; });
    }
    return [];
}

function hrmsSnapshotId(item) {
    var id = hrmsRowField(item, ['historyId', 'HISTORY_ID', 'history_id', 'HID', 'hid', 'ID', 'id']);
    if (id == null || id === '') {
        id = hrmsFirstRowValue(item);
    }
    return id;
}

function hrmsSnapshotDate(item) {
    return hrmsRowField(item, [
        'snapshotDate', 'DATE', 'date', 'HISTORY_DATE', 'history_date',
        'CREATED_DATE', 'created_date', 'SNAPSHOT_DATE', 'snapshot_date'
    ]);
}

function populateSnapshotDropdown(rows) {
    var dropdown = $('#historyId');
    dropdown.empty().append($('<option>', {
        value: '',
        text: 'Choose snapshot…',
        disabled: true,
        selected: true
    }));

    var seen = {};
    var added = 0;
    rows.forEach(function (item) {
        var id = hrmsSnapshotId(item);
        if (id == null || id === '') {
            return;
        }
        var date = hrmsSnapshotDate(item);
        var key = String(id) + '|' + String(date != null ? date : '');
        if (seen[key]) {
            return;
        }
        seen[key] = true;
        var label = date != null && date !== '' ? id + ' | ' + date : String(id);
        dropdown.append($('<option>', { value: String(id), text: label }));
        added += 1;
    });

    var wrap = document.querySelector('#historyId') && document.querySelector('#historyId').closest('.hrms-select-wrap');
    if (window.HrmsCustomSelect && wrap) {
        HrmsCustomSelect.refreshWrap(wrap);
    }
    return added;
}

function showSnapshotField() {
    var row = document.getElementById('historyActionsRow');
    if (row) {
        row.classList.add('has-snapshot');
    }
}

function hideSnapshotField() {
    var row = document.getElementById('historyActionsRow');
    if (row) {
        row.classList.remove('has-snapshot');
    }
}

function hrmsFirstRowValue(row) {
    if (row == null) {
        return null;
    }
    if (typeof row !== 'object') {
        return row;
    }
    var keys = Object.keys(row);
    for (var i = 0; i < keys.length; i++) {
        var value = row[keys[i]];
        if (value != null && value !== '') {
            return value;
        }
    }
    return null;
}

function hrmsRowField(row, names) {
    if (!row || !names) {
        return null;
    }
    for (var i = 0; i < names.length; i++) {
        var name = names[i];
        if (row[name] != null && row[name] !== '') {
            return row[name];
        }
        var lower = String(name).toLowerCase();
        for (var key in row) {
            if (Object.prototype.hasOwnProperty.call(row, key) && key.toLowerCase() === lower) {
                return row[key];
            }
        }
    }
    return null;
}

function setHistoryEmptyState(visible, message) {
    var el = document.getElementById('no-data-message');
    if (!el) return;
    el.classList.toggle('is-visible', visible);
    if (message) {
        var detail = el.querySelector('.hrms-empty-detail');
        if (detail) detail.textContent = message;
    }
}

function getTable() {
    fillTableNameSelect('#tableTemp', '#tableName', tabList, tabTempList);
    clearHistoryResults();
    resetSnapshotDropdown();
    hideSnapshotField();
}

function clearHistoryResults() {
    historyTableData = [];
    var table = $('#dataTable');
    if (table.parent().hasClass('hrms-dt-table-area')) {
        table.unwrap();
    }
    if ($.fn.DataTable.isDataTable(table)) {
        table.DataTable().clear().destroy();
    }
    table.find('thead tr').empty();
    table.find('tbody').empty();
    $('#table-div').css({ visibility: 'hidden', display: 'none' });
    $('#export-csv-btn').hide();
}

function showHistoryResultsTable() {
    $('#table-div').css({ visibility: 'visible', display: '' });
    $('#export-csv-btn').show();
}

function resetSnapshotDropdown() {
    populateSnapshotDropdown([]);
}

function search() {
    var baseTable = hrmsRequireTableName('#tableName');
    if (!baseTable) {
        setHistoryEmptyState(true, 'Select table type and table name first.');
        return;
    }
    if (!$('#fromDate').val() || !$('#toDate').val()) {
        setHistoryEmptyState(true, 'Choose a from and to date, then click Find snapshots.');
        return;
    }

    if (window.HrmsCustomSelect) {
        HrmsCustomSelect.closeAll();
    }
    clearHistoryResults();
    resetSnapshotDropdown();
    setHistoryEmptyState(false);
    $('#loader').modal('show');
    showSnapshotField();

    $.ajax({
        type: 'GET',
        url: '/api/user/historyTableId?tabName=' + encodeURIComponent(baseTable)
            + '&fromDate=' + encodeURIComponent($('#fromDate').val())
            + '&toDate=' + encodeURIComponent($('#toDate').val()),
        dataType: 'json',
        timeout: 600000
    }).done(function (data) {
        try {
            var rows = hrmsCoerceApiRows(data && data.result);
            if (rows.length) {
                var added = populateSnapshotDropdown(rows);
                if (added > 0) {
                    setHistoryEmptyState(false);
                } else {
                    setHistoryEmptyState(true, 'Snapshots were returned but could not be read. Contact your administrator.');
                }
            } else {
                clearHistoryResults();
                resetSnapshotDropdown();
                setHistoryEmptyState(true, 'No snapshots in this date range. Try wider dates.');
            }
        } catch (e) {
            console.error('Failed to process snapshot response', e);
            clearHistoryResults();
            resetSnapshotDropdown();
            setHistoryEmptyState(true, 'Unable to load snapshots. Please contact admin.');
        }
    }).fail(function (xhr) {
        clearHistoryResults();
        resetSnapshotDropdown();
        setHistoryEmptyState(true);
        hrmsShowApiError('no-data-message', xhr, hrmsAjaxErrorMessage(xhr));
    }).always(function () {
        hrmsForceHideLoader();
    });
}

function searchHistory() {
    var baseTable = hrmsRequireTableName('#tableName');
    var historyId = $('#historyId').val();
    if (!baseTable || !historyId) {
        setHistoryEmptyState(true, 'Select a snapshot from the list after Find snapshots.');
        return;
    }

    $('#loader').modal('show');
    $.ajax({
        type: 'GET',
        url: '/api/user/historyTableData?tabName=' + encodeURIComponent(baseTable)
            + '&historyId=' + encodeURIComponent(historyId),
        dataType: 'json',
        timeout: 600000
    }).done(function (data) {
        try {
            var rows = hrmsCoerceApiRows(data && data.result);
            if (rows.length) {
                historyTableData = rows;
                renderDataTable(rows, '#dataTable');
                showHistoryResultsTable();
                setHistoryEmptyState(false);
            } else {
                clearHistoryResults();
                setHistoryEmptyState(true, 'No records in this snapshot.');
            }
        } catch (e) {
            console.error('Failed to render history table', e);
            clearHistoryResults();
            setHistoryEmptyState(true, 'Data loaded but could not render the grid.');
        }
    }).fail(function (xhr) {
        clearHistoryResults();
        setHistoryEmptyState(true);
        hrmsShowApiError('no-data-message', xhr, hrmsAjaxErrorMessage(xhr));
    }).always(function () {
        hrmsForceHideLoader();
    });
}

function syncDateDisplay(input) {
    var wrap = input.closest('.hrms-date-wrap');
    if (!wrap) return;
    var display = wrap.querySelector('.hrms-date-display');
    if (!display) return;
    var placeholder = display.getAttribute('data-placeholder') || 'YYYY-MM-DD';
    if (input.value) {
        display.textContent = input.value;
        display.classList.remove('is-placeholder');
    } else {
        display.textContent = placeholder;
        display.classList.add('is-placeholder');
    }
}

document.addEventListener('DOMContentLoaded', function () {
    fillTableNameSelect('#tableTemp', '#tableName', tabList, tabTempList);

    document.querySelectorAll('.hrms-date-wrap').forEach(function (wrap) {
        var input = wrap.querySelector('.hrms-date-input');
        if (!input) return;
        syncDateDisplay(input);
        input.addEventListener('input', function () { syncDateDisplay(input); });
        input.addEventListener('change', function () { syncDateDisplay(input); });
        wrap.addEventListener('click', function () {
            if (typeof input.showPicker === 'function') {
                try {
                    input.showPicker();
                } catch (err) {
                    input.focus();
                }
            } else {
                input.focus();
            }
        });
    });
});

function tableToCsv() {
    if (!historyTableData.length) return;
    const keys = Object.keys(historyTableData[0]);
    var csv = keys.join(',') + '\n';
    historyTableData.forEach(function (row) {
        csv += keys.map(function (k) { return row[k]; }).join(',') + '\n';
    });
    var a = document.createElement('a');
    a.href = 'data:text/csv;charset=utf-8,' + encodeURI(csv);
    a.download = $('#tableName').val() + '_history.csv';
    a.click();
}
