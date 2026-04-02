package com.smartmeat.exception;

import com.smartmeat.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException e) {
        return error(HttpStatus.NOT_FOUND, "Not Found", e.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
        return error(HttpStatus.BAD_REQUEST, "Bad Request", e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException e) {
        return error(HttpStatus.FORBIDDEN, "Forbidden",
                "You don't have permission to perform this action");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return error(HttpStatus.BAD_REQUEST, "Validation Failed", msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraint(ConstraintViolationException e) {
        return error(HttpStatus.BAD_REQUEST, "Validation Failed", e.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleFileSize(MaxUploadSizeExceededException e) {
        return error(HttpStatus.BAD_REQUEST, "File Too Large", "Maximum file size is 10MB");
    }

    /**
     * SSE (Server-Sent Events) connections produce AsyncRequestTimeoutException
     * when the client disconnects or the server shuts down.
     * The response is already a text/event-stream so we CANNOT write a JSON body —
     * just return null and let Tomcat close the connection cleanly.
     */
    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public ResponseEntity<Void> handleAsyncTimeout(
            AsyncRequestTimeoutException e,
            HttpServletRequest request) {
        // Only log at DEBUG — this is normal behaviour for long-lived SSE connections
        log.debug("SSE connection timed out for {}", request.getRequestURI());
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(
            Exception e,
            HttpServletRequest request) {

        // SSE requests use text/event-stream — we cannot serialize JSON into them.
        // Silently swallow any exception that slips through on an async/SSE request.
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("text/event-stream")) {
            log.debug("Swallowing exception on SSE request {}: {}",
                    request.getRequestURI(), e.getMessage());
            return ResponseEntity.noContent().build();
        }

        log.error("Unhandled exception on {}", request.getRequestURI(), e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected error occurred. Please try again.");
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String error, String msg) {
        return ResponseEntity.status(status).body(
                ErrorResponse.builder()
                        .status(status.value())
                        .error(error)
                        .message(msg)
                        .timestamp(Instant.now())
                        .build()
        );
    }
    

}