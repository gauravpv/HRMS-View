function hrmsFormatUploadIssue(issue) {
    if (!issue) {
        return "";
    }
    var msg = issue.message || "";
    if (issue.row != null && issue.columnName) {
        var col = issue.column != null ? " (column " + issue.column + ")" : "";
        return "Row " + issue.row + ", " + issue.columnName + col + ": " + msg;
    }
    if (issue.row != null) {
        return "Row " + issue.row + ": " + msg;
    }
    return msg;
}

function hrmsExtractUploadIssuesFromXhr(xhr) {
    var body = xhr && xhr.responseJSON ? xhr.responseJSON : {};
    var issues = body.issues || [];
    if (body.row != null && !issues.length) {
        issues = [
            {
                row: body.row,
                column: body.column,
                columnName: body.columnName,
                value: body.value,
                message: body.errorMsg
            }
        ];
    }
    return issues;
}

function hrmsShowUploadResult(options) {
    var title = options.title || "Message";
    var message = options.message || "";
    var issues = options.issues || [];
    var isError = !!options.isError;

    var titleEl = document.getElementById("upload-result-title");
    var msgEl = document.getElementById("msg");
    var listEl = document.getElementById("upload-result-issues");
    var moreEl = document.getElementById("upload-result-more");
    var modal = document.getElementById("successModal");

    if (titleEl) {
        titleEl.textContent = title;
    }
    if (msgEl) {
        msgEl.textContent = message;
    }
    if (listEl) {
        listEl.innerHTML = "";
        if (issues.length) {
            issues.forEach(function (issue) {
                var li = document.createElement("li");
                li.textContent = hrmsFormatUploadIssue(issue);
                listEl.appendChild(li);
            });
            listEl.classList.remove("hidden");
        } else {
            listEl.classList.add("hidden");
        }
    }
    if (moreEl) {
        if (options.moreHint) {
            moreEl.textContent = options.moreHint;
            moreEl.classList.remove("hidden");
        } else {
            moreEl.textContent = "";
            moreEl.classList.add("hidden");
        }
    }
    if (modal) {
        var content = modal.querySelector(".modal-content");
        if (content) {
            content.classList.toggle("hrms-modal--error", isError);
            content.classList.toggle("hrms-modal--success", !isError);
        }
        $("#successModal").modal("show");
    }
}

function hrmsShowUploadErrorFromXhr(xhr, fallbackTitle) {
    var body = xhr && xhr.responseJSON ? xhr.responseJSON : {};
    hrmsShowUploadResult({
        title: fallbackTitle || "Upload failed",
        message: body.errorMsg || hrmsAjaxErrorMessage(xhr),
        issues: hrmsExtractUploadIssuesFromXhr(xhr),
        isError: true
    });
}

function hrmsShowUploadProgressResult(progress) {
    var status = progress.status;
    var isError = status === "ERROR";
    var hasRowErrors = status === "COMPLETED_WITH_ERRORS";
    var title = isError ? "Upload failed" : hasRowErrors ? "Upload completed with errors" : "Upload successful";
    var issues = progress.issues || [];
    var moreHint = "";
    if (hasRowErrors && progress.errorCount > issues.length) {
        moreHint =
            "Showing first " +
            issues.length +
            " of " +
            progress.errorCount +
            " failed rows. Fix these in your file and upload again.";
    }
    hrmsShowUploadResult({
        title: title,
        message: progress.message || "",
        issues: issues,
        moreHint: moreHint,
        isError: isError || hasRowErrors
    });
}
