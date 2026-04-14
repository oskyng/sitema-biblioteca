package com.osanzana.bffservice.exception;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpStatusCodeException;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(HttpStatusCodeException.class)
    public ResponseEntity<Object> handleHttpStatusCodeException(HttpStatusCodeException e) {
        logger.error("Error en llamada externa: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
        try {
            // Intentar propagar el cuerpo exacto si es JSON
            ObjectMapper mapper = new ObjectMapper();
            Object errorBody = mapper.readValue(e.getResponseBodyAsString(), Object.class);
            return new ResponseEntity<>(errorBody, e.getStatusCode());
        } catch (Exception ex) {
            // Si no es JSON, devolver el mensaje plano
            Map<String, Object> body = new HashMap<>();
            body.put("status", e.getStatusCode().value());
            body.put("error", "Error en servicio externo");
            body.put("message", e.getResponseBodyAsString());
            return new ResponseEntity<>(body, e.getStatusCode());
        }
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGeneralException(Exception e) {
        logger.error("Error no controlado: ", e);
        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", "Internal Server Error");
        body.put("message", e.getMessage());
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
