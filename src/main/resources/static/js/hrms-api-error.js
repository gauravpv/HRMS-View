function hrmsLooksLikeLoginPage(text) {
    if (!text || text.indexOf('<!DOCTYPE') === -1) {
        return false;
    }
    var lower = text.toLowerCase();
    return (
        lower.indexOf('j_username') !== -1 ||
        lower.indexOf('name="username"') !== -1 ||
        (lower.indexOf('/login') !== -1 &&
            (lower.indexOf('password') !== -1 || lower.indexOf('sign in') !== -1))
    );
}

function hrmsShouldRedirectSessionExpired(xhr) {
    if (!xhr) {
        return false;
    }
    if (xhr.status === 401) {
        return true;
    }
    if (xhr.status === 403 || xhr.status === 0) {
        return false;
    }
    if (typeof window.hrmsIsUploadInProgress === 'function' && window.hrmsIsUploadInProgress()) {
        return false;
    }
    return hrmsLooksLikeLoginPage(xhr.responseText || '');
}

function hrmsAjaxErrorMessage(xhr) {
    if (!xhr) {
        return 'Unable to load data. Please contact admin.';
    }
    if (xhr.status === 401) {
        if (typeof window.hrmsRedirectSessionExpired === 'function') {
            window.hrmsRedirectSessionExpired();
        }
        return 'Session expired. Please sign in again.';
    }
    if (xhr.status === 403) {
        return 'Access denied or request blocked. Please contact admin.';
    }
    if (xhr.responseJSON && xhr.responseJSON.errorMsg) {
        return xhr.responseJSON.errorMsg;
    }
    if (hrmsShouldRedirectSessionExpired(xhr)) {
        if (typeof window.hrmsRedirectSessionExpired === 'function') {
            window.hrmsRedirectSessionExpired();
        }
        return 'Session expired or request was redirected. Please sign in again.';
    }
    if (xhr.responseText && xhr.responseText.indexOf('<!DOCTYPE') !== -1) {
        return 'The server returned an unexpected response. Please try again or contact admin.';
    }
    return 'Unable to load data. Please contact admin.';
}

window.hrmsLooksLikeLoginPage = hrmsLooksLikeLoginPage;

function hrmsCleanupModalBackdrops() {
    var $loader = $('#loader');
    if ($loader.length) {
        $loader.removeClass('show').attr('aria-hidden', 'true').css('display', 'none');
    }
    $('body').removeClass('modal-open').css('padding-right', '');
    $('.modal-backdrop').remove();
}

function hrmsForceHideLoader(callback) {
    var $loader = $('#loader');
    var done = false;
    function finish() {
        if (done) {
            return;
        }
        done = true;
        hrmsCleanupModalBackdrops();
        if (typeof callback === 'function') {
            callback();
        }
    }
    if (!$loader.length) {
        finish();
        return;
    }
    $loader.one('hidden.bs.modal', finish);
    $loader.modal('hide');
    setTimeout(finish, 500);
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

function hrmsHandleApiFailure(containerId, xhr) {
    try {
        hrmsShowApiError(containerId, xhr, hrmsAjaxErrorMessage(xhr));
    } catch (e) {
        console.error('Failed to display API error', e);
    } finally {
        hrmsForceHideLoader();
    }
}
