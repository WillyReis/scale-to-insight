package com.sti.orders;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class OrdersServiceApplication {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Path RAW_ROOT = Paths.get(getEnv("RAW_ROOT", "/data/raw"));
    private static final Path ACCESS_LOG_FILE = RAW_ROOT.resolve("access/events.jsonl");
    private static final Path SALES_LOG_FILE = RAW_ROOT.resolve("sales/events.jsonl");

    private static final String CONNECTION_STRING = getEnv(
        "AZURE_STORAGE_CONNECTION_STRING",
        "DefaultEndpointsProtocol=http;" +
            "AccountName=devstoreaccount1;" +
            "AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;" +
            "BlobEndpoint=http://azurite:10000/devstoreaccount1;"
    );
    private static final String RAW_CONTAINER = getEnv("AZURE_RAW_CONTAINER", "raw");

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(5001), 0);

        // Health endpoint usado por monitoramento e smoke tests do pipeline.
        server.createContext("/health", exchange -> {
            logAccess(exchange);
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendJson(exchange, 405, Map.of("error", "method not allowed"));
                return;
            }
            sendJson(exchange, 200, Map.of("status", "ok", "service", "orders-service"));
        });

        // Endpoint de origem de dados de vendas (camada App -> Raw).
        server.createContext("/orders", exchange -> {
            logAccess(exchange);
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendJson(exchange, 405, Map.of("error", "method not allowed"));
                return;
            }

            Map<String, Object> payload;
            try {
                byte[] body = exchange.getRequestBody().readAllBytes();
                if (body.length == 0) {
                    payload = new HashMap<>();
                } else {
                    payload = MAPPER.readValue(body, Map.class);
                }
            } catch (Exception ex) {
                payload = new HashMap<>();
            }

            Map<String, Object> saleEvent = new HashMap<>();
            saleEvent.put("order_id", UUID.randomUUID().toString());
            saleEvent.put("created_at", ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));
            saleEvent.put("amount", toDouble(payload.getOrDefault("amount", 100.0)));
            saleEvent.put("payment_method", String.valueOf(payload.getOrDefault("payment_method", "credit_card")));
            saleEvent.put("status", String.valueOf(payload.getOrDefault("status", "approved")));

            appendJsonLine(SALES_LOG_FILE, saleEvent);
            boolean uploaded = uploadToAzureBlob(saleEvent);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "order created");
            response.put("order", saleEvent);
            response.put("azure_blob_uploaded", uploaded);
            sendJson(exchange, 201, response);
        });

        server.start();
    }

    private static void logAccess(HttpExchange exchange) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("timestamp", ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            event.put("method", exchange.getRequestMethod());
            event.put("path", exchange.getRequestURI().getPath());
            event.put("remote_addr", exchange.getRemoteAddress().toString());
            event.put("user_agent", exchange.getRequestHeaders().getFirst("User-Agent"));
            appendJsonLine(ACCESS_LOG_FILE, event);
        } catch (Exception ignored) {
            // Access log failures should not block request processing.
        }
    }

    private static void appendJsonLine(Path file, Map<String, Object> payload) throws IOException {
        Files.createDirectories(file.getParent());
        String line = MAPPER.writeValueAsString(payload) + "\n";
        Files.writeString(file, line, StandardCharsets.UTF_8,
            Files.exists(file) ? java.nio.file.StandardOpenOption.APPEND : java.nio.file.StandardOpenOption.CREATE);
    }

    private static boolean uploadToAzureBlob(Map<String, Object> saleEvent) {
        try {
            String createdAt = String.valueOf(saleEvent.get("created_at"));
            ZonedDateTime ts = ZonedDateTime.parse(createdAt);
            // Particiona por data para simular organizacao comum de data lake.
            String blobName = String.format(
                "sales/%04d/%02d/%02d/%s.json",
                ts.getYear(),
                ts.getMonthValue(),
                ts.getDayOfMonth(),
                saleEvent.get("order_id")
            );

            BlobServiceClient client = new BlobServiceClientBuilder()
                .connectionString(CONNECTION_STRING)
                .buildClient();

            BlobContainerClient containerClient = client.getBlobContainerClient(RAW_CONTAINER);
            if (!containerClient.exists()) {
                containerClient.create();
            }

            String content = MAPPER.writeValueAsString(saleEvent);
            containerClient.getBlobClient(blobName)
                .upload(new java.io.ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), content.length(), true);
            return true;
        } catch (Exception ex) {
            return false;
        }
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

    private static double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception ex) {
            return 100.0;
        }
    }
}
