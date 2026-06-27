// @path services/camera-service/src/main/java/com/fofoqueiro/camera/exception/GlobalExceptionHandler.java
// @owner camera-service
// @responsibility Centraliza tratamento de exceções — nunca vaza stack trace para o cliente
// @see docs/CODE_STYLE.md#padroes-obrigatorios | docs/API_CONTRACTS.md#convencoes
package com.fofoqueiro.camera.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.LinkedHashMap;
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

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(apiError("ACCESS_DENIED", "Sem permissão para acessar este recurso", 403, null));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(apiError("NOT_FOUND", "Recurso não encontrado", 404, null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex, WebRequest request) {
        log.error("Erro não tratado em camera-service: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(apiError("INTERNAL_ERROR", "Erro interno do servidor", 500, null));
    }

    private Map<String, Object> apiError(String code, String message, int status, Object details) {
        var error = new LinkedHashMap<String, Object>();
        error.put("code", code);
        error.put("message", message);
        error.put("status", status);
        error.put("timestamp", Instant.now().toString());
        if (details != null) error.put("details", details);
        return Map.of("error", error);
    }
}
