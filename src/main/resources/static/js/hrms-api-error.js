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
