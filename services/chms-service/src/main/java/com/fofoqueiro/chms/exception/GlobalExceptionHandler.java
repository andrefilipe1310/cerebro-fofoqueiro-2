// @path services/chms-service/src/main/java/com/fofoqueiro/chms/exception/GlobalExceptionHandler.java
// @owner chms-service
// @responsibility Centraliza tratamento de exceções — nunca vaza stack trace para o cliente
// @see docs/CODE_STYLE.md#padroes-obrigatorios
package com.fofoqueiro.chms.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(apiError("NOT_FOUND", "Recurso não encontrado", 404, null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex, WebRequest request) {
        log.error("Erro não tratado em chms-service: {}", ex.getMessage(), ex);
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
