var searchTableData = [];
var searchTableName = "";

function setSearchSummary(message) {
    var el = document.getElementById("search-summary");
    if (!el) return;
    if (message) {
        el.textContent = message;
        el.hidden = false;
    } else {
        el.textContent = "";
        el.hidden = true;
    }
}

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
        setSearchSummary("");
        return;
    }
    if (!$("#tableTemp").val()) {
        setSearchEmptyState(true, "Select table type (Temp or Master) first.");
        setSearchSummary("");
        return;
    }

    if (window.HrmsCustomSelect) {
        HrmsCustomSelect.closeAll();
    }

    searchTableName = tableName;
    searchTableData = [];
    setSearchSummary("");
    $("#table-div").css("visibility", "hidden");
    $("#export-csv-btn").hide();
    setSearchEmptyState(false);
    $("#loader").modal("show");

    $.ajax({
        type: "GET",
        url: "/api/user/searchTableData?tabName=" + encodeURIComponent(tableName) + "&lite=true",
        dataType: "json",
        cache: false,
        timeout: 600000
    })
        .done(function (data) {
            if (data && data.result && data.result.length) {
                try {
                    searchTableData = data.result;
                    setSearchSummary(data.msg && data.msg.indexOf("Showing") === 0 ? data.msg : "");
                    renderDataTable(data.result, "#dataTable", {
                        onReady: function () {
                            hrmsForceHideLoader();
                            $("#table-div").css("visibility", "visible");
                            $("#export-csv-btn").show();
                            setSearchEmptyState(false);
                        }
                    });
                } catch (e) {
                    console.error("Failed to render table", e);
                    hrmsForceHideLoader();
                    setSearchSummary("");
                    setSearchEmptyState(
                        true,
                        "Data loaded but could not render the grid. Try export or a smaller table."
                    );
                    $("#table-div").css("visibility", "hidden");
                    $("#export-csv-btn").hide();
                }
            } else {
                hrmsForceHideLoader();
                setSearchSummary("");
                setSearchEmptyState(true, "No records found for this table.");
                $("#table-div").css("visibility", "hidden");
                $("#export-csv-btn").hide();
            }
        })
        .fail(function (xhr) {
            hrmsForceHideLoader();
            setSearchSummary("");
            setSearchEmptyState(true);
            hrmsShowApiError("no-data-message", xhr, hrmsAjaxErrorMessage(xhr));
            $("#table-div").css("visibility", "hidden");
            $("#export-csv-btn").hide();
        });
}

function tableToCsv() {
    var tableName = searchTableName || $("#tableName").val();
    if (!tableName) return;

    $("#loader").modal("show");
    $.ajax({
        type: "GET",
        url: "/api/user/searchTableData?tabName=" + encodeURIComponent(tableName) + "&lite=false",
        dataType: "json",
        cache: false,
        timeout: 600000
    })
        .done(function (data) {
            hrmsForceHideLoader();
            if (!data || !data.result || !data.result.length) {
                return;
            }
            var rows = data.result;
            var keys = Object.keys(rows[0]);
            var csv = keys.join(",") + "\n";
            rows.forEach(function (row) {
                csv +=
                    keys
                        .map(function (k) {
                            var value = row[k];
                            if (value == null) {
                                return "";
                            }
                            var text = String(value);
                            if (/[",\n]/.test(text)) {
                                return '"' + text.replace(/"/g, '""') + '"';
                            }
                            return text;
                        })
                        .join(",") + "\n";
            });
            var a = document.createElement("a");
            a.href = "data:text/csv;charset=utf-8," + encodeURIComponent(csv);
            a.download = tableName + ".csv";
            a.click();
        })
        .fail(function (xhr) {
            hrmsForceHideLoader();
            hrmsShowApiError("no-data-message", xhr, "Export failed. Please try again.");
        });
}
