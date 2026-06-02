var historyTableData = [];

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
    var tableName = baseTable + "_history";
    $.ajax({
        type: "GET",
        url: "/api/user/historyTableId?tabName=" + encodeURIComponent(tableName)
            + "&fromDate=" + encodeURIComponent($("#fromDate").val())
            + "&toDate=" + encodeURIComponent($("#toDate").val()),
        dataType: "json",
        timeout: 600000,
        success: function (data) {
            $("#loader").modal('hide');
            if (data && data.result && data.result.length) {
                var dropdown = $('#historyId');
                dropdown.empty().append($('<option>', { value: '', text: 'Choose snapshot…', disabled: true, selected: true }));
                var seen = {};
                data.result.forEach(function (item) {
                    var id = item.HISTORY_ID != null ? item.HISTORY_ID : item.history_id;
                    var date = item.DATE != null ? item.DATE : item.date;
                    var key = id + '|' + date;
                    if (seen[key]) return;
                    seen[key] = true;
                    dropdown.append($('<option>', { value: id, text: id + ' | ' + date }));
                });
                if (window.HrmsCustomSelect && dropdown[0]) {
                    HrmsCustomSelect.refreshWrap(dropdown[0].closest('.hrms-select-wrap'));
                }
                setHistoryEmptyState(false);
            } else {
                clearHistoryResults();
                resetSnapshotDropdown();
                setHistoryEmptyState(true, 'No snapshots in this date range. Try wider dates (e.g. 2024-05-01 to 2024-12-31).');
            }
        },
        error: function (xhr) {
            $("#loader").modal('hide');
            clearHistoryResults();
            resetSnapshotDropdown();
            setHistoryEmptyState(true);
            hrmsShowApiError('no-data-message', xhr, hrmsAjaxErrorMessage(xhr));
        }
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
        url: "/api/user/historyTableData?tabName=" + encodeURIComponent(baseTable + "_history")
            + "&historyId=" + encodeURIComponent(historyId),
        dataType: "json",
        timeout: 600000,
        success: function (data) {
            $("#loader").modal('hide');
            if (data && data.result && data.result.length) {
                try {
                    historyTableData = data.result;
                    renderDataTable(data.result, '#dataTable');
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
        },
        error: function (xhr) {
            $("#loader").modal('hide');
            clearHistoryResults();
            setHistoryEmptyState(true);
            hrmsShowApiError('no-data-message', xhr, hrmsAjaxErrorMessage(xhr));
        }
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

    var fromDate = document.getElementById('fromDate');
    var toDate = document.getElementById('toDate');
    if (fromDate) fromDate.addEventListener('change', search);
    if (toDate) toDate.addEventListener('change', search);
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
