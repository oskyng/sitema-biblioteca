package org.example.models;

import java.sql.Date;

public class Prestamo {
    private Long id;
    private Long usuarioId;
    private Long libroId;
    private Date fechaPrestamo;
    private Date fechaDevolucion;
    private String estado;

    public Prestamo() {}
    public Prestamo(Long id, Long usuarioId, Long libroId, Date fechaPrestamo, Date fechaDevolucion, String estado) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.libroId = libroId;
        this.fechaPrestamo = fechaPrestamo;
        this.fechaDevolucion = fechaDevolucion;
        this.estado = estado;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    public Long getLibroId() { return libroId; }
    public void setLibroId(Long libroId) { this.libroId = libroId; }
    public Date getFechaPrestamo() { return fechaPrestamo; }
    public void setFechaPrestamo(Date fechaPrestamo) { this.fechaPrestamo = fechaPrestamo; }
    public Date getFechaDevolucion() { return fechaDevolucion; }
    public void setFechaDevolucion(Date fechaDevolucion) { this.fechaDevolucion = fechaDevolucion; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
