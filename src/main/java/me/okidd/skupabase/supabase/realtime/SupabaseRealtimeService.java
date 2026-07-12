package me.okidd.skupabase.supabase.realtime;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.okidd.skupabase.supabase.SupabaseConfig;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class SupabaseRealtimeService {
    private final JavaPlugin plugin;
    private final SupabaseConfig config;
    private final Gson gson = new Gson();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final AtomicLong refCounter = new AtomicLong(1L);
    private final Map<String, SupabaseRealtimeSubscription> subscriptions = new ConcurrentHashMap<>();
    private volatile WebSocket webSocket;
    private volatile boolean shuttingDown;
    private volatile boolean reconnectScheduled;
    private volatile int heartbeatTaskId = -1;

    public SupabaseRealtimeService(JavaPlugin plugin, SupabaseConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public boolean isConfigured() {
        return config.isRealtimeConfigured();
    }

    public SupabaseRealtimeSubscription subscribe(String schema, String table, String eventType) {
        if (!isConfigured()) {
            plugin.getLogger().warning("Realtime is not configured. Set SUPABASE_URL and SUPABASE_SECRET_KEY in config.yml.");
            return null;
        }

        String normalizedSchema = schema == null || schema.isBlank() ? "public" : schema.trim();
        String normalizedTable = table == null ? "" : table.trim();
        String normalizedEvent = eventType == null || eventType.isBlank() ? "*" : eventType.trim().toUpperCase();

        if (normalizedTable.isBlank()) {
            return null;
        }

        String localId = UUID.randomUUID().toString();
        String topic = "realtime:skupabase:" + localId;
        SupabaseRealtimeSubscription subscription = new SupabaseRealtimeSubscription(localId, topic, normalizedSchema, normalizedTable, normalizedEvent);
        subscriptions.put(localId, subscription);

        plugin.getLogger().info("Preparing realtime subscription topic=" + topic + " table=" + normalizedSchema + "." + normalizedTable + " event=" + normalizedEvent);

        connectIfNeeded();
        if (webSocket != null) {
            join(subscription);
        }
        return subscription;
    }

    public boolean unsubscribe(String localId) {
        SupabaseRealtimeSubscription subscription = subscriptions.remove(localId);
        if (subscription == null) {
            return false;
        }

        WebSocket socket = webSocket;
        if (socket != null && !socket.isOutputClosed()) {
            sendFrame(socket, null, nextRef(), subscription.topic(), "phx_leave", new JsonObject());
        }
        return true;
    }

    public SupabaseRealtimeSubscription getSubscription(String localId) {
        return subscriptions.get(localId);
    }

    public Collection<SupabaseRealtimeSubscription> getSubscriptions() {
        return new ArrayList<>(subscriptions.values());
    }

    public void shutdown() {
        shuttingDown = true;
        stopHeartbeat();
        WebSocket socket = webSocket;
        webSocket = null;
        if (socket != null) {
            try {
                socket.sendClose(WebSocket.NORMAL_CLOSURE, "plugin shutdown");
            } catch (Exception ignored) {
            }
        }
    }

    private void connectIfNeeded() {
        if (shuttingDown) {
            return;
        }

        WebSocket socket = webSocket;
        if (socket != null && !socket.isInputClosed() && !socket.isOutputClosed()) {
            return;
        }

        synchronized (this) {
            socket = webSocket;
            if (socket != null && !socket.isInputClosed() && !socket.isOutputClosed()) {
                return;
            }

            String url = buildWebSocketUrl();
            if (url == null) {
                return;
            }

            reconnectScheduled = false;
            httpClient.newWebSocketBuilder()
                    .buildAsync(URI.create(url), new Listener())
                    .whenComplete((ws, throwable) -> {
                        if (throwable != null) {
                            plugin.getLogger().warning("Failed to connect to Supabase Realtime: " + throwable.getMessage());
                            scheduleReconnect();
                            return;
                        }

                        webSocket = ws;
                        plugin.getLogger().info("Connected to Supabase Realtime.");
                        startHeartbeat();
                        for (SupabaseRealtimeSubscription subscription : subscriptions.values()) {
                            join(subscription);
                        }
                    });
        }
    }

    private void join(SupabaseRealtimeSubscription subscription) {
        WebSocket socket = webSocket;
        if (socket == null || socket.isOutputClosed()) {
            return;
        }

        JsonObject configJson = new JsonObject();
        JsonObject broadcast = new JsonObject();
        broadcast.addProperty("ack", false);
        broadcast.addProperty("self", false);
        configJson.add("broadcast", broadcast);

        JsonObject presence = new JsonObject();
        presence.addProperty("enabled", false);
        configJson.add("presence", presence);

        JsonArray postgresChanges = new JsonArray();
        JsonObject change = new JsonObject();
        change.addProperty("event", subscription.eventType());
        change.addProperty("schema", subscription.schema());
        change.addProperty("table", subscription.table());
        postgresChanges.add(change);
        configJson.add("postgres_changes", postgresChanges);
        configJson.addProperty("private", false);

        JsonObject payload = new JsonObject();
        payload.add("config", configJson);

        plugin.getLogger().info("Sending realtime join topic=" + subscription.topic() + " table=" + subscription.schema() + "." + subscription.table() + " event=" + subscription.eventType());
        sendFrame(socket, null, nextRef(), subscription.topic(), "phx_join", payload);
    }

    private void handleFrame(String rawMessage) {
        try {
            JsonElement element = JsonParser.parseString(rawMessage);
            if (!element.isJsonArray()) {
                return;
            }

            JsonArray frame = element.getAsJsonArray();
            if (frame.size() < 5) {
                return;
            }

            String topic = getAsString(frame.get(2));
            String event = getAsString(frame.get(3));
            JsonElement payload = frame.get(4);

            if ("debug".equalsIgnoreCase(config.realtimeLogLevel())) {
                plugin.getLogger().info("Realtime frame received: " + event + " on " + topic + " -> " + payload);
            }

            if ("phx_reply".equals(event)) {
                handleReply(topic, payload);
                return;
            }

            if ("system".equals(event)) {
                handleSystem(topic, payload);
                return;
            }

            if ("postgres_changes".equals(event)) {
                handlePostgresChanges(topic, payload);
                return;
            }

            if ("phx_close".equals(event) || "phx_error".equals(event)) {
                plugin.getLogger().warning("Realtime channel closed: " + topic + " event=" + event);
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to parse realtime frame: " + ex.getMessage());
        }
    }

    private void handleReply(String topic, JsonElement payload) {
        if (!payload.isJsonObject()) {
            return;
        }

        JsonObject object = payload.getAsJsonObject();
        String status = getAsString(object.get("status"));
        JsonElement response = object.get("response");

        if (!"ok".equalsIgnoreCase(status) || response == null || !response.isJsonObject()) {
            if ("error".equalsIgnoreCase(status)) {
                plugin.getLogger().warning("Realtime subscription failed: " + object);
            }
            return;
        }

        JsonObject responseObject = response.getAsJsonObject();
        JsonElement postgresChanges = responseObject.get("postgres_changes");
        if (postgresChanges == null || !postgresChanges.isJsonArray() || postgresChanges.getAsJsonArray().isEmpty()) {
            return;
        }

        JsonObject first = postgresChanges.getAsJsonArray().get(0).getAsJsonObject();
        JsonElement remoteIdElement = first.get("id");
        if (remoteIdElement == null || !remoteIdElement.isJsonPrimitive()) {
            return;
        }

        int remoteId = remoteIdElement.getAsInt();
        SupabaseRealtimeSubscription subscription = findByTopic(topic);
        if (subscription == null) {
            return;
        }

        subscription.remoteSubscriptionId(remoteId);
        subscription.setJoined(true);
        plugin.getLogger().info("Realtime subscription ready for " + subscription.schema() + "." + subscription.table());
    }

    private void handleSystem(String topic, JsonElement payload) {
        if (!payload.isJsonObject()) {
            return;
        }

        JsonObject object = payload.getAsJsonObject();
        String status = getAsString(object.get("status"));
        String message = getAsString(object.get("message"));
        String extension = getAsString(object.get("extension"));

        if ("postgres_changes".equalsIgnoreCase(extension) && "ok".equalsIgnoreCase(status)) {
            plugin.getLogger().info("Realtime is watching " + topic);
        }

        if ("postgres_changes".equalsIgnoreCase(extension) && "ok".equalsIgnoreCase(status)) {
            SupabaseRealtimeSubscription subscription = findByTopic(topic);
            if (subscription != null) {
                subscription.setJoined(true);
                plugin.getLogger().info("Realtime subscription confirmed for " + subscription.schema() + "." + subscription.table());
            }
        }

        if ("error".equalsIgnoreCase(status)) {
            plugin.getLogger().warning("Realtime issue: " + message);
        }
    }

    private void handlePostgresChanges(String topic, JsonElement payload) {
        if (payload == null || !payload.isJsonObject()) {
            return;
        }

        SupabaseRealtimeSubscription subscription = findByTopic(topic);
        if (subscription == null) {
            return;
        }

        JsonObject object = payload.getAsJsonObject();
        JsonElement dataElement = object.get("data");
        if (dataElement == null || !dataElement.isJsonObject()) {
            return;
        }

        JsonObject data = dataElement.getAsJsonObject();
        String schema = getAsString(data.get("schema"));
        String table = getAsString(data.get("table"));
        String eventType = getAsString(data.get("type"));
        String commitTimestamp = getAsString(data.get("commit_timestamp"));
        String recordJson = toJson(data.get("record"));
        String oldRecordJson = toJson(data.get("old_record"));
        String payloadJson = gson.toJson(data);

        Bukkit.getPluginManager().callEvent(new SupabasePostgresChangeEvent(
                subscription.id(),
                topic,
                schema,
                table,
                eventType,
                commitTimestamp,
                payloadJson,
                recordJson,
                oldRecordJson
        ));
    }

    private SupabaseRealtimeSubscription findByTopic(String topic) {
        for (SupabaseRealtimeSubscription subscription : subscriptions.values()) {
            if (subscription.topic().equals(topic)) {
                return subscription;
            }
        }
        return null;
    }

    private String buildWebSocketUrl() {
        String base = config.realtimeUrl();
        String apiKey = config.realtimeApiKey();
        if (base.isBlank() || apiKey.isBlank()) {
            return null;
        }

        String separator = base.contains("?") ? "&" : "?";
        return base + separator
                + "apikey=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8)
                + "&vsn=2.0.0"
                + "&log_level=" + URLEncoder.encode(config.realtimeLogLevel(), StandardCharsets.UTF_8);
    }

    private void scheduleReconnect() {
        if (shuttingDown || reconnectScheduled) {
            return;
        }
        reconnectScheduled = true;
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            reconnectScheduled = false;
            connectIfNeeded();
        }, 20L * 5);
    }

    private void startHeartbeat() {
        if (heartbeatTaskId != -1) {
            return;
        }

        heartbeatTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            WebSocket socket = webSocket;
            if (socket == null || socket.isOutputClosed()) {
                return;
            }

            sendFrame(socket, null, nextRef(), "phoenix", "heartbeat", new JsonObject());
        }, 20L * config.realtimeHeartbeatSeconds(), 20L * config.realtimeHeartbeatSeconds()).getTaskId();
    }

    private void stopHeartbeat() {
        if (heartbeatTaskId == -1) {
            return;
        }

        Bukkit.getScheduler().cancelTask(heartbeatTaskId);
        heartbeatTaskId = -1;
    }

    private void sendFrame(WebSocket socket, String joinRef, String ref, String topic, String event, JsonElement payload) {
        JsonArray frame = new JsonArray();
        if (joinRef == null) {
            frame.add(JsonNull.INSTANCE);
        } else {
            frame.add(joinRef);
        }
        frame.add(ref);
        frame.add(topic);
        frame.add(event);
        frame.add(payload == null ? JsonNull.INSTANCE : payload);
        socket.sendText(gson.toJson(frame), true);
    }

    private String nextRef() {
        return String.valueOf(refCounter.getAndIncrement());
    }

    private String getAsString(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return "";
        }
        return element.getAsString();
    }

    private String toJson(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return "{}";
        }
        return gson.toJson(element);
    }

    private final class Listener implements WebSocket.Listener {
        private final StringBuilder messageBuffer = new StringBuilder();

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
            SupabaseRealtimeService.this.webSocket = webSocket;
            plugin.getLogger().info("Supabase Realtime websocket open: " + webSocket);
        }

        @Override
        public CompletableFuture<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            messageBuffer.append(data);
            if (last) {
                String message = messageBuffer.toString();
                messageBuffer.setLength(0);
                handleFrame(message);
            }
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            plugin.getLogger().warning("Supabase Realtime websocket closed: " + statusCode + " " + reason);
            SupabaseRealtimeService.this.webSocket = null;
            stopHeartbeat();
            scheduleReconnect();
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            plugin.getLogger().warning("Supabase Realtime websocket error: " + error.getMessage());
            SupabaseRealtimeService.this.webSocket = null;
            stopHeartbeat();
            scheduleReconnect();
        }
    }
}
