package org.example.functions.eventos;

import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;
import com.azure.messaging.eventgrid.EventGridEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.models.Prestamo;
import org.example.util.DatabaseConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Azure Functions with Event Grid trigger.
 */
public class BibliotecaEventGridFunction {

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * This function will be invoked when an event is received from Event Grid.
     */
    @FunctionName("BibliotecaEventGridFunction")
    public void run(
            @EventGridTrigger(name = "eventGridEvent") EventGridEvent event,
            final ExecutionContext context) {
        context.getLogger().info("Java Event Grid trigger function executed.");
        context.getLogger().info("Subject: " + event.getSubject());
        context.getLogger().info("Type: " + event.getEventType());

        if ("Biblioteca.PrestamoCreado".equals(event.getEventType())) {
            try {
                Prestamo p = event.getData().toObject(Prestamo.class);
                procesarPrestamo(p, context);
            } catch (Exception e) {
                context.getLogger().severe("Error al procesar el evento de préstamo: " + e.getMessage());
            }
        }
    }

    private void procesarPrestamo(Prestamo p, ExecutionContext context) {
        try (Connection conn = DatabaseConfig.getConnection(context)) {
            conn.setAutoCommit(false);
            try {
                // 1. Validar si el usuario ya tiene este libro prestado
                String duplicateCheckSql = "SELECT COUNT(*) FROM prestamos WHERE usuario_id = ? AND libro_id = ? AND estado = 'ACTIVO'";
                try (PreparedStatement dupStmt = conn.prepareStatement(duplicateCheckSql)) {
                    dupStmt.setLong(1, p.getUsuarioId());
                    dupStmt.setLong(2, p.getLibroId());
                    try (ResultSet rs = dupStmt.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            context.getLogger().warning("El usuario " + p.getUsuarioId() + " ya tiene un préstamo activo para el libro " + p.getLibroId());
                            conn.rollback();
                            return;
                        }
                    }
                }

                // 2. Verificar disponibilidad y restar 1
                String checkSql = "SELECT disponible FROM libros WHERE id = ?";
                try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                    checkStmt.setLong(1, p.getLibroId());
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (rs.next()) {
                            int disponible = rs.getInt("disponible");
                            if (disponible <= 0) {
                                context.getLogger().warning("Libro no disponible para ID: " + p.getLibroId());
                                conn.rollback();
                                return;
                            }
                        } else {
                            context.getLogger().warning("Libro no encontrado para ID: " + p.getLibroId());
                            conn.rollback();
                            return;
                        }
                    }
                }

                String updateLibroSql = "UPDATE libros SET disponible = disponible - 1 WHERE id = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateLibroSql)) {
                    updateStmt.setLong(1, p.getLibroId());
                    updateStmt.executeUpdate();
                }

                // 2. Insertar préstamo
                String sql = "INSERT INTO prestamos (usuario_id, libro_id, fecha_prestamo, fecha_devolucion, estado) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setLong(1, p.getUsuarioId());
                    pstmt.setLong(2, p.getLibroId());
                    pstmt.setDate(3, p.getFechaPrestamo());
                    pstmt.setDate(4, p.getFechaDevolucion());
                    pstmt.setString(5, p.getEstado() != null ? p.getEstado() : "ACTIVO");
                    pstmt.executeUpdate();
                }

                conn.commit();
                context.getLogger().info("Prestamo procesado exitosamente en BD para usuario: " + p.getUsuarioId());

            } catch (Exception e) {
                conn.rollback();
                context.getLogger().severe("Error en la transacción de préstamo: " + e.getMessage());
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            context.getLogger().severe("Error de conexión a BD: " + e.getMessage());
        }
    }
}
