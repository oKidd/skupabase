package me.felipe.skupabase.skript;

import ch.njol.skript.Skript;
import me.felipe.skupabase.skript.effects.EffSupabaseExecute;
import me.felipe.skupabase.skript.expressions.ExprSupabaseSubmitQuery;
import me.felipe.skupabase.skript.expressions.ExprSupabaseQueryResult;
import me.felipe.skupabase.skript.expressions.ExprSupabaseQueryStatus;
import me.felipe.skupabase.supabase.SupabaseService;
import org.bukkit.plugin.java.JavaPlugin;

public final class SkriptSyntaxRegistrar {
    private SkriptSyntaxRegistrar() {
    }

    public static void register(JavaPlugin plugin, SupabaseService supabaseService) {
        Skript.registerAddon(plugin);
        EffSupabaseExecute.setService(supabaseService);
        ExprSupabaseSubmitQuery.setService(supabaseService);
        ExprSupabaseQueryResult.setService(supabaseService);
        ExprSupabaseQueryStatus.setService(supabaseService);

        Skript.registerEffect(EffSupabaseExecute.class,
                "run supabase query %string%",
                "execute supabase query %string%");

        Skript.registerExpression(ExprSupabaseQueryResult.class, String.class, ch.njol.skript.lang.ExpressionType.SIMPLE,
                "supabase query result %string%");

        Skript.registerExpression(ExprSupabaseQueryStatus.class, String.class, ch.njol.skript.lang.ExpressionType.SIMPLE,
                "supabase query status %string%");

        Skript.registerExpression(ExprSupabaseSubmitQuery.class, String.class, ch.njol.skript.lang.ExpressionType.SIMPLE,
                "supabase query %string%");
    }
}
