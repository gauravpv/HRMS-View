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
        columnDefs: [
            { orderable: false, targets: [1, 3] },
            { width: "36%", targets: 0 },
            { width: "14%", targets: 1 },
            { width: "22%", targets: 2 },
            { width: "28%", targets: 3 }
        ],
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

document.addEventListener("click", function (event) {
    var button = event.target.closest(".hrms-role-switch__btn");
    if (!button || button.classList.contains("is-active")) {
        return;
    }
    var form = button.closest(".hrms-user-role-form");
    if (!form) {
        return;
    }
    var role = button.getAttribute("data-role");
    var userName = form.getAttribute("data-user-name");
    var label = userName || "this user";
    if (!confirm("Change role for " + label + " to " + role + "?")) {
        return;
    }
    var hidden = form.querySelector(".hrms-user-role-value");
    if (hidden) {
        hidden.value = role;
    }
    form.submit();
});
