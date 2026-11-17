package com.csnmbai.bot;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public final class Main {
    private static final int LONG_POLL_SECONDS = 50;

    private final String token;
    private final String apiBase;
    private final HttpClient http;
    private long nextUpdateId = 0;

    private Main(String token) {
        this.token = token;
        this.apiBase = "https://api.telegram.org/bot" + token + "/";
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    public static void main(String[] args) throws Exception {
        String token = System.getenv("TELEGRAM_BOT_TOKEN");
        if (token == null || token.isBlank()) {
            throw new IllegalStateException(
                    "Missing TELEGRAM_BOT_TOKEN environment variable. Set it in FadeHost.");
        }

        String username = System.getenv("TELEGRAM_BOT_USERNAME");
        System.out.println("Starting Telegram bot"
                + (username == null || username.isBlank() ? "" : " @" + username.replace("@", ""))
                + "...");

        Main bot = new Main(token.trim());
        bot.deleteWebhook();
        bot.runForever();
    }

    private void runForever() {
        System.out.println("Bot is running. Waiting for Telegram updates...");
        while (!Thread.currentThread().isInterrupted()) {
            try {
                JsonObject response = callGet("getUpdates",
                        "timeout=" + LONG_POLL_SECONDS + "&offset=" + nextUpdateId);
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
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private void handleUpdate(JsonObject update) {
        if (!update.has("message") || !update.get("message").isJsonObject()) {
            return;
        }

        JsonObject message = update.getAsJsonObject("message");
        if (!message.has("chat") || !message.get("chat").isJsonObject()) {
            return;
        }

        long chatId = message.getAsJsonObject("chat").get("id").getAsLong();
        String text = message.has("text") ? message.get("text").getAsString() : "";

        if (text.startsWith("/start")) {
            sendMessage(chatId, "你好！我是 @csnmbai。\n机器人已经启动成功。");
        } else if (text.startsWith("/help")) {
            sendMessage(chatId, "可用命令：\n/start - 启动机器人\n/help - 查看帮助");
        }
    }

    private void sendMessage(long chatId, String text) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("chat_id", chatId);
            body.addProperty("text", text);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiBase + "sendMessage"))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response =
                    http.send(request, HttpResponse.BodyHandlers.ofString());
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            if (!json.get("ok").getAsBoolean()) {
                System.err.println("sendMessage failed: " + response.body());
            }
        } catch (Exception e) {
            System.err.println("Could not send message: " + e.getMessage());
        }
    }

    private JsonObject callGet(String method, String query) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiBase + method + "?" + query))
                .timeout(Duration.ofSeconds(65))
                .GET()
                .build();

        HttpResponse<String> response =
                http.send(request, HttpResponse.BodyHandlers.ofString());
        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();

        if (response.statusCode() / 100 != 2 || !json.get("ok").getAsBoolean()) {
            throw new IllegalStateException("Telegram API error: " + response.body());
        }
        return json;
    }

    private void deleteWebhook() throws Exception {
        callGet("deleteWebhook", "drop_pending_updates=false");
    }
}
