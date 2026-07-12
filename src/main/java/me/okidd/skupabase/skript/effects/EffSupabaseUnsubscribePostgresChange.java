package me.okidd.skupabase.skript.effects;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Effect;
import ch.njol.util.Kleenean;
import me.okidd.skupabase.supabase.realtime.SupabaseRealtimeService;
import org.bukkit.event.Event;

public final class EffSupabaseUnsubscribePostgresChange extends Effect {
    private static volatile SupabaseRealtimeService service;
    private Expression<String> subscriptionId;

    public static void setService(SupabaseRealtimeService realtimeService) {
        service = realtimeService;
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.subscriptionId = (Expression<String>) expressions[0];
        return true;
    }

    @Override
    protected void execute(Event event) {
        SupabaseRealtimeService current = service;
        if (current == null) {
            return;
        }

        String id = subscriptionId.getSingle(event);
        if (id == null || id.isBlank()) {
            return;
        }

        current.unsubscribe(id.trim());
    }

    @Override
    public String toString(Event event, boolean debug) {
        return "unsubscribe postgres subscription";
    }

    @Override
    public String getSyntaxTypeName() {
        return "unsubscribe postgres subscription";
    }
}
