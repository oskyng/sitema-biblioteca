package org.example.functions.usuarios;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import org.example.models.Usuario;
import org.example.util.DatabaseConfig;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UsuarioFunction {

    private static final ObjectMapper mapper = new ObjectMapper();

    @FunctionName("usuarios")
    public HttpResponseMessage run(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE},
                    route = "usuarios",
                    authLevel = AuthorizationLevel.ANONYMOUS)
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        context.getLogger().info("Processing request for Usuarios REST CRUD.");

        try (Connection conn = DatabaseConfig.getConnection(context)) {
            switch (request.getHttpMethod()) {
                case GET:
                    return handleGet(request, conn);
                case POST:
                    return handlePost(request, conn);
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
            String sql = "SELECT id, nombre, email FROM usuarios WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setLong(1, Long.parseLong(idStr));
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        Usuario u = new Usuario(rs.getLong("id"), rs.getString("nombre"), rs.getString("email"));
                        return request.createResponseBuilder(HttpStatus.OK)
                                .header("Content-Type", "application/json")
                                .body(mapper.writeValueAsString(u))
                                .build();
                    } else {
                        return request.createResponseBuilder(HttpStatus.NOT_FOUND).body("Usuario no encontrado").build();
                    }
                }
            }
        } else {
            List<Usuario> usuarios = new ArrayList<>();
            String sql = "SELECT id, nombre, email FROM usuarios";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    usuarios.add(new Usuario(rs.getLong("id"), rs.getString("nombre"), rs.getString("email")));
                }
            }
            return request.createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(mapper.writeValueAsString(usuarios))
                    .build();
        }
    }

    private HttpResponseMessage handlePost(HttpRequestMessage<Optional<String>> request, Connection conn) throws Exception {
        String body = request.getBody().orElse(null);
        if (body == null) return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Body is required").build();

        Usuario u = mapper.readValue(body, Usuario.class);
        String sql = "INSERT INTO usuarios (nombre, email) VALUES (?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, new String[]{"ID"})) {
            pstmt.setString(1, u.getNombre());
            pstmt.setString(2, u.getEmail());
            pstmt.executeUpdate();
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    u.setId(generatedKeys.getLong(1));
                }
            }
        }
        return request.createResponseBuilder(HttpStatus.CREATED)
                .header("Content-Type", "application/json")
                .body(mapper.writeValueAsString(u))
                .build();
    }

    private HttpResponseMessage handlePut(HttpRequestMessage<Optional<String>> request, Connection conn) throws Exception {
        String body = request.getBody().orElse(null);
        if (body == null) return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Body is required").build();

        Usuario u = mapper.readValue(body, Usuario.class);
        if (u.getId() == null) return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("ID is required for update").build();

        String sql = "UPDATE usuarios SET nombre = ?, email = ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, u.getNombre());
            pstmt.setString(2, u.getEmail());
            pstmt.setLong(3, u.getId());
            int affected = pstmt.executeUpdate();
            if (affected == 0) return request.createResponseBuilder(HttpStatus.NOT_FOUND).body("Usuario no encontrado").build();
        }
        return request.createResponseBuilder(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body(mapper.writeValueAsString(u))
                .build();
    }

    private HttpResponseMessage handleDelete(HttpRequestMessage<Optional<String>> request, Connection conn) throws Exception {
        String idStr = request.getQueryParameters().get("id");
        if (idStr == null) return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("ID parameter is required").build();

        Long usuarioId = Long.parseLong(idStr);

        // Iniciar transacción
        conn.setAutoCommit(false);
        try {
            // 1. Eliminar préstamos asociados
            String deletePrestamosSql = "DELETE FROM prestamos WHERE usuario_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(deletePrestamosSql)) {
                pstmt.setLong(1, usuarioId);
                pstmt.executeUpdate();
            }

            // 2. Eliminar usuario
            String sql = "DELETE FROM usuarios WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setLong(1, usuarioId);
                int affected = pstmt.executeUpdate();
                if (affected == 0) {
                    conn.rollback();
                    return request.createResponseBuilder(HttpStatus.NOT_FOUND).body("Usuario no encontrado").build();
                }
            }

            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }

        return request.createResponseBuilder(HttpStatus.NO_CONTENT).build();
    }
}
