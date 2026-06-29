var searchTableData = [];

function setSearchEmptyState(visible, message) {
    var el = document.getElementById("no-data-message");
    if (!el) return;
    el.classList.toggle("is-visible", visible);
    if (message) {
        var detail = el.querySelector(".hrms-empty-detail");
        if (detail) detail.textContent = message;
    }
}

function getTable() {
    fillTableNameSelect("#tableTemp", "#tableName", tabList, tabTempList);
}

function search() {
    var tableName = hrmsRequireTableName("#tableName");
    if (!tableName) {
        setSearchEmptyState(true, "Select table type and table name, then run the query.");
        return;
    }
    if (!$("#tableTemp").val()) {
        setSearchEmptyState(true, "Select table type (Temp or Master) first.");
        return;
    }

    if (window.HrmsCustomSelect) {
        HrmsCustomSelect.closeAll();
    }
    $("#loader").modal("show");
    $.ajax({
        type: "GET",
        url: "/api/user/searchTableData?tabName=" + encodeURIComponent(tableName),
        dataType: "json",
        cache: false,
        timeout: 600000,
        success: function (data) {
            $("#loader").modal("hide");
            if (data && data.result && data.result.length) {
                try {
                    searchTableData = data.result;
                    renderDataTable(data.result, "#dataTable");
                    $("#table-div").css("visibility", "visible");
                    $("#export-csv-btn").show();
                    setSearchEmptyState(false);
                } catch (e) {
                    console.error("Failed to render table", e);
                    setSearchEmptyState(true, "Data loaded but could not render the grid. Try a smaller table or export.");
                    $("#table-div").css("visibility", "hidden");
                    $("#export-csv-btn").hide();
                }
            } else {
                setSearchEmptyState(true, "No records found for this table.");
                $("#table-div").css("visibility", "hidden");
                $("#export-csv-btn").hide();
            }
        },
        error: function (xhr) {
            $("#loader").modal("hide");
            setSearchEmptyState(true);
            hrmsShowApiError("no-data-message", xhr, hrmsAjaxErrorMessage(xhr));
            $("#table-div").css("visibility", "hidden");
            $("#export-csv-btn").hide();
        }
    });
}

function tableToCsv() {
    if (!searchTableData.length) return;
    const keys = Object.keys(searchTableData[0]);
    var csv = keys.join(",") + "\n";
    searchTableData.forEach(function (row) {
        csv += keys.map(function (k) {
            return row[k];
        }).join(",") + "\n";
    });
    var a = document.createElement("a");
    a.href = "data:text/csv;charset=utf-8," + encodeURI(csv);
    a.download = $("#tableName").val() + ".csv";
    a.click();
}
