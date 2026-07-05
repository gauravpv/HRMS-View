package com.app.web;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import com.app.dto.AjaxError;
import com.app.exception.HrmsApiException;
import com.app.support.UserFacingMessages;

@RestControllerAdvice(basePackages = "com.app.controller")
public class GlobalRestExceptionHandler {

    private static final Logger logger = LogManager.getLogger(GlobalRestExceptionHandler.class);

    @ExceptionHandler(HrmsApiException.class)
    public ResponseEntity<AjaxError> handleApiException(HrmsApiException ex) {
        if (ex.isUserFacingValidation() || ex.getClientMessage().equals(ex.getMessage())) {
            logger.warn("API request rejected: {}", ex.getMessage());
        } else {
            logger.error("API request failed (user message: {}): {}", ex.getClientMessage(), ex.getMessage(), ex);
        }
        return ApiResponses.clientError(ex);
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MissingServletRequestPartException.class,
            MethodArgumentTypeMismatchException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<AjaxError> handleBadRequest(Exception ex) {
        logger.warn("Bad request: {}", ex.getMessage());
        if (ex instanceof MissingServletRequestPartException partEx
                && "chunk".equals(partEx.getRequestPartName())) {
            logger.warn(
                    "Upload chunk part missing — request body may have been removed by WAF/proxy: {}",
                    ex.getMessage());
            return ApiResponses.clientError(UserFacingMessages.UPLOAD_FAILED);
        }
        return ApiResponses.clientError("Invalid request parameters.");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<AjaxError> handleUnreadableBody(HttpMessageNotReadableException ex) {
        if (ex.getMessage() != null && ex.getMessage().contains("Required request body is missing")) {
            logger.warn(
                    "Required request body missing — upload payload may have been removed by WAF/proxy: {}",
                    ex.getMessage());
            return ApiResponses.clientError(UserFacingMessages.UPLOAD_FAILED);
        }
        logger.warn("Unreadable request body: {}", ex.getMessage());
        return ApiResponses.clientError("Invalid request body.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<AjaxError> handleUnexpected(Exception ex) {
        logger.error("Unhandled API error", ex);
        return ApiResponses.clientError("An unexpected error occurred. Please contact admin.");
    }
}
