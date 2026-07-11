package me.okidd.skupabase.skript.expressions;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import me.okidd.skupabase.supabase.QueryJob;
import me.okidd.skupabase.supabase.SupabaseService;
import org.bukkit.event.Event;

public final class ExprSupabaseQueryResult extends SimpleExpression<String> {
    private static volatile SupabaseService service;
    private Expression<String> queryId;

    public static void setService(SupabaseService supabaseService) {
        service = supabaseService;
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.queryId = (Expression<String>) expressions[0];
        return true;
    }

    @Override
    protected String[] get(Event event) {
        SupabaseService current = service;
        if (current == null) {
            return new String[0];
        }

        String id = queryId.getSingle(event);
        QueryJob job = null;
        if (id != null && !id.isBlank()) {
            job = current.getJob(id);
        }
        if (job == null) {
            job = current.getLastJob();
        }
        if (job == null || !job.isDone() || job.result() == null) {
            return new String[0];
        }
        return new String[]{job.result()};
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
        return "supabase query result " + queryId.toString(event, debug);
    }

    @Override
    public String getSyntaxTypeName() {
        return "supabase query result";
    }
}
