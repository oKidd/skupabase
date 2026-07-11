package me.okidd.skupabase.skript.effects;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import me.okidd.skupabase.supabase.QueryJob;
import me.okidd.skupabase.supabase.SupabaseService;
import org.bukkit.event.Event;

public final class EffSupabaseExecute extends Effect {
    private static volatile SupabaseService service;
    private Expression<String> sql;

    public static void setService(SupabaseService supabaseService) {
        service = supabaseService;
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.sql = (Expression<String>) expressions[0];
        return true;
    }

    @Override
    protected void execute(Event event) {
        SupabaseService current = service;
        if (current == null) {
            return;
        }

        String query = sql.getSingle(event);
        if (query == null || query.isBlank()) {
            return;
        }

        current.submit(query);
    }

    @Override
    public String toString(Event event, boolean debug) {
        return "run supabase query " + sql.toString(event, debug);
    }

    @Override
    public String getSyntaxTypeName() {
        return "supabase query";
    }
}
