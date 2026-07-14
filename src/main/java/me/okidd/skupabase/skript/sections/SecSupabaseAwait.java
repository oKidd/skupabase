package me.okidd.skupabase.skript.sections;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.EffectSection;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.config.SectionNode;
import ch.njol.util.Kleenean;
import ch.njol.skript.effects.Delay;
import me.okidd.skupabase.supabase.QueryJob;
import me.okidd.skupabase.supabase.SupabaseService;
import ch.njol.skript.variables.Variables;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;

import java.util.List;

public final class SecSupabaseAwait extends EffectSection {
    private static volatile SupabaseService service;

    private Expression<String> queryId;
    private boolean awaitLast;

    public static void setService(SupabaseService supabaseService) {
        service = supabaseService;
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult, SectionNode sectionNode, List<TriggerItem> triggerItems) {
        this.awaitLast = matchedPattern == 0;
        if (!this.awaitLast) {
            this.queryId = (Expression<String>) expressions[0];
        }

        if (sectionNode != null) {
            loadCode(sectionNode);
        }
        return true;
    }

    @Override
    protected TriggerItem walk(Event event) {
        SupabaseService current = service;
        if (current == null) {
            return hasSection() ? walk(event, false) : getNext();
        }

        QueryJob job = resolveJob(event, current);
        if (job == null) {
            return hasSection() ? walk(event, false) : getNext();
        }

        if (job.isDone()) {
            return hasSection() ? walk(event, true) : getNext();
        }

        Delay.addDelayedEvent(event);
        Object locals = Variables.removeLocals(event);
        job.completion().thenAccept(completed -> Bukkit.getScheduler().runTask(current.plugin(), () -> {
            Variables.setLocalVariables(event, locals);
            try {
                TriggerItem.walk(walk(event, true), event);
            } finally {
                Variables.removeLocals(event);
            }
        }));
        return null;
    }

    @Override
    public String toString(Event event, boolean debug) {
        if (awaitLast) {
            return "await last supabase query";
        }
        return "await " + (queryId == null ? "<none>" : queryId.toString(event, debug)) + " supabase query";
    }

    @Override
    public String getSyntaxTypeName() {
        return "await supabase query";
    }

    private QueryJob resolveJob(Event event, SupabaseService current) {
        if (awaitLast) {
            return current.getLastJob();
        }

        if (queryId == null) {
            return null;
        }

        String id = queryId.getSingle(event);
        if (id == null || id.isBlank()) {
            return null;
        }
        return current.getJob(id);
    }
}
