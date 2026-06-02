var columnNames = "";
var UPLOAD_POLL_MS = 800;

function uploadTableSelect() {
    return $("#uploadTableName").length ? $("#uploadTableName") : $("#tableName");
}

function downloadFormat() {
    const tableName = uploadTableSelect().val();
    if (tableName != "Select") {
        $("#format").removeClass("hidden");
        $.ajax({
            type: "GET",
            contentType: "application/json",
            url: "/api/user/getColumns?tabName=" + tableName,
            dataType: "json",
            cache: false,
            timeout: 600000,
            success: function (data) {
                columnNames = data.result[0];
            },
            error: function (xhr) {
                hrmsShowUploadErrorFromXhr(xhr, "Could not load template");
            }
        });
    }
}

function formatCSV() {
    const tableName = uploadTableSelect().val();
    var listColumns = columnNames.split(",");
    var obj = {};
    var csv = "";
    var data = [];
    for (var i = 0; i < listColumns.length; i++) {
        listColumns[i] = listColumns[i].trim();
        csv += listColumns[i] + (i === listColumns.length - 1 ? "\n" : ",");
    }
    listColumns.forEach((item) => {
        if (item != "PREVIOUS_STATE" && item != "NEW_STATE" && item != "ACTION" && item != "UPDATED_BY") {
            const key_ = item.toLowerCase().replace(/_([a-z])/g, function (match, group1) {
                return group1.toUpperCase();
            });
            obj[key_] = "";
        }
    });
    listColumns.forEach((item) => {
        if (item == "PREVIOUS_STATE" || item == "NEW_STATE") {
            const txtObj = JSON.stringify(obj).replaceAll(",", "|");
            data.push("'" + txtObj + "'");
        } else if (item == "ACTION") {
            data.push("'BULK_APPROVED'");
        } else if (item == "ID") {
            data.push("default");
        } else if (item == "CREATED_DATE" || item == "LAST_UPDATED_DATE") {
            data.push("current_timestamp()");
        } else if (item == "UPDATED_BY") {
            data.push("'" + $("#username").text() + "'");
        } else if (item == "STATUS") {
            data.push("0");
        } else {
            data.push("''");
        }
    });
    csv += data.join(",") + "\n";
    var hiddenElement = document.createElement("a");
    hiddenElement.href = "data:text/csv;charset=utf-8," + encodeURI(csv);
    hiddenElement.download = tableName + "_format.csv";
    hiddenElement.click();
}

function ajaxGet(url) {
    return new Promise((resolve, reject) => {
        $.ajax({
            type: "GET",
            url: url,
            timeout: 600000,
            success: (data) => resolve(data),
            error: (xhr) => reject(xhr)
        });
    });
}

function setUploadStep(stepId, state) {
    const el = document.getElementById(stepId);
    if (!el) return;
    el.classList.remove("is-pending", "is-active", "is-done", "is-error");
    el.classList.add("is-" + state);
    const icon = el.querySelector(".hrms-upload-step-icon");
    if (!icon) return;
    if (state === "done") {
        icon.textContent = "check_circle";
    } else if (state === "active") {
        icon.textContent = "progress_activity";
    } else if (state === "error") {
        icon.textContent = "error";
    } else {
        icon.textContent = "radio_button_unchecked";
    }
}

function resetUploadBar() {
    $("#progress-total").text("0");
    $("#progress-processed").text("0");
    $("#progress-percentage").text("0");
    $("#progress-bar").css("width", "0%").attr("aria-valuenow", 0);
}

function openUploadProgressModal(withPreSteps) {
    const stepsEl = document.getElementById("upload-phase-steps");
    const progressEl = document.getElementById("upload-phase-progress");
    resetUploadBar();
    if (withPreSteps) {
        stepsEl.classList.remove("hidden");
        progressEl.classList.add("hidden");
        setUploadStep("step-truncate", "pending");
        setUploadStep("step-history", "pending");
    } else {
        stepsEl.classList.add("hidden");
        progressEl.classList.remove("hidden");
    }
    $("#upload-progress-modal").modal("show");
}

