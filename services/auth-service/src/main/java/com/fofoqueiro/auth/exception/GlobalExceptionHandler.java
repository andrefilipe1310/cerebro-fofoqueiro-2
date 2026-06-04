// @path services/auth-service/src/main/java/com/fofoqueiro/auth/exception/GlobalExceptionHandler.java
// @owner auth-service
// @responsibility Centraliza tratamento de exceções — nunca vaza stack trace para o cliente
// @see docs/CODE_STYLE.md#padroes-obrigatorios | docs/API_CONTRACTS.md#convencoes
package com.fofoqueiro.auth.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField, f -> f.getDefaultMessage() != null ? f.getDefaultMessage() : "inválido"));
        return ResponseEntity.badRequest().body(apiError("VALIDATION_ERROR", "Dados inválidos na requisição", 400, fields));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(apiError("INVALID_CREDENTIALS", "Credenciais inválidas", 401, null));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(apiError("ACCESS_DENIED", "Sem permissão para acessar este recurso", 403, null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex, WebRequest request) {
        log.error("Erro não tratado em auth-service: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(apiError("INTERNAL_ERROR", "Erro interno do servidor", 500, null));
    }

    private Map<String, Object> apiError(String code, String message, int status, Object details) {
        var error = new java.util.LinkedHashMap<String, Object>();
        error.put("code", code);
        error.put("message", message);
        error.put("status", status);
        error.put("timestamp", Instant.now().toString());
        if (details != null) error.put("details", details);
        return Map.of("error", error);
    }
}
