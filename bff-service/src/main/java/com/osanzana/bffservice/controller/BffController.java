package com.osanzana.bffservice.controller;

import com.osanzana.bffservice.service.LibraryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class BffController {

    private final LibraryService libraryService;

    public BffController(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    // Usuarios
    @GetMapping("/usuarios")
    public ResponseEntity<Object> getUsuarios() {
        return libraryService.getAllUsuarios();
    }

    @GetMapping("/usuarios/{id}")
    public ResponseEntity<Object> getUsuario(@PathVariable String id) {
        return libraryService.getUsuarioById(id);
    }

    @PostMapping("/usuarios")
    public ResponseEntity<Object> createUsuario(@RequestBody Object body) {
        return libraryService.createUsuario(body);
    }

    @PutMapping("/usuarios/{id}")
    public ResponseEntity<Object> updateUsuario(@PathVariable String id, @RequestBody Object body) {
        return libraryService.updateUsuario(id, body);
    }

    @DeleteMapping("/usuarios/{id}")
    public void deleteUsuario(@PathVariable String id) {
        libraryService.deleteUsuario(id);
    }

    // Prestamos
    @GetMapping("/prestamos")
    public ResponseEntity<Object> getPrestamos() {
        return libraryService.getAllPrestamos();
    }

    @GetMapping("/prestamos/{id}")
    public ResponseEntity<Object> getPrestamo(@PathVariable String id) {
        return libraryService.getPrestamoById(id);
    }

    @PostMapping("/prestamos")
    public ResponseEntity<Object> createPrestamo(@RequestBody Object body) {
        return libraryService.createPrestamo(body);
    }

    @PutMapping("/prestamos/{id}")
    public ResponseEntity<Object> updatePrestamo(@PathVariable String id, @RequestBody Object body) {
        return libraryService.updatePrestamo(id, body);
    }

    @DeleteMapping("/prestamos/{id}")
    public void deletePrestamo(@PathVariable String id) {
        libraryService.deletePrestamo(id);
    }

    // GraphQL - Libros
    @PostMapping("/libros")
    public ResponseEntity<Object> proxyLibros(@RequestBody java.util.Map<String, Object> body) {
        return libraryService.proxyLibros(body);
    }

    // GraphQL - Autores
    @PostMapping("/autores")
    public ResponseEntity<Object> proxyAutores(@RequestBody java.util.Map<String, Object> body) {
        return libraryService.proxyAutores(body);
    }
}
