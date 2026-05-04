package com.osanzana.bffservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.osanzana.bffservice.model.Prestamo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class LibraryService {

    private static final Logger logger = LoggerFactory.getLogger(LibraryService.class);
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${functions.usuarios.url}")
    private String usuariosUrl;

    @Value("${functions.prestamos.url}")
    private String prestamosUrl;

    @Value("${functions.libros.url}")
    private String librosUrl;

    @Value("${functions.autores.url}")
    private String autoresUrl;

    public LibraryService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    // Usuarios
    public ResponseEntity<Object> getAllUsuarios() {
        logger.info("Obteniendo todos los usuarios de: {}", usuariosUrl);
        return restTemplate.getForEntity(usuariosUrl, Object.class);
    }

    public ResponseEntity<Object> getUsuarioById(String id) {
        logger.info("Obteniendo usuario ID: {} de: {}", id, usuariosUrl);
        return restTemplate.getForEntity(usuariosUrl + "?id=" + id, Object.class);
    }

    public ResponseEntity<Object> createUsuario(Object body) {
        logger.info("Creando usuario en: {} con body: {}", usuariosUrl, body);
        try {
            String jsonBody = objectMapper.writeValueAsString(body);
            logger.info("JSON a enviar: {}", jsonBody);
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(jsonBody, headers);
            return restTemplate.postForEntity(usuariosUrl, entity, Object.class);
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            logger.error("Error HTTP en servicio de usuarios: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            logger.error("Error al preparar la solicitud: {}", e.getMessage(), e);
            throw new RuntimeException("Error al preparar la solicitud: " + e.getMessage(), e);
        }
    }

    public ResponseEntity<Object> updateUsuario(String id, Object body) {
        logger.info("Actualizando usuario ID: {} en: {} con body: {}", id, usuariosUrl, body);
        try {
            String jsonBody = objectMapper.writeValueAsString(body);
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(jsonBody, headers);
            return restTemplate.exchange(usuariosUrl + "?id=" + id, org.springframework.http.HttpMethod.PUT, entity, Object.class);
        } catch (Exception e) {
            logger.error("Error al actualizar usuario: {}", e.getMessage());
            throw new RuntimeException("Error al actualizar usuario: " + e.getMessage(), e);
        }
    }

    public void deleteUsuario(String id) {
        logger.info("Eliminando usuario ID: {} de: {}", id, usuariosUrl);
        restTemplate.delete(usuariosUrl + "?id=" + id);
    }

    // Prestamos
    public ResponseEntity<Object> getAllPrestamos() {
        logger.info("Obteniendo todos los prestamos de: {}", prestamosUrl);
        return restTemplate.getForEntity(prestamosUrl, Object.class);
    }

    public ResponseEntity<Object> getPrestamoById(String id) {
        logger.info("Obteniendo prestamo ID: {} de: {}", id, prestamosUrl);
        return restTemplate.getForEntity(prestamosUrl + "?id=" + id, Object.class);
    }

    public ResponseEntity<Object> createPrestamo(Object body) {
        logger.info("Creando prestamo en: {} con body: {}", prestamosUrl, body);
        try {
            Prestamo p = objectMapper.convertValue(body, Prestamo.class);
            String jsonBody = objectMapper.writeValueAsString(p);
            logger.info("JSON a enviar: {}", jsonBody);
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(jsonBody, headers);
            return restTemplate.postForEntity(prestamosUrl, entity, Object.class);
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            logger.error("Error HTTP en servicio de prestamos: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            logger.error("Error al preparar la solicitud: {}", e.getMessage(), e);
            throw new RuntimeException("Error al preparar la solicitud: " + e.getMessage(), e);
        }
    }

    public ResponseEntity<Object> updatePrestamo(String id, Object body) {
        logger.info("Actualizando prestamo ID: {} en: {} con body: {}", id, prestamosUrl, body);
        try {
            String jsonBody = objectMapper.writeValueAsString(body);
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(jsonBody, headers);
            return restTemplate.exchange(prestamosUrl + "?id=" + id, org.springframework.http.HttpMethod.PUT, entity, Object.class);
        } catch (Exception e) {
            logger.error("Error al actualizar prestamo: {}", e.getMessage());
            throw new RuntimeException("Error al actualizar prestamo: " + e.getMessage(), e);
        }
    }

    public void deletePrestamo(String id) {
        logger.info("Eliminando prestamo ID: {} de: {}", id, prestamosUrl);
        restTemplate.delete(prestamosUrl + "?id=" + id);
    }

    // Libros (GraphQL)
    public ResponseEntity<Object> proxyLibros(java.util.Map<String, Object> body) {
        logger.info("Redirigiendo petición GraphQL a la función de Libros en: {}", librosUrl);
        try {
            String jsonBody = objectMapper.writeValueAsString(body);
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(jsonBody, headers);
            return restTemplate.postForEntity(librosUrl, entity, Object.class);
        } catch (Exception e) {
            logger.error("Error al redirigir petición GraphQL a Libros: {}", e.getMessage());
            throw new RuntimeException("Error al redirigir petición GraphQL: " + e.getMessage(), e);
        }
    }

    // Autores (GraphQL)
    public ResponseEntity<Object> proxyAutores(java.util.Map<String, Object> body) {
        logger.info("Redirigiendo petición GraphQL a la función de Autores en: {}", autoresUrl);
        try {
            String jsonBody = objectMapper.writeValueAsString(body);
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(jsonBody, headers);
            return restTemplate.postForEntity(autoresUrl, entity, Object.class);
        } catch (Exception e) {
            logger.error("Error al redirigir petición GraphQL a Autores: {}", e.getMessage());
            throw new RuntimeException("Error al redirigir petición GraphQL: " + e.getMessage(), e);
        }
    }
}
