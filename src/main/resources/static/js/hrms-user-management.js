document.getElementById("user-status-filter")?.addEventListener("change", function () {
    window.location.href = "/user-management?status=" + encodeURIComponent(this.value);
});

$(function () {
    var table = document.getElementById("usersTable");
    if (!table || !$.fn.DataTable) return;
    if (!table.querySelector("tbody tr")) return;

    $("#usersTable").DataTable({
        paging: true,
        responsive: false,
        pageLength: 10,
        lengthMenu: [
            [10, 25, 50, -1],
            [10, 25, 50, "All"]
        ],
        order: [[0, "asc"]],
        deferRender: true,
        autoWidth: false,
        dom: '<"hrms-dt-toolbar"f>rt<"hrms-dt-footer"lpi>',
        columnDefs: [{ orderable: false, targets: 3 }],
        initComplete: function () {
            var $table = $(this.api().table().node());
            if (!$table.parent().hasClass("hrms-dt-table-area")) {
                $table.wrap('<div class="hrms-dt-table-area custom-scrollbar"></div>');
            }
        },
        language: {
            search: "",
            searchPlaceholder: "Search users…",
            lengthMenu: "Show _MENU_",
            info: "Showing _START_–_END_ of _TOTAL_",
            infoEmpty: "No users",
            zeroRecords: "No matching users",
            paginate: {
                first: "«",
                previous: "‹",
                next: "›",
                last: "»"
            }
        }
    });
});
