package me.okidd.skupabase.skript.events;

import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import me.okidd.skupabase.supabase.realtime.SupabasePostgresChangeEvent;
import org.bukkit.event.Event;

public final class EvtSupabasePostgresChange extends SkriptEvent {
    @Override
    public boolean init(Literal<?>[] expressions, int matchedPattern, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    public boolean check(Event event) {
        return event instanceof SupabasePostgresChangeEvent;
    }

    @Override
    public String toString(Event event, boolean debug) {
        return "supabase postgres change";
    }
}
