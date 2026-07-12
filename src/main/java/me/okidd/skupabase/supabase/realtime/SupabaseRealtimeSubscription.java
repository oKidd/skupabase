package me.okidd.skupabase.supabase.realtime;

import java.util.concurrent.atomic.AtomicBoolean;

public final class SupabaseRealtimeSubscription {
    private final String id;
    private final String topic;
    private final String schema;
    private final String table;
    private final String eventType;
    private volatile Integer remoteSubscriptionId;
    private final AtomicBoolean joined = new AtomicBoolean(false);

    public SupabaseRealtimeSubscription(String id, String topic, String schema, String table, String eventType) {
        this.id = id;
        this.topic = topic;
        this.schema = schema;
        this.table = table;
        this.eventType = eventType;
    }

    public String id() {
        return id;
    }

    public String topic() {
        return topic;
    }

    public String schema() {
        return schema;
    }

    public String table() {
        return table;
    }

    public String eventType() {
        return eventType;
    }

    public Integer remoteSubscriptionId() {
        return remoteSubscriptionId;
    }

    public void remoteSubscriptionId(Integer remoteSubscriptionId) {
        this.remoteSubscriptionId = remoteSubscriptionId;
    }

    public boolean isJoined() {
        return joined.get();
    }

    public void setJoined(boolean value) {
        joined.set(value);
    }
}
