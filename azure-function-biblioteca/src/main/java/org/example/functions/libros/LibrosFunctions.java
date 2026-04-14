package org.example.functions.libros;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import org.example.models.Libro;
import org.example.util.DatabaseConfig;

import java.sql.*;
import java.util.*;

public class LibrosFunctions {

    private static final ObjectMapper mapper = new ObjectMapper();

    @FunctionName("libros")
    public HttpResponseMessage run(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.GET, HttpMethod.POST},
                    route = "libros",
                    authLevel = AuthorizationLevel.ANONYMOUS)
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        context.getLogger().info("Processing GraphQL request for Libros.");

        String body = request.getBody().orElse(null);

        try (Connection conn = DatabaseConfig.getConnection(context)) {
            Object data = null;
            if (request.getHttpMethod() == HttpMethod.GET || body == null) {
                data = getAllLibros(conn);
            } else {
                JsonNode jsonNode = mapper.readTree(body);
                String query = jsonNode.has("query") ? jsonNode.get("query").asText() : "";
                
                if (query.contains("getLibros") || query.contains("libros")) {
                    data = getAllLibros(conn);
                } else if (query.contains("getLibro") || query.contains("libroById")) {
                    Long id = jsonNode.get("variables").get("id").asLong();
                    data = getLibro(conn, id);
                } else if (query.contains("createLibro")) {
                    String titulo = jsonNode.get("variables").get("titulo").asText();
                    Long autorId = jsonNode.get("variables").get("autorId").asLong();
                    Integer disponible = jsonNode.get("variables").get("disponible").asInt();
                    data = createLibro(conn, titulo, autorId, disponible);
                } else if (query.contains("updateLibro")) {
                    Long id = jsonNode.get("variables").get("id").asLong();
                    String titulo = jsonNode.get("variables").get("titulo").asText();
                    Long autorId = jsonNode.get("variables").get("autorId").asLong();
                    Integer disponible = jsonNode.get("variables").get("disponible").asInt();
                    data = updateLibro(conn, id, titulo, autorId, disponible);
                } else if (query.contains("deleteLibro")) {
                    Long id = jsonNode.get("variables").get("id").asLong();
                    data = deleteLibro(conn, id);
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("data", data);
            return request.createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(mapper.writeValueAsString(response))
                    .build();

        } catch (Exception e) {
            context.getLogger().severe("Error: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"errors\": [{\"message\": \"" + e.getMessage() + "\"}]}")
                    .build();
        }
    }

    private List<Libro> getAllLibros(Connection conn) throws Exception {
        List<Libro> list = new ArrayList<>();
        String sql = "SELECT id, titulo, autor_id, disponible FROM libros";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Libro(rs.getLong("id"), rs.getString("titulo"), rs.getLong("autor_id"), rs.getInt("disponible")));
            }
        }
        return list;
    }

    private Libro getLibro(Connection conn, Long id) throws Exception {
        String sql = "SELECT id, titulo, autor_id, disponible FROM libros WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Libro(rs.getLong("id"), rs.getString("titulo"), rs.getLong("autor_id"), rs.getInt("disponible"));
                }
            }
        }
        return null;
    }

    private Libro createLibro(Connection conn, String titulo, Long autorId, Integer disponible) throws Exception {
        String sql = "INSERT INTO libros (titulo, autor_id, disponible) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, new String[]{"ID"})) {
            pstmt.setString(1, titulo);
            pstmt.setLong(2, autorId);
            pstmt.setInt(3, disponible);
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return new Libro(rs.getLong(1), titulo, autorId, disponible);
                }
            }
        }
        return null;
    }

    private Libro updateLibro(Connection conn, Long id, String titulo, Long autorId, Integer disponible) throws Exception {
        String sql = "UPDATE libros SET titulo = ?, autor_id = ?, disponible = ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, titulo);
            pstmt.setLong(2, autorId);
            pstmt.setInt(3, disponible);
            pstmt.setLong(4, id);
            pstmt.executeUpdate();
            return new Libro(id, titulo, autorId, disponible);
        }
    }

    private String deleteLibro(Connection conn, Long id) throws Exception {
        String sql = "DELETE FROM libros WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            pstmt.executeUpdate();
            return "Libro eliminado";
        }
    }
}
