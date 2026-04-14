package org.example.models;

public class Libro {
    private Long id;
    private String titulo;
    private Long autorId;
    private Integer disponible;

    public Libro() {}
    public Libro(Long id, String titulo, Long autorId, Integer disponible) {
        this.id = id;
        this.titulo = titulo;
        this.autorId = autorId;
        this.disponible = disponible;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public Long getAutorId() { return autorId; }
    public void setAutorId(Long autorId) { this.autorId = autorId; }
    public Integer getDisponible() { return disponible; }
    public void setDisponible(Integer disponible) { this.disponible = disponible; }
}
