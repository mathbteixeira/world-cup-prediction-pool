package io.github.mathbteixeira.worldcuppredictionpool.common.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ApiErrorResponse> handleResponseStatusException(ResponseStatusException exception,
                                                                   HttpServletRequest request) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        String message = Optional.ofNullable(exception.getReason())
                .filter(reason -> !reason.isBlank())
                .orElse(status.getReasonPhrase());
        return ResponseEntity.status(status).body(toResponse(status, message, request));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
                                                                  HttpServletRequest request) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Request validation failed");
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(toResponse(status, message, request));
    }

    @ExceptionHandler({ConstraintViolationException.class, HttpMessageNotReadableException.class})
    ResponseEntity<ApiErrorResponse> handleBadRequest(Exception exception,
                                                      HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(toResponse(status, "Request validation failed", request));
    }

    private ApiErrorResponse toResponse(HttpStatus status, String message, HttpServletRequest request) {
        return new ApiErrorResponse(
                Instant.now(),
                status.name(),
                status.value(),
                message,
                request.getRequestURI()
        );
    }
}
