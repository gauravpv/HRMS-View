var historyTableData = [];

function hrmsCleanupModalBackdrops() {
    if ($('.modal.show').length === 0) {
        $('body').removeClass('modal-open').css('padding-right', '');
        $('.modal-backdrop').remove();
    }
}

function hideHistoryLoader(next) {
    var $loader = $('#loader');
    var done = false;
    function finish() {
        if (done) {
            return;
        }
        done = true;
        hrmsCleanupModalBackdrops();
        if (typeof next === 'function') {
            next();
        }
    }
    $loader.one('hidden.bs.modal', finish);
    $loader.modal('hide');
    setTimeout(function () {
        if ($loader.hasClass('show')) {
            $loader.removeClass('show').attr('aria-hidden', 'true').hide();
        }
        finish();
    }, 500);
}

function hrmsCoerceApiRows(result) {
    if (!result) {
        return [];
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
    var dropdown = $('#historyId');
    dropdown.empty().append($('<option>', { value: '', text: 'Choose snapshot…', disabled: true, selected: true }));
    if (window.HrmsCustomSelect && dropdown[0]) {
        HrmsCustomSelect.refreshWrap(dropdown[0].closest('.hrms-select-wrap'));
    }
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
    $("#loader").modal('show');
    $("#historyIdRow").css("visibility", "visible");

    $.ajax({
        type: "GET",
        url: "/api/user/historyTableId?tabName=" + encodeURIComponent(baseTable)
            + "&fromDate=" + encodeURIComponent($("#fromDate").val())
            + "&toDate=" + encodeURIComponent($("#toDate").val()),
        dataType: "json",
        timeout: 600000
    }).done(function (data) {
        var rows = hrmsCoerceApiRows(data && data.result);
        if (rows.length) {
            var dropdown = $('#historyId');
            dropdown.empty().append($('<option>', { value: '', text: 'Choose snapshot…', disabled: true, selected: true }));
            var seen = {};
            var added = 0;
            rows.forEach(function (item) {
                var id = hrmsRowField(item, ['HISTORY_ID', 'history_id', 'HID', 'hid', 'ID', 'id']);
                if (id == null || id === '') {
                    id = hrmsFirstRowValue(item);
                }
                if (id == null || id === '') {
                    return;
                }
                var date = hrmsRowField(item, ['DATE', 'date', 'HISTORY_DATE', 'history_date', 'CREATED_DATE', 'created_date', 'SNAPSHOT_DATE', 'snapshot_date']);
                var key = String(id) + '|' + String(date != null ? date : '');
                if (seen[key]) {
                    return;
                }
                seen[key] = true;
                var label = date != null && date !== '' ? id + ' | ' + date : String(id);
                dropdown.append($('<option>', { value: String(id), text: label }));
                added += 1;
            });
            if (window.HrmsCustomSelect && dropdown[0]) {
                HrmsCustomSelect.refreshWrap(dropdown[0].closest('.hrms-select-wrap'));
            }
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
    }).fail(function (xhr) {
        clearHistoryResults();
        resetSnapshotDropdown();
        setHistoryEmptyState(true);
        hrmsShowApiError('no-data-message', xhr, hrmsAjaxErrorMessage(xhr));
    }).always(function () {
        hideHistoryLoader();
    });
}

function searchHistory() {
    var baseTable = hrmsRequireTableName('#tableName');
    var historyId = $('#historyId').val();
    if (!baseTable || !historyId) {
        setHistoryEmptyState(true, 'Select a snapshot from the list after Find snapshots.');
        return;
    }

    $("#loader").modal('show');
    $.ajax({
        type: "GET",
        url: "/api/user/historyTableData?tabName=" + encodeURIComponent(baseTable)
            + "&historyId=" + encodeURIComponent(historyId),
        dataType: "json",
        timeout: 600000
    }).done(function (data) {
        var rows = hrmsCoerceApiRows(data && data.result);
        if (rows.length) {
            try {
                historyTableData = rows;
                renderDataTable(rows, '#dataTable');
                showHistoryResultsTable();
                setHistoryEmptyState(false);
            } catch (e) {
                console.error('Failed to render history table', e);
                setHistoryEmptyState(true, 'Data loaded but could not render the grid.');
                clearHistoryResults();
            }
        } else {
            clearHistoryResults();
            setHistoryEmptyState(true, 'No records in this snapshot.');
        }
    }).fail(function (xhr) {
        clearHistoryResults();
        setHistoryEmptyState(true);
        hrmsShowApiError('no-data-message', xhr, hrmsAjaxErrorMessage(xhr));
    }).always(function () {
        hideHistoryLoader();
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
    var csv = keys.join(',') + "\n";
    historyTableData.forEach(function (row) {
        csv += keys.map(function (k) { return row[k]; }).join(',') + "\n";
    });
    var a = document.createElement('a');
    a.href = 'data:text/csv;charset=utf-8,' + encodeURI(csv);
    a.download = $("#tableName").val() + '_history.csv';
    a.click();
}
