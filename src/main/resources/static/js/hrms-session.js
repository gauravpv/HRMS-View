/**
 * Client-side idle timeout aligned with server.servlet.session.timeout.
 * Long-running uploads/movements can call hrmsBeginUploadSession() so idle redirect
 * and keepalive run for the whole upload (including pre-upload steps).
 */
(function initHrmsSession() {
    var meta = document.querySelector('meta[name="hrms-session-timeout-minutes"]');
    if (!meta) {
        return;
    }

    var minutes = parseInt(meta.getAttribute('content'), 10);
    if (!minutes || minutes < 1) {
        return;
    }

    var expiredPath = document.querySelector('meta[name="hrms-session-expired-url"]');
    var expiredUrl = expiredPath ? expiredPath.getAttribute('content') : '/session-expired';
    var idleMs = minutes * 60 * 1000;
    var timerId = null;
    var suspended = 0;
    var keepAliveTimerId = null;
    var uploadSessionHeld = false;
    var KEEP_ALIVE_MS = Math.min(5 * 60 * 1000, Math.max(idleMs - 60000, 60000));

    function clearIdleTimer() {
        if (timerId !== null) {
            clearTimeout(timerId);
            timerId = null;
        }
    }

    function scheduleIdleTimer() {
        clearIdleTimer();
        if (suspended > 0) {
            return;
        }
        timerId = setTimeout(function () {
            window.location.replace(expiredUrl);
        }, idleMs);
    }

    function pingSession() {
        if (!window.jQuery) {
            return;
        }
        jQuery.ajax({
            type: 'GET',
            url: '/api/user/sessionPing',
            timeout: 15000,
            hrmsSuppressSessionRedirect: true
        });
    }

    function startKeepAlive() {
        if (keepAliveTimerId !== null) {
            return;
        }
        pingSession();
        keepAliveTimerId = setInterval(pingSession, KEEP_ALIVE_MS);
    }

    function stopKeepAlive() {
        if (keepAliveTimerId !== null) {
            clearInterval(keepAliveTimerId);
            keepAliveTimerId = null;
        }
    }

    window.hrmsSessionSuspendIdle = function () {
        suspended += 1;
        clearIdleTimer();
        if (suspended === 1) {
            startKeepAlive();
        }
    };

    window.hrmsSessionResumeIdle = function () {
        suspended = Math.max(0, suspended - 1);
        if (suspended === 0) {
            stopKeepAlive();
        }
        scheduleIdleTimer();
    };

    window.hrmsBeginUploadSession = function () {
        if (uploadSessionHeld) {
            return;
        }
        uploadSessionHeld = true;
        window.hrmsSessionSuspendIdle();
    };

    window.hrmsEndUploadSession = function () {
        if (!uploadSessionHeld) {
            return;
        }
        uploadSessionHeld = false;
        window.hrmsSessionResumeIdle();
    };

    window.hrmsIsUploadInProgress = function () {
        return uploadSessionHeld;
    };

    function onUserActivity() {
        scheduleIdleTimer();
    }

    ['mousedown', 'keydown', 'scroll', 'touchstart'].forEach(function (eventName) {
        document.addEventListener(eventName, onUserActivity, { passive: true });
    });

    scheduleIdleTimer();

    function looksLikeLoginHtml(text) {
        if (typeof window.hrmsLooksLikeLoginPage === 'function') {
            return window.hrmsLooksLikeLoginPage(text);
        }
        if (!text || text.indexOf('<!DOCTYPE') === -1) {
            return false;
        }
        var lower = text.toLowerCase();
        return lower.indexOf('j_username') !== -1 || lower.indexOf('name="username"') !== -1;
    }

    window.hrmsRedirectSessionExpired = function () {
        window.location.replace(expiredUrl);
    };

    if (window.jQuery) {
        jQuery(document).ajaxComplete(function (_event, xhr, settings) {
            if (!xhr || (settings && settings.hrmsSuppressSessionRedirect)) {
                return;
            }
            if (settings && settings.url) {
                if (settings.url.indexOf('/login') !== -1 || settings.url.indexOf('/sessionPing') !== -1) {
                    return;
                }
            }
            if (window.hrmsIsUploadInProgress() && xhr.status !== 401) {
                return;
            }
            if (xhr.status === 401 || looksLikeLoginHtml(xhr.responseText)) {
                window.hrmsRedirectSessionExpired();
            }
        });
    }
})();
