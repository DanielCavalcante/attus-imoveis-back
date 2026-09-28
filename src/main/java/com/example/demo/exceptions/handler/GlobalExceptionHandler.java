package com.example.demo.exceptions.handler;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;

import org.springframework.security.access.AccessDeniedException;

import com.example.demo.exceptions.custom.BusinessException;
import com.example.demo.exceptions.custom.ConflictException;
import com.example.demo.exceptions.custom.ResourceNotFoundException;
import com.example.demo.exceptions.models.ApiError;

import jakarta.persistence.EntityNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final org.slf4j.Logger log =
        org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(EntityNotFoundException ex) {
        ApiError error = new ApiError(HttpStatus.NOT_FOUND.value(), ex.getMessage(), LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusinessException(BusinessException ex) {
        ApiError error = new ApiError(HttpStatus.BAD_REQUEST.value(), ex.getMessage(), LocalDateTime.now());

        return ResponseEntity.badRequest().body(error);
    }

    /** Recurso existe e e publico, mas nao e desta conta. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        ApiError error = new ApiError(HttpStatus.FORBIDDEN.value(), ex.getMessage(), LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * Corrida entre duas requisicoes numa restricao unica. A mensagem do banco
     * nomeia tabela e restricao, e isso nao vai para quem chama.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Violacao de integridade", ex);

        ApiError error = new ApiError(
            HttpStatus.CONFLICT.value(),
            "Estes dados ja estao em uso",
            LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException ex) {
        ApiError error = new ApiError(HttpStatus.CONFLICT.value(), ex.getMessage(), LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /** A mensagem nomeia o campo reprovado: sem isso quem consome nao sabe o que corrigir. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(field -> field.getField() + ": " + field.getDefaultMessage())
            .reduce((a, b) -> a + "; " + b)
            .orElse("Requisicao invalida");

        ApiError error = new ApiError(HttpStatus.BAD_REQUEST.value(), message, LocalDateTime.now());

        return ResponseEntity.badRequest().body(error);
    }

    /** Enum fora do conjunto aceito chega aqui como corpo ilegivel, e vale 400, nao 500. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex) {
        ApiError error = new ApiError(HttpStatus.BAD_REQUEST.value(), "Corpo da requisicao invalido", LocalDateTime.now());

        return ResponseEntity.badRequest().body(error);
    }

    /** Respondia 400 apesar do nome. Recurso ausente e 404. */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFoundException(ResourceNotFoundException ex) {
        ApiError error = new ApiError(HttpStatus.NOT_FOUND.value(), ex.getMessage(), LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

}