function revealUploadBar() {
    document.getElementById("upload-phase-progress").classList.remove("hidden");
}

async function runTruncateAndHistorySteps() {
    const tableName = encodeURIComponent(uploadTableSelect().val());
    openUploadProgressModal(true);
    setUploadStep("step-truncate", "active");
    await ajaxGet("/api/user/truncateTable?tableName=" + tableName);
    setUploadStep("step-truncate", "done");

    setUploadStep("step-history", "active");
    await ajaxGet("/api/user/moveToHistory?tableName=" + tableName);
    setUploadStep("step-history", "done");

    revealUploadBar();
}

function clearTableError() {
    $("#table-name-msg").text("");
}

function showTableError(message) {
    $("#table-name-msg").text(message);
}

function clearFileUploadError() {
    var msgEl = document.getElementById("file-upload-msg");
    var dropzone = document.getElementById("dropzone");
    var fileLabel = document.getElementById("file-label");
    if (msgEl) {
        msgEl.textContent = "";
        msgEl.hidden = true;
        msgEl.classList.add("hidden");
        msgEl.style.display = "";
    }
    if (dropzone) {
        dropzone.classList.remove("is-error");
    }
    if (fileLabel) {
        fileLabel.classList.remove("is-error");
        if (!getSelectedUploadFile()) {
            fileLabel.textContent = "No file selected";
        }
    }
}

function showFileUploadError(message) {
    var msgEl = document.getElementById("file-upload-msg");
    var dropzone = document.getElementById("dropzone");
    var fileLabel = document.getElementById("file-label");
    if (msgEl) {
        msgEl.textContent = message;
        msgEl.hidden = false;
        msgEl.classList.remove("hidden");
        msgEl.style.display = "block";
    }
    if (dropzone) {
        dropzone.classList.add("is-error");
        dropzone.scrollIntoView({ behavior: "smooth", block: "nearest" });
    }
    if (fileLabel) {
        fileLabel.textContent = message;
        fileLabel.classList.add("is-error");
    }
}

function getSelectedUploadFile() {
    const input = document.getElementById("file");
    return input && input.files && input.files.length ? input.files[0] : null;
}

function sendFile() {
    const tableName = uploadTableSelect().val();
    const file = getSelectedUploadFile();
    const check = $("#customCheck").prop("checked");

    clearTableError();
    clearFileUploadError();

    if (tableName === "Select") {
        showTableError("Please select a target table.");
        return;
    }

    if (!file) {
        showFileUploadError("Please select a CSV file before starting upload.");
        return;
    }

    const startUploadFlow = () => {
        if (!check) {
            openUploadProgressModal(false);
        }
        startAsyncUpload(file, tableName);
    };

    if (check) {
        checkTruncateTable(startUploadFlow, () => {});
    } else {
        startUploadFlow();
    }
}

function startAsyncUpload(file, tableName) {
    var formData = new FormData();
    formData.append("file", file);
    $.ajax({
        type: "POST",
        contentType: false,
        processData: false,
        url: "/api/user/addFileAsync?tableName=" + encodeURIComponent(tableName),
        data: formData,
        timeout: 1800000,
        success: function (data) {
            if (data.result && data.result[0] && data.result[0].progressKey) {
                startProgressPolling(data.result[0].progressKey, data.result[0].totalRows);
            } else {
                $("#upload-progress-modal").modal("hide");
                hrmsShowUploadResult({
                    title: "Upload started",
                    message: data.msg || "Upload started.",
                    isError: false
                });
            }
        },
        error: function (e) {
            $("#upload-progress-modal").modal("hide");
            hrmsShowUploadErrorFromXhr(e, "Upload could not start");
            showFileUploadError(
                (e.responseJSON && e.responseJSON.errorMsg) ||
                    "Upload could not start. Check the file and table, then try again."
            );
        }
    });
}

