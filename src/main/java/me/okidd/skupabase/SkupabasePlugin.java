package me.okidd.skupabase;

import ch.njol.skript.Skript;
import org.bukkit.plugin.java.JavaPlugin;
import me.okidd.skupabase.skript.SkriptSyntaxRegistrar;
import me.okidd.skupabase.supabase.SupabaseConfig;
import me.okidd.skupabase.supabase.SupabaseService;

public final class SkupabasePlugin extends JavaPlugin {
    private SupabaseService supabaseService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        SupabaseConfig config = SupabaseConfig.from(getConfig());
        this.supabaseService = new SupabaseService(this, config);

        if (getServer().getPluginManager().getPlugin("Skript") != null) {
            SkriptSyntaxRegistrar.register(this, supabaseService);
            getLogger().info("Skript detected, Skupabase syntax registered.");
        } else {
            getLogger().warning("Skript is not installed or not enabled yet. Skupabase syntax was not registered.");
        }
    }

    @Override
    public void onDisable() {
        if (supabaseService != null) {
            supabaseService.shutdown();
        }
    }
}
