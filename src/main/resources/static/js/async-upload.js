// Async bulk upload functionality with progress tracking

function sendFileWithProgress() {
    const tableName = $("#tableName").val();
    const check = $('#customCheck').prop('checked');

    const checkTruncateTablePromise = new Promise((resolve, reject) => {
        if (check) {
            console.log("Checking for Truncate Box Checked");
            checkTruncateTable(resolve, reject);
        } else {
            resolve();
            $("#loader").modal('show');
        }
    });

    function handleCheckResult() {
        if (tableName == "Select") {
            $("#table-name-msg").html("Table name is required");
            return Promise.reject(new Error("Table name is required"));
        } else {
            $("#table-name-msg").html("");
            return Promise.resolve();
        }
    }

    checkTruncateTablePromise.then(handleCheckResult)
        .then(() => {
            const fileInput = $("#file");
            const file = fileInput.prop('files')[0];

            if (file) {
                var formData = new FormData();
                formData.append('file', file);

                setTimeout(function () {
                    // Use the new async endpoint
                    var url = '/api/user/addFileAsync?tableName=' + tableName;
                    $("#loader").modal('show');
                    
                    $.ajax({
                        type: "POST",
                        contentType: false,
                        processData: false,
                        url: url,
                        data: formData,
                        timeout: 60000, // Reduced timeout since we're starting async
                        success: function (data) {
                            console.log("Upload started successfully");
                            $("#loader").modal('hide');
                            
                            // Start polling for progress
                            if(data.result && data.result[0] && data.result[0].progressKey) {
                                showProgressModal(data.result[0].progressKey, data.result[0].totalRows);
                            } else {
                                $("#msg").html(data.msg);
                                $("#successModal").modal("show");
                            }
                        },
                        error: function (e) {
                            console.log("Error starting upload");
                            $("#msg").html(e.responseJSON ? e.responseJSON.errorMsg : "Upload failed");
                            $("#loader").modal('hide');
                            $("#successModal").modal("show");
                        },
                    });
                }, 1500);
            } else {
                console.log("No file selected.");
                $("#msg").html("Please select a file to upload");
                $("#successModal").modal("show");
            }
        })
        .catch(error => {
            console.error(error);
        });
}

function showProgressModal(progressKey, totalRows) {
    // Show progress modal
    $("#upload-progress-modal").modal('show');
    $("#progress-total").text(totalRows);
    $("#progress-processed").text("0");
    $("#progress-percentage").text("0");
    $("#progress-bar").css("width", "0%").attr("aria-valuenow", 0);
    
    // Start polling for progress
    var progressInterval = setInterval(function() {
        $.ajax({
            type: "GET",
            url: "/api/user/uploadProgress?progressKey=" + progressKey,
            timeout: 10000,
            success: function(data) {
                if(data.result && data.result[0]) {
                    var progress = data.result[0];
                    var percentage = progress.percentage || 0;
                    
                    $("#progress-processed").text(progress.processedRows);
                    $("#progress-percentage").text(percentage);
                    $("#progress-bar").css("width", percentage + "%").attr("aria-valuenow", percentage);
                    
                    // Check if upload is complete
                    if(progress.status === "COMPLETED" || 
                       progress.status === "ERROR" || 
                       progress.status === "COMPLETED_WITH_ERRORS") {
                        clearInterval(progressInterval);
                        
                        // Wait a moment to show 100% before closing
                        setTimeout(function() {
                            $("#upload-progress-modal").modal('hide');
                            $("#msg").html(progress.message || "Upload completed");
                            $("#successModal").modal("show");
                            
                            // Reset file input
                            $("#file").val('');
                        }, 1000);
                    }
                }
            },
            error: function(e) {
                console.log("Error checking progress", e);
                clearInterval(progressInterval);
                $("#upload-progress-modal").modal('hide');
                $("#msg").html("Error checking upload progress. The upload may still be running.");
                $("#successModal").modal("show");
            }
        });
    }, 2000); // Check every 2 seconds
}
