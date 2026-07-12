package me.okidd.skupabase.skript.expressions;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import me.okidd.skupabase.supabase.realtime.SupabaseRealtimeService;
import me.okidd.skupabase.supabase.realtime.SupabaseRealtimeSubscription;
import org.bukkit.event.Event;

public final class ExprSupabaseSubscribePostgresChanges extends SimpleExpression<String> {
    private static volatile SupabaseRealtimeService service;
    private Expression<String> table;
    private Expression<String> eventType;

    public static void setService(SupabaseRealtimeService realtimeService) {
        service = realtimeService;
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.table = (Expression<String>) expressions[0];
        this.eventType = (Expression<String>) expressions[1];
        return true;
    }

    @Override
    protected String[] get(Event event) {
        SupabaseRealtimeService current = service;
        if (current == null) {
            return new String[0];
        }

        String tableValue = table.getSingle(event);
        String eventValue = eventType.getSingle(event);
        if (tableValue == null || tableValue.isBlank()) {
            return new String[0];
        }

        String[] schemaAndTable = parseTable(tableValue);
        SupabaseRealtimeSubscription subscription = current.subscribe(schemaAndTable[0], schemaAndTable[1], eventValue);
        if (subscription == null) {
            return new String[0];
        }
        return new String[]{subscription.id()};
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public String toString(Event event, boolean debug) {
        return "subscribe to postgres changes in table";
    }

    @Override
    public String getSyntaxTypeName() {
        return "supabase realtime subscription";
    }

    private String[] parseTable(String raw) {
        String trimmed = raw.trim();
        int dot = trimmed.indexOf('.');
        if (dot <= 0 || dot == trimmed.length() - 1) {
            return new String[]{"public", trimmed};
        }
        return new String[]{trimmed.substring(0, dot), trimmed.substring(dot + 1)};
    }
}
