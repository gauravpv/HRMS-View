/** Display label for a DB table name (strip _master/_temp, uppercase). Values sent to API stay unchanged. */
function hrmsFormatTableDisplayName(tableName) {
    if (!tableName) {
        return '';
    }
    return String(tableName)
        .replace(/_master$/i, '')
        .replace(/_temp$/i, '')
        .toUpperCase();
}

function hrmsFormatCell(value) {
    if (value == null || value === '') {
        return '';
    }
    if (typeof value === 'string' && /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}/.test(value)) {
        var d = new Date(value);
        if (!isNaN(d.getTime())) {
            return d.toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' });
        }
    }
    if (typeof value === 'number' && !Number.isInteger(value)) {
        return Number(value.toFixed(2));
    }
    return value;
}

function renderDataTable(data, tableSelector, options) {
    options = options || {};
    if (!data || !data.length) {
        if (typeof options.onReady === "function") {
            options.onReady();
        }
        return;
    }
    var table = $(tableSelector);
    if (table.parent().hasClass("hrms-dt-table-area")) {
        table.unwrap();
    }
    if ($.fn.DataTable.isDataTable(table)) {
        table.DataTable().clear().destroy();
    }
    table.find("thead tr").empty();
    table.find("tbody").empty();

    var keys = Object.keys(data[0]);
    keys.forEach(function (k) {
        table.find("thead tr").append($("<th>").text(k));
    });

    var columns = keys.map(function (k) {
        return {
            title: k,
            data: k,
            defaultContent: "",
            render: function (value) {
                return hrmsFormatCell(value);
            }
        };
    });

    table.addClass("hrms-data-table display compact stripe");

    var columnDefs = [];
    keys.forEach(function (k, idx) {
        if (k === "ACTION" || k === "UPDATED_BY" || k === "CITY_NAME") {
            columnDefs.push({ targets: idx, width: "9rem" });
        }
    });

    table.DataTable({
        data: data,
        columns: columns,
        columnDefs: columnDefs,
        responsive: false,
        paging: true,
        pageLength: 25,
        lengthMenu: [[10, 25, 50, 100], [10, 25, 50, 100]],
        deferRender: true,
        processing: true,
        autoWidth: false,
        order: [],
        dom: '<"hrms-dt-toolbar"f>rt<"hrms-dt-footer"lpi>',
        initComplete: function () {
            var $table = $(this.api().table().node());
            if (!$table.parent().hasClass("hrms-dt-table-area")) {
                $table.wrap('<div class="hrms-dt-table-area custom-scrollbar"></div>');
            }
            if (typeof options.onReady === "function") {
                options.onReady();
            }
        },
        language: {
            search: "",
            searchPlaceholder: "Filter rows…",
            lengthMenu: "Show _MENU_",
            info: "Showing _START_–_END_ of _TOTAL_",
            infoEmpty: "No matching rows",
            infoFiltered: "(filtered from _MAX_ total)",
            zeroRecords: "No matching rows",
            processing: "Loading rows…",
            paginate: {
                first: "«",
                previous: "‹",
                next: "›",
                last: "»"
            }
        }
    });
}

function fillTableNameSelect(tempSelectId, nameSelectId, tabList, tabTempList) {
    var tableTemp = $(tempSelectId).val();
    var nameSelect = $(nameSelectId);
    nameSelect.empty().append('<option value="">Choose table…</option>');
    if (tableTemp === 'temp') {
        $.each(tabTempList, function (i, value) {
            nameSelect.append($('<option>', { value: value, text: hrmsFormatTableDisplayName(value) }));
        });
    } else if (tableTemp === 'master') {
        $.each(tabList, function (i, value) {
            nameSelect.append($('<option>', { value: value, text: hrmsFormatTableDisplayName(value) }));
        });
    }
    nameSelect.val('');
    if (window.HrmsCustomSelect && nameSelect[0]) {
        HrmsCustomSelect.refreshWrap(nameSelect[0].closest('.hrms-select-wrap'));
    }
}

function hrmsRequireTableName(selectId) {
    var value = $(selectId).val();
    if (!value) {
        return null;
    }
    return value;
}
