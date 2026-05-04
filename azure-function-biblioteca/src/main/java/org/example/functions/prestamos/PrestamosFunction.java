package org.example.functions.prestamos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import org.example.models.Prestamo;
import org.example.util.DatabaseConfig;
import org.example.util.EventGridUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PrestamosFunction {

    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @FunctionName("prestamos")
    public HttpResponseMessage run(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE},
                    route = "prestamos",
                    authLevel = AuthorizationLevel.ANONYMOUS)
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        context.getLogger().info("Processing request for Prestamos REST CRUD.");

        try (Connection conn = DatabaseConfig.getConnection(context)) {
            switch (request.getHttpMethod()) {
                case GET:
                    return handleGet(request, conn);
                case POST:
                    return handlePost(request, conn, context);
                case PUT:
                    return handlePut(request, conn);
                case DELETE:
                    return handleDelete(request, conn);
                default:
                    return request.createResponseBuilder(HttpStatus.METHOD_NOT_ALLOWED).build();
            }
        } catch (Exception e) {
            context.getLogger().severe("Error: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage())
                    .build();
        }
    }

    private HttpResponseMessage handleGet(HttpRequestMessage<Optional<String>> request, Connection conn) throws Exception {
        String idStr = request.getQueryParameters().get("id");
        if (idStr != null) {
            String sql = "SELECT id, usuario_id, libro_id, fecha_prestamo, fecha_devolucion, estado FROM prestamos WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setLong(1, Long.parseLong(idStr));
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        Prestamo p = new Prestamo(
                                rs.getLong("id"),
                                rs.getLong("usuario_id"),
                                rs.getLong("libro_id"),
                                rs.getObject("fecha_prestamo", java.time.LocalDate.class),
                                rs.getObject("fecha_devolucion", java.time.LocalDate.class),
                                rs.getString("estado")
                        );
                        return request.createResponseBuilder(HttpStatus.OK)
                                .header("Content-Type", "application/json")
                                .body(mapper.writeValueAsString(p))
                                .build();
                    } else {
                        return request.createResponseBuilder(HttpStatus.NOT_FOUND).body("Prestamo no encontrado").build();
                    }
                }
            }
        } else {
            List<Prestamo> prestamos = new ArrayList<>();
            String sql = "SELECT id, usuario_id, libro_id, fecha_prestamo, fecha_devolucion, estado FROM prestamos";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    prestamos.add(new Prestamo(
                            rs.getLong("id"),
                            rs.getLong("usuario_id"),
                            rs.getLong("libro_id"),
                            rs.getObject("fecha_prestamo", java.time.LocalDate.class),
                            rs.getObject("fecha_devolucion", java.time.LocalDate.class),
                            rs.getString("estado")
                    ));
                }
            }
            return request.createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(mapper.writeValueAsString(prestamos))
                    .build();
        }
    }

    private HttpResponseMessage handlePost(HttpRequestMessage<Optional<String>> request, Connection conn, final ExecutionContext context) throws Exception {
        String body = request.getBody().orElse(null);
        if (body == null) return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Body is required").build();

        Prestamo p = mapper.readValue(body, Prestamo.class);
        
        // Asignar fechas por defecto si no vienen
        if (p.getFechaPrestamo() == null) {
            p.setFechaPrestamo(java.time.LocalDate.now());
        }
        if (p.getFechaDevolucion() == null) {
            p.setFechaDevolucion(p.getFechaPrestamo().plusDays(14));
        }
        
        // Publicar evento a Event Grid
        // La lógica de BD ahora se manejará asincrónicamente en el consumidor del evento
        EventGridUtil.publishEvent(
            "/biblioteca/prestamos/solicitud",
            "Biblioteca.PrestamoCreado",
            p,
            context
        );

        return request.createResponseBuilder(HttpStatus.ACCEPTED)
                .header("Content-Type", "application/json")
                .body(mapper.writeValueAsString(p))
                .build();
    }

    private HttpResponseMessage handlePut(HttpRequestMessage<Optional<String>> request, Connection conn) throws Exception {
        String body = request.getBody().orElse(null);
        if (body == null) return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Body is required").build();

        Prestamo p = mapper.readValue(body, Prestamo.class);
        if (p.getId() == null) return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("ID is required for update").build();

        String sql = "UPDATE prestamos SET usuario_id = ?, libro_id = ?, fecha_prestamo = ?, fecha_devolucion = ?, estado = ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, p.getUsuarioId());
            pstmt.setLong(2, p.getLibroId());
            pstmt.setObject(3, p.getFechaPrestamo());
            pstmt.setObject(4, p.getFechaDevolucion());
            pstmt.setString(5, p.getEstado());
            pstmt.setLong(6, p.getId());
            int affected = pstmt.executeUpdate();
            if (affected == 0) return request.createResponseBuilder(HttpStatus.NOT_FOUND).body("Prestamo no encontrado").build();
        }
        return request.createResponseBuilder(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body(mapper.writeValueAsString(p))
                .build();
    }

    private HttpResponseMessage handleDelete(HttpRequestMessage<Optional<String>> request, Connection conn) throws Exception {
        String idStr = request.getQueryParameters().get("id");
        if (idStr == null) return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("ID parameter is required").build();

        String sql = "DELETE FROM prestamos WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, Long.parseLong(idStr));
            int affected = pstmt.executeUpdate();
            if (affected == 0) return request.createResponseBuilder(HttpStatus.NOT_FOUND).body("Prestamo no encontrado").build();
        }
        return request.createResponseBuilder(HttpStatus.NO_CONTENT).build();
    }
}
