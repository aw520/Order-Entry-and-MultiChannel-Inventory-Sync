package org.example.backend.error;

import java.sql.SQLException;
import java.time.Instant;

import jakarta.validation.ConstraintViolationException;

import org.example.backend.dto.ErrorResponse;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    //quantity is 0 and fails @Min(1).
    ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse("Request validation failed.");
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    //limit=101 fails @Max(100).
    ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> violation.getMessage())
                .orElse("Request validation failed.");
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    //quantity is "abc" instead of a number.
    ResponseEntity<ErrorResponse> handleUnreadableRequest() {
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request body is missing or malformed.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    //GET /api/orders/abc, where id must be a long.
    ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return response(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "%s has an invalid value.".formatted(exception.getName())
        );
    }

    @ExceptionHandler(NotFoundException.class)
    //GET /api/orders/999 when the order does not exist.
    ResponseEntity<ErrorResponse> handleNotFound(NotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, exception.code(), exception.getMessage());
    }

    @ExceptionHandler(InsufficientInventoryException.class)
    //ordering 2 units when only 1 CSR unit is available.
    ResponseEntity<ErrorResponse> handleInsufficientInventory(InsufficientInventoryException exception) {
        return response(HttpStatus.CONFLICT, "INSUFFICIENT_CSR_INVENTORY", exception.getMessage());
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    //a database row lock times out.
    ResponseEntity<ErrorResponse> handleLockConflict(PessimisticLockingFailureException exception) {
        log.warn("Temporary database lock conflict", exception);
        return temporaryDatabaseConflict();
    }

    @ExceptionHandler(UnableToExecuteStatementException.class)
    //JDBI encounters a deadlock or SQL execution failure.
    ResponseEntity<ErrorResponse> handleJdbiFailure(UnableToExecuteStatementException exception) {
        if (isTransientDatabaseConflict(exception)) {
            log.warn("Temporary database lock conflict", exception);
            return temporaryDatabaseConflict();
        }
        log.error("Unexpected database failure", exception);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred.");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
        log.error("Unexpected request failure", exception);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred.");
    }

    private boolean isTransientDatabaseConflict(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof SQLException sqlException
                    && (sqlException.getErrorCode() == 1205 || sqlException.getErrorCode() == 1213)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private ResponseEntity<ErrorResponse> temporaryDatabaseConflict() {
        return response(
                HttpStatus.SERVICE_UNAVAILABLE,
                "TEMPORARY_DATABASE_CONFLICT",
                "Inventory is temporarily busy. Please retry."
        );
    }

    private ResponseEntity<ErrorResponse> response(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(code, message, Instant.now()));
    }
}
