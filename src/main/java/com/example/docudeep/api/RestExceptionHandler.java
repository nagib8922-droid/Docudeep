package com.example.docudeep.api;

import com.example.docudeep.service.DocumentValidationException;
import com.example.docudeep.service.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(ex.getMessage()));
    }

    @ExceptionHandler(DocumentValidationException.class)
    public ResponseEntity<Map<String, String>> handleValidation(DocumentValidationException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error(ex.getMessage()));
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    public ResponseEntity<Map<String, String>> handleBadRequest(Exception ex) {
        String message = ex instanceof MethodArgumentNotValidException manve
                ? manve.getBindingResult().getAllErrors().stream().findFirst().map(err -> err.getDefaultMessage()).orElse("Requête invalide")
                : ex.getMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error(message));
    }

    private Map<String, String> error(String message) {
        Map<String, String> payload = new HashMap<>();
        payload.put("error", message);
        return payload;
    }
}
