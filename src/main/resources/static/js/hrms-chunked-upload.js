/**
 * Sends CSV as small text/plain chunks to avoid Akamai WAF rule 3000180
 * (partial request body inspection on large multipart uploads).
 */
(function (global) {
    var CHUNK_BYTES = 24576;

    function generateUploadId() {
        if (global.crypto && typeof global.crypto.randomUUID === "function") {
            return global.crypto.randomUUID();
        }
        return "upload-" + Date.now() + "-" + Math.random().toString(16).slice(2);
    }

    function postChunk(file, tableName, uploadId, chunkIndex, totalChunks, chunkText) {
        var params = new URLSearchParams({
            uploadId: uploadId,
            chunkIndex: String(chunkIndex),
            totalChunks: String(totalChunks),
            tableName: tableName,
            fileName: file.name
        });

        return $.ajax({
            type: "POST",
            contentType: "text/plain; charset=UTF-8",
            processData: false,
            url: "/api/user/addFileChunk?" + params.toString(),
            data: chunkText,
            timeout: 120000
        });
    }

    function uploadCsvInChunks(file, tableName) {
        return new Promise(function (resolve, reject) {
            var reader = new FileReader();
            reader.onload = function () {
                var text = reader.result;
                if (typeof text !== "string") {
                    reject(new Error("Could not read the CSV file."));
                    return;
                }

                var uploadId = generateUploadId();
                var totalChunks = Math.max(1, Math.ceil(text.length / CHUNK_BYTES));
                var chain = Promise.resolve();

                for (var i = 0; i < totalChunks; i++) {
                    (function (chunkIndex) {
                        chain = chain.then(function () {
                            var start = chunkIndex * CHUNK_BYTES;
                            var chunkText = text.slice(start, start + CHUNK_BYTES);
                            return postChunk(file, tableName, uploadId, chunkIndex, totalChunks, chunkText);
                        });
                    })(i);
                }

                chain.then(resolve).catch(reject);
            };
            reader.onerror = function () {
                reject(new Error("Could not read the CSV file."));
            };
            reader.readAsText(file, "UTF-8");
        });
    }

    global.hrmsUploadCsvInChunks = uploadCsvInChunks;
})(window);
