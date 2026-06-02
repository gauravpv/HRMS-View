/** Shared UI: sidebar, data-management accordion */
(function () {
    document.getElementById('sidebar-toggle')?.addEventListener('click', function () {
        document.getElementById('hrms-sidebar')?.classList.toggle('open');
    });

    var group = document.getElementById('data-mgmt-group');
    var toggle = document.getElementById('data-mgmt-toggle');
    if (!group || !toggle) return;

    function setExpanded(expanded) {
        group.classList.toggle('is-expanded', expanded);
        toggle.setAttribute('aria-expanded', String(expanded));
    }

    if (group.classList.contains('is-child-active')) {
        setExpanded(true);
    }

    toggle.addEventListener('click', function () {
        setExpanded(!group.classList.contains('is-expanded'));
    });
})();
