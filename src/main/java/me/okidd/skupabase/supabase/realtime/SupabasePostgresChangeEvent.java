package me.okidd.skupabase.supabase.realtime;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class SupabasePostgresChangeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final String subscriptionId;
    private final String channel;
    private final String schema;
    private final String table;
    private final String eventType;
    private final String commitTimestamp;
    private final String payloadJson;
    private final String newRecordJson;
    private final String oldRecordJson;

    public SupabasePostgresChangeEvent(
            String subscriptionId,
            String channel,
            String schema,
            String table,
            String eventType,
            String commitTimestamp,
            String payloadJson,
            String newRecordJson,
            String oldRecordJson
    ) {
        super(true);
        this.subscriptionId = subscriptionId;
        this.channel = channel;
        this.schema = schema;
        this.table = table;
        this.eventType = eventType;
        this.commitTimestamp = commitTimestamp;
        this.payloadJson = payloadJson;
        this.newRecordJson = newRecordJson;
        this.oldRecordJson = oldRecordJson;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public String getChannel() {
        return channel;
    }

    public String getSchema() {
        return schema;
    }

    public String getTable() {
        return table;
    }

    public String getEventType() {
        return eventType;
    }

    public String getCommitTimestamp() {
        return commitTimestamp;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public String getNewRecordJson() {
        return newRecordJson;
    }

    public String getOldRecordJson() {
        return oldRecordJson;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
