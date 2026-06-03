function hrmsAjaxErrorMessage(xhr) {
    if (!xhr) {
        return 'Unable to load data. Please contact admin.';
    }
    if (xhr.status === 401 || xhr.status === 403) {
        if (typeof window.hrmsRedirectSessionExpired === 'function') {
            window.hrmsRedirectSessionExpired();
        }
        return 'Session expired. Please sign in again.';
    }
    if (xhr.responseJSON && xhr.responseJSON.errorMsg) {
        return xhr.responseJSON.errorMsg;
    }
    if (xhr.responseText && xhr.responseText.indexOf('<!DOCTYPE') !== -1) {
        if (typeof window.hrmsRedirectSessionExpired === 'function') {
            window.hrmsRedirectSessionExpired();
        }
        return 'Session expired or request was redirected. Please sign in again.';
    }
    return 'Unable to load data. Please contact admin.';
}

function hrmsShowApiError(containerId, xhr, fallbackMessage) {
    var msg = fallbackMessage || hrmsAjaxErrorMessage(xhr);
    var el = document.getElementById(containerId);
    if (!el) {
        return;
    }
    el.classList.add('is-visible');
    var detail = el.querySelector('.hrms-empty-detail');
    if (detail) {
        detail.textContent = msg;
    }
}
