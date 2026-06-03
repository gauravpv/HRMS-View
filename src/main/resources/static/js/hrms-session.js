/**
 * Client-side idle timeout aligned with server.servlet.session.timeout.
 * Long-running uploads/movements can call hrmsSessionSuspendIdle() so polling does not fight the UI timer.
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

    window.hrmsSessionSuspendIdle = function () {
        suspended += 1;
        clearIdleTimer();
    };

    window.hrmsSessionResumeIdle = function () {
        suspended = Math.max(0, suspended - 1);
        scheduleIdleTimer();
    };

    function onUserActivity() {
        scheduleIdleTimer();
    }

    ['mousedown', 'keydown', 'scroll', 'touchstart'].forEach(function (eventName) {
        document.addEventListener(eventName, onUserActivity, { passive: true });
    });

    scheduleIdleTimer();

    function looksLikeLoginHtml(text) {
        if (!text || text.indexOf('<!DOCTYPE') === -1) {
            return false;
        }
        var lower = text.toLowerCase();
        return lower.indexOf('login') !== -1 || lower.indexOf('sign in') !== -1;
    }

    window.hrmsRedirectSessionExpired = function () {
        window.location.replace(expiredUrl);
    };

    if (window.jQuery) {
        jQuery(document).ajaxComplete(function (_event, xhr, settings) {
            if (!xhr || (settings && settings.url && settings.url.indexOf('/login') !== -1)) {
                return;
            }
            if (xhr.status === 401 || looksLikeLoginHtml(xhr.responseText)) {
                window.hrmsRedirectSessionExpired();
            }
        });
    }
})();
