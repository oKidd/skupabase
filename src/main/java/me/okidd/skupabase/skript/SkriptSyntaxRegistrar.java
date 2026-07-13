package me.okidd.skupabase.skript;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.ExpressionType;
import me.okidd.skupabase.skript.effects.EffSupabaseExecute;
import me.okidd.skupabase.skript.effects.EffSupabaseUnsubscribePostgresChange;
import me.okidd.skupabase.skript.events.EvtSupabasePostgresChange;
import me.okidd.skupabase.skript.expressions.ExprSupabaseChangeField;
import me.okidd.skupabase.skript.expressions.ExprSupabaseSubscribePostgresChanges;
import me.okidd.skupabase.skript.expressions.ExprSupabaseSubmitQuery;
import me.okidd.skupabase.skript.expressions.ExprSupabaseQueryResult;
import me.okidd.skupabase.skript.expressions.ExprSupabaseQueryStatus;
import me.okidd.skupabase.skript.sections.SecSupabaseAwait;
import me.okidd.skupabase.supabase.SupabaseService;
import me.okidd.skupabase.supabase.realtime.SupabasePostgresChangeEvent;
import me.okidd.skupabase.supabase.realtime.SupabaseRealtimeService;
import org.bukkit.plugin.java.JavaPlugin;

public final class SkriptSyntaxRegistrar {
    private SkriptSyntaxRegistrar() {
    }

    public static void register(JavaPlugin plugin, SupabaseService supabaseService, SupabaseRealtimeService realtimeService) {
        Skript.registerAddon(plugin);
        EffSupabaseExecute.setService(supabaseService);
        ExprSupabaseSubmitQuery.setService(supabaseService);
        ExprSupabaseQueryResult.setService(supabaseService);
        ExprSupabaseQueryStatus.setService(supabaseService);
        SecSupabaseAwait.setService(supabaseService);
        ExprSupabaseSubscribePostgresChanges.setService(realtimeService);
        EffSupabaseUnsubscribePostgresChange.setService(realtimeService);

        Skript.registerEffect(EffSupabaseExecute.class,
                "run supabase query %string%",
                "execute supabase query %string%");

        Skript.registerExpression(ExprSupabaseQueryResult.class, String.class, ExpressionType.SIMPLE,
                "supabase query result %string%",
                "supabase single query result %string%");

        Skript.registerExpression(ExprSupabaseQueryStatus.class, String.class, ExpressionType.SIMPLE,
                "supabase query status %string%");

        Skript.registerExpression(ExprSupabaseSubmitQuery.class, String.class, ExpressionType.SIMPLE,
                "supabase query %string%");

        Skript.registerExpression(ExprSupabaseSubscribePostgresChanges.class, String.class, ExpressionType.SIMPLE,
                "subscribe to postgres changes in table %string% with event %string%");

        Skript.registerExpression(ExprSupabaseChangeField.class, String.class, ExpressionType.SIMPLE,
                "supabase change schema",
                "supabase change table",
                "supabase change type",
                "supabase change payload",
                "supabase change record",
                "supabase change old record",
                "supabase change subscription id",
                "supabase change channel",
                "supabase change commit timestamp");

        Skript.registerEffect(EffSupabaseUnsubscribePostgresChange.class,
                "unsubscribe postgres subscription %string%");

        Skript.registerEvent("Supabase postgres change", EvtSupabasePostgresChange.class, SupabasePostgresChangeEvent.class,
                "supabase postgres change");

        Skript.registerSection(SecSupabaseAwait.class,
                "await last supabase query",
                "await %string% supabase query");
    }
}
