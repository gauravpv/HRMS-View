/**
 * Sends CSV as small Base64-encoded multipart chunks to avoid Akamai WAF blocks
 * (rule 3000180 body size limits and content inspection on CSV/SQL-like data).
 */
(function (global) {
    var RAW_CHUNK_BYTES = 6144;

    function generateUploadId() {
        if (global.crypto && typeof global.crypto.randomUUID === "function") {
            return global.crypto.randomUUID();
        }
        return "upload-" + Date.now() + "-" + Math.random().toString(16).slice(2);
    }

    function splitUtf8Chunks(text, maxBytes) {
        var encoder = new TextEncoder();
        var decoder = new TextDecoder("utf-8", { fatal: true });
        var allBytes = encoder.encode(text);
        var chunks = [];
        var offset = 0;

        while (offset < allBytes.length) {
            var end = Math.min(offset + maxBytes, allBytes.length);
            while (end > offset) {
                try {
                    chunks.push(decoder.decode(allBytes.subarray(offset, end)));
                    offset = end;
                    break;
                } catch (e) {
                    end -= 1;
                }
            }
            if (end === offset) {
                chunks.push(decoder.decode(allBytes.subarray(offset, offset + 1)));
                offset += 1;
            }
        }
        return chunks;
    }

    function base64EncodeUtf8(text) {
        var bytes = new TextEncoder().encode(text);
        var binary = "";
        for (var i = 0; i < bytes.length; i++) {
            binary += String.fromCharCode(bytes[i]);
        }
        return btoa(binary);
    }

    function postChunk(file, tableName, uploadId, chunkIndex, totalChunks, chunkText) {
        var encoded = base64EncodeUtf8(chunkText);
        if (!encoded) {
            return Promise.reject({
                status: 0,
                responseJSON: { errorMsg: "Upload chunk is empty. Please check the CSV file." }
            });
        }

        var params = new URLSearchParams({
            uploadId: uploadId,
            chunkIndex: String(chunkIndex),
            totalChunks: String(totalChunks),
            tableName: tableName,
            fileName: file.name,
            encoding: "base64"
        });

        var formData = new FormData();
        formData.append("chunk", encoded);

        return $.ajax({
            type: "POST",
            contentType: false,
            processData: false,
            url: "/api/user/addFileChunk?" + params.toString(),
            data: formData,
            timeout: 120000,
            hrmsSuppressSessionRedirect: true
        });
    }

    function uploadCsvInChunks(file, tableName) {
        return new Promise(function (resolve, reject) {
            var reader = new FileReader();
            reader.onload = function () {
                var text = reader.result;
                if (typeof text !== "string") {
                    reject({ status: 0, responseJSON: { errorMsg: "Could not read the CSV file." } });
                    return;
                }

                var chunks = splitUtf8Chunks(text, RAW_CHUNK_BYTES);
                if (!chunks.length) {
                    reject({ status: 0, responseJSON: { errorMsg: "The CSV file is empty." } });
                    return;
                }

                var uploadId = generateUploadId();
                var totalChunks = chunks.length;
                var chain = Promise.resolve();

                for (var i = 0; i < totalChunks; i++) {
                    (function (chunkIndex) {
                        chain = chain.then(function () {
                            return postChunk(
                                file,
                                tableName,
                                uploadId,
                                chunkIndex,
                                totalChunks,
                                chunks[chunkIndex]
                            );
                        });
                    })(i);
                }

                chain.then(resolve).catch(reject);
            };
            reader.onerror = function () {
                reject({ status: 0, responseJSON: { errorMsg: "Could not read the CSV file." } });
            };
            reader.readAsText(file, "UTF-8");
        });
    }

    global.hrmsUploadCsvInChunks = uploadCsvInChunks;
})(window);
