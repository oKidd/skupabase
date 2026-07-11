package me.okidd.skupabase.skript.expressions;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import me.okidd.skupabase.supabase.QueryJob;
import me.okidd.skupabase.supabase.SupabaseService;
import org.bukkit.event.Event;

public final class ExprSupabaseSubmitQuery extends SimpleExpression<String> {
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
    protected String[] get(Event event) {
        SupabaseService current = service;
        if (current == null) {
            return new String[0];
        }

        String query = sql.getSingle(event);
        if (query == null || query.isBlank()) {
            return new String[0];
        }

        QueryJob job = current.submit(query);
        return new String[]{job.id()};
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
        return "supabase query " + sql.toString(event, debug);
    }

    @Override
    public String getSyntaxTypeName() {
        return "supabase query";
    }
}
