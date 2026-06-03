(function initDataMovementPage() {
    function updateCount(section, checkboxClass, countId) {
        var total = document.querySelectorAll('#' + section + ' .' + checkboxClass).length;
        var checked = document.querySelectorAll('#' + section + ' .' + checkboxClass + ':checked').length;
        var el = document.getElementById(countId);
        if (el) {
            el.textContent = checked + ' of ' + total + ' selected';
        }
    }

    function bindSection(sectionId, checkboxClass, countId) {
        var section = document.getElementById(sectionId);
        if (!section) return;

        section.querySelectorAll('.' + checkboxClass).forEach(function (cb) {
            cb.addEventListener('change', function () {
                updateCount(sectionId, checkboxClass, countId);
                var msg = section.querySelector('[id$="-msg"]');
                if (msg) msg.textContent = '';
            });
        });

        updateCount(sectionId, checkboxClass, countId);
    }

    document.querySelectorAll('[data-select-all]').forEach(function (btn) {
        btn.addEventListener('click', function () {
            var target = btn.getAttribute('data-select-all');
            var section = target === 'master' ? 'master-section' : 'main-section';
            var cls = target === 'master' ? 'table-checkbox-master' : 'table-checkbox-main';
            document.querySelectorAll('#' + section + ' .' + cls).forEach(function (cb) { cb.checked = true; });
            updateCount(section, cls, target + '-count');
            var msg = document.getElementById(target + '-msg');
            if (msg) msg.textContent = '';
        });
    });

    document.querySelectorAll('[data-select-none]').forEach(function (btn) {
        btn.addEventListener('click', function () {
            var target = btn.getAttribute('data-select-none');
            var section = target === 'master' ? 'master-section' : 'main-section';
            var cls = target === 'master' ? 'table-checkbox-master' : 'table-checkbox-main';
            document.querySelectorAll('#' + section + ' .' + cls).forEach(function (cb) { cb.checked = false; });
            updateCount(section, cls, target + '-count');
        });
    });

    function bindTableSearch(sectionId, inputId) {
        var input = document.getElementById(inputId);
        if (!input) return;
        input.addEventListener('input', function () {
            var q = this.value.trim().toLowerCase();
            document.querySelectorAll('#' + sectionId + ' .hrms-move-row').forEach(function (item) {
                var label = item.querySelector('.hrms-move-row__label');
                var text = label ? label.textContent.toLowerCase() : '';
                item.hidden = q.length > 0 && text.indexOf(q) === -1;
            });
        });
    }

    bindSection('master-section', 'table-checkbox-master', 'master-count');
    bindSection('main-section', 'table-checkbox-main', 'main-count');
    bindTableSearch('master-section', 'master-search');
    bindTableSearch('main-section', 'main-search');
})();

function hrmsCleanupModalBackdrops() {
    if ($('.modal.show').length === 0) {
        $('body').removeClass('modal-open').css('padding-right', '');
        $('.modal-backdrop').remove();
    }
}

function hrmsHideLoaderThen(next) {
    var $loader = $('#loader');
    var done = false;
    function finish() {
        if (done) {
            return;
        }
        done = true;
        hrmsCleanupModalBackdrops();
        if (typeof next === 'function') {
            next();
        }
    }
    $loader.one('hidden.bs.modal', finish);
    $loader.modal('hide');
    setTimeout(function () {
        if ($loader.hasClass('show')) {
            $loader.removeClass('show').attr('aria-hidden', 'true').hide();
        }
        finish();
    }, 500);
}

function hrmsShowMovementResult(resp, err, errorTitle) {
    hrmsHideLoaderThen(function () {
        if (typeof window.hrmsSessionResumeIdle === 'function') {
            window.hrmsSessionResumeIdle();
        }
        if (err) {
            if (window.hrmsShowUploadErrorFromXhr) {
                hrmsShowUploadErrorFromXhr(err, errorTitle);
            } else {
                $('#msg').html(err.responseJSON ? err.responseJSON.errorMsg : 'Operation failed');
                $('#successModal').modal('show');
            }
            return;
        }
        $('#msg').html((resp && resp.msg) ? resp.msg : 'Completed.');
        $('#successModal').modal('show');
    });
}

function runDataMovement(apiUrl, checkedBoxes, errorTitle) {
    var $confirm = $('#exampleModalCenter');
    $confirm.modal('hide');
    $confirm.one('hidden.bs.modal', function () {
        hrmsCleanupModalBackdrops();
        if (typeof window.hrmsSessionSuspendIdle === 'function') {
            window.hrmsSessionSuspendIdle();
        }
        $('#loader').modal('show');
        $.ajax({
            type: 'POST',
            url: apiUrl,
            traditional: true,
            data: { stringList: checkedBoxes },
            dataType: 'json',
            timeout: 3600000,
            success: function (resp) {
                hrmsShowMovementResult(resp, null, errorTitle);
            },
            error: function (err) {
                hrmsShowMovementResult(null, err, errorTitle);
            }
        });
    });
}

function validateMasterMove() {
    var checked = $('#master-section input.table-checkbox-master:checked');
    if (checked.length === 0) {
        $('#master-msg').text('Please select at least one table');
        return;
    }
    $('#master-msg').text('');
    $('#message').html('Are you sure you want to move selected tables to master?');
    $('#exampleModalCenter').modal('show');
    $('#accept-change').off('click').on('click', function () {
        var checkedBoxes = [];
        $('#master-section input.table-checkbox-master:checked').each(function () {
            checkedBoxes.push(this.value);
        });
        runDataMovement('/api/user/masterDataMovement', checkedBoxes, 'Move to Master failed');
    });
}

function validateMainMove() {
    var checked = $('#main-section input.table-checkbox-main:checked');
    if (checked.length === 0) {
        $('#main-msg').text('Please select at least one table');
        return;
    }
    $('#main-msg').text('');
    $('#message').html('Are you sure you want to move selected tables to main?');
    $('#exampleModalCenter').modal('show');
    $('#accept-change').off('click').on('click', function () {
        var checkedBoxes = [];
        $('#main-section input.table-checkbox-main:checked').each(function () {
            checkedBoxes.push(this.value);
        });
        runDataMovement('/api/user/mainDataMovement', checkedBoxes, 'Move to Main failed');
    });
}