function startProgressPolling(progressKey, totalRows) {
    $("#progress-total").text(totalRows);
    $("#progress-processed").text("0");
    $("#progress-percentage").text("0");
    $("#progress-bar").css("width", "0%").attr("aria-valuenow", 0);

    var progressInterval = setInterval(function () {
        $.ajax({
            type: "GET",
            url: "/api/user/uploadProgress?progressKey=" + encodeURIComponent(progressKey),
            timeout: 10000,
            success: function (data) {
                if (data.result && data.result[0]) {
                    var progress = data.result[0];
                    var percentage = progress.percentage || 0;
                    $("#progress-processed").text(progress.processedRows);
                    $("#progress-percentage").text(percentage);
                    $("#progress-bar").css("width", percentage + "%").attr("aria-valuenow", percentage);
                    if (
                        progress.status === "COMPLETED" ||
                        progress.status === "ERROR" ||
                        progress.status === "COMPLETED_WITH_ERRORS"
                    ) {
                        clearInterval(progressInterval);
                        setTimeout(function () {
                            $("#upload-progress-modal").modal("hide");
                            hrmsShowUploadProgressResult(progress);
                            if (progress.status === "COMPLETED") {
                                $("#file").val("");
                                document.getElementById("file-label").textContent = "No file selected";
                                clearFileUploadError();
                            }
                        }, 800);
                    }
                }
            },
            error: function (xhr) {
                clearInterval(progressInterval);
                $("#upload-progress-modal").modal("hide");
                hrmsShowUploadResult({
                    title: "Progress unavailable",
                    message:
                        "Could not check upload progress. The upload may still be running — refresh the page or try again.",
                    issues: [],
                    isError: true
                });
            }
        });
    }, UPLOAD_POLL_MS);
}

function checkTruncateTable(resolve, reject) {
    $("#message").html("All previous data will be lost. Are you sure?");
    $("#exampleModalCenter").modal("show");
    $("#accept-change")
        .off("click")
        .on("click", async () => {
            $("#exampleModalCenter").modal("hide");
            try {
                await runTruncateAndHistorySteps();
                resolve();
            } catch (err) {
                setUploadStep("step-truncate", "error");
                setUploadStep("step-history", "error");
                $("#upload-progress-modal").modal("hide");
                hrmsShowUploadErrorFromXhr(err, "Pre-upload step failed");
                reject(new Error("Pre-upload failed"));
            }
        });
    $("#reject-change")
        .off("click")
        .on("click", () => {
            reject(new Error("Cancelled"));
            $("#exampleModalCenter").modal("hide");
        });
}

document.addEventListener("DOMContentLoaded", function () {
    var uploadBtn = document.querySelector(".hrms-upload-submit");
    if (uploadBtn) {
        uploadBtn.addEventListener("click", function (e) {
            e.preventDefault();
            sendFile();
        });
    }

    var dropzone = document.getElementById("dropzone");
    var fileInput = document.getElementById("file");
    if (dropzone && fileInput) {
        dropzone.addEventListener("click", () => fileInput.click());
        dropzone.addEventListener("dragover", (e) => {
            e.preventDefault();
            dropzone.classList.add("is-dragover");
        });
        dropzone.addEventListener("dragleave", () => {
            dropzone.classList.remove("is-dragover");
        });
        dropzone.addEventListener("drop", (e) => {
            e.preventDefault();
            dropzone.classList.remove("is-dragover");
            if (e.dataTransfer.files.length) {
                fileInput.files = e.dataTransfer.files;
                document.getElementById("file-label").textContent = e.dataTransfer.files[0].name;
                clearFileUploadError();
            }
        });
        fileInput.addEventListener("change", () => {
            if (fileInput.files[0]) {
                document.getElementById("file-label").textContent = fileInput.files[0].name;
                clearFileUploadError();
            }
        });
    }
});
