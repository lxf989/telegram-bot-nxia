package com.csnmbai.bot;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class Main {
    private static final int LONG_POLL_SECONDS = 50;

    private final String apiBase;
    private final HttpClient http;
    private long nextUpdateId = 0;

    private Main(String token) {
        apiBase = "https://api.telegram.org/bot" + token + "/";
        http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    public static void main(String[] args) throws Exception {
        String token = System.getenv("TELEGRAM_BOT_TOKEN");

        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Missing TELEGRAM_BOT_TOKEN");
        }

        Main bot = new Main(token.trim());
        bot.callGet("deleteWebhook", "drop_pending_updates=false");

        System.out.println("CSNMBAI bot started!");

        bot.runForever();
    }

    private void runForever() {
        while (true) {
            try {
                JsonObject response = callGet(
                        "getUpdates",
                        "timeout=" + LONG_POLL_SECONDS + "&offset=" + nextUpdateId
                );

                JsonArray updates = response.getAsJsonArray("result");

                for (JsonElement element : updates) {
                    JsonObject update = element.getAsJsonObject();
                    nextUpdateId = update.get("update_id").getAsLong() + 1;
                    handleUpdate(update);
                }

            } catch (Exception e) {
                System.err.println("Polling error: " + e.getMessage());

                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private void handleUpdate(JsonObject update) {
        if (!update.has("message")) return;

        JsonObject message = update.getAsJsonObject("message");

        if (!message.has("text") || !message.has("chat")) return;

        long chatId = message.getAsJsonObject("chat")
                .get("id").getAsLong();

        String text = message.get("text").getAsString();

        if (text.startsWith("/start")) {
            sendMessage(chatId,
                    "你好！我是 @csnmbai。\n机器人已经启动成功。");
        } else if (text.startsWith("/help")) {
            sendMessage(chatId,
                    "可用命令：\n/start - 启动机器人\n/help - 查看帮助");
        }
    }

    private void sendMessage(long chatId, String text) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("chat_id", chatId);
            body.addProperty("text", text);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiBase + "sendMessage"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            http.send(request, HttpResponse.BodyHandlers.ofString());

        } catch (Exception e) {
            System.err.println("Send error: " + e.getMessage());
        }
    }

    private JsonObject callGet(String method, String query)
            throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiBase + method + "?" + query))
                .timeout(Duration.ofSeconds(65))
                .GET()
                .build();

        HttpResponse<String> response = http.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        JsonObject json = JsonParser.parseString(
                response.body()
        ).getAsJsonObject();

        if (!json.get("ok").getAsBoolean()) {
            throw new IllegalStateException(response.body());
        }

        return json;
    }
}
