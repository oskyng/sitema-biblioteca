package org.example.functions.autores;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import org.example.models.Autor;
import org.example.util.DatabaseConfig;

import java.sql.*;
import java.util.*;

public class AutoresFunction {

    private static final ObjectMapper mapper = new ObjectMapper();

    @FunctionName("autores")
    public HttpResponseMessage run(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.GET, HttpMethod.POST},
                    route = "autores",
                    authLevel = AuthorizationLevel.ANONYMOUS)
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        context.getLogger().info("Processing GraphQL request for Autores.");

        String body = request.getBody().orElse(null);

        try (Connection conn = DatabaseConfig.getConnection(context)) {
            Object data = null;
            if (request.getHttpMethod() == HttpMethod.GET || body == null) {
                // Return all authors for simple GET or POST with no body
                data = getAllAutores(conn);
            } else {
                JsonNode jsonNode = mapper.readTree(body);
                String query = jsonNode.has("query") ? jsonNode.get("query").asText() : "";
                
                if (query.contains("getAutores")) {
                    data = getAllAutores(conn);
                } else if (query.contains("getAutor")) {
                    Long id = jsonNode.get("variables").get("id").asLong();
                    data = getAutor(conn, id);
                } else if (query.contains("createAutor")) {
                    String nombre = jsonNode.get("variables").get("nombre").asText();
                    String nacionalidad = jsonNode.get("variables").get("nacionalidad").asText();
                    data = createAutor(conn, nombre, nacionalidad);
                } else if (query.contains("updateAutor")) {
                    Long id = jsonNode.get("variables").get("id").asLong();
                    String nombre = jsonNode.get("variables").get("nombre").asText();
                    String nacionalidad = jsonNode.get("variables").get("nacionalidad").asText();
                    data = updateAutor(conn, id, nombre, nacionalidad);
                } else if (query.contains("deleteAutor")) {
                    Long id = jsonNode.get("variables").get("id").asLong();
                    data = deleteAutor(conn, id);
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

    private List<Autor> getAllAutores(Connection conn) throws Exception {
        List<Autor> list = new ArrayList<>();
        String sql = "SELECT id, nombre, nacionalidad FROM autores";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Autor(rs.getLong("id"), rs.getString("nombre"), rs.getString("nacionalidad")));
            }
        }
        return list;
    }

    private Autor getAutor(Connection conn, Long id) throws Exception {
        String sql = "SELECT id, nombre, nacionalidad FROM autores WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Autor(rs.getLong("id"), rs.getString("nombre"), rs.getString("nacionalidad"));
                }
            }
        }
        return null;
    }

    private Autor createAutor(Connection conn, String nombre, String nacionalidad) throws Exception {
        String sql = "INSERT INTO autores (nombre, nacionalidad) VALUES (?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, new String[]{"ID"})) {
            pstmt.setString(1, nombre);
            pstmt.setString(2, nacionalidad);
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return new Autor(rs.getLong(1), nombre, nacionalidad);
                }
            }
        }
        return null;
    }

    private Autor updateAutor(Connection conn, Long id, String nombre, String nacionalidad) throws Exception {
        String sql = "UPDATE autores SET nombre = ?, nacionalidad = ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nombre);
            pstmt.setString(2, nacionalidad);
            pstmt.setLong(3, id);
            pstmt.executeUpdate();
            return new Autor(id, nombre, nacionalidad);
        }
    }

    private String deleteAutor(Connection conn, Long id) throws Exception {
        String sql = "DELETE FROM autores WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            pstmt.executeUpdate();
            return "Autor eliminado";
        }
    }
}
