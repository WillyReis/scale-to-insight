package com.sti.finance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FinanceServiceApplication {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Path WAREHOUSE_DB = Paths.get(getEnv("WAREHOUSE_DB", "/data/warehouse/warehouse.db"));

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(5002), 0);

        // Endpoint de saude para validar disponibilidade do servico.
        server.createContext("/health", exchange -> {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendJson(exchange, 405, Map.of("error", "method not allowed"));
                return;
            }
            sendJson(exchange, 200, Map.of("status", "ok", "service", "finance-service"));
        });

        // Endpoint que expõe o Data Mart para o setor financeiro.
        server.createContext("/kpis", exchange -> {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendJson(exchange, 405, Map.of("error", "method not allowed"));
                return;
            }
            sendJson(exchange, 200, collectKpis());
        });

        server.start();
    }

    private static Map<String, Object> collectKpis() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("total_sales", 0.0);
        summary.put("total_orders", 0);
        summary.put("avg_ticket", 0.0);

        List<Map<String, Object>> byDay = new ArrayList<>();

        if (!Files.exists(WAREHOUSE_DB)) {
            return Map.of("summary", summary, "by_day", byDay);
        }

        String dbUrl = "jdbc:sqlite:" + WAREHOUSE_DB;
        try (Connection conn = DriverManager.getConnection(dbUrl); Statement stmt = conn.createStatement()) {
            ResultSet totals = stmt.executeQuery(
                "SELECT COALESCE(SUM(total_sales), 0), COALESCE(SUM(total_orders), 0), COALESCE(AVG(avg_ticket), 0) " +
                    "FROM dm_sales_performance"
            );
            if (totals.next()) {
                summary.put("total_sales", round2(totals.getDouble(1)));
                summary.put("total_orders", totals.getInt(2));
                summary.put("avg_ticket", round2(totals.getDouble(3)));
            }

            ResultSet rows = stmt.executeQuery(
                "SELECT sale_date, total_sales, total_orders, avg_ticket " +
                    "FROM dm_sales_performance ORDER BY sale_date DESC LIMIT 7"
            );
            while (rows.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("sale_date", rows.getString(1));
                row.put("total_sales", round2(rows.getDouble(2)));
                row.put("total_orders", rows.getInt(3));
                row.put("avg_ticket", round2(rows.getDouble(4)));
                byDay.add(row);
            }
        } catch (Exception ignored) {
            return Map.of("summary", summary, "by_day", byDay);
        }

        return Map.of("summary", summary, "by_day", byDay);
    }

    private static void sendJson(HttpExchange exchange, int statusCode, Map<String, Object> payload) throws IOException {
        byte[] response = MAPPER.writeValueAsBytes(payload);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    private static String getEnv(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
