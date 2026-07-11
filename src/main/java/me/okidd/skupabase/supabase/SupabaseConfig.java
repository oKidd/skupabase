package me.okidd.skupabase.supabase;

import org.bukkit.configuration.file.FileConfiguration;

public record SupabaseConfig(
        String jdbcUrl,
        String username,
        String password,
        int connectTimeoutSeconds,
        int queryTimeoutSeconds,
        int maxResultRows
) {
    public static SupabaseConfig from(FileConfiguration config) {
        return new SupabaseConfig(
                config.getString("jdbc-url", "").trim(),
                config.getString("username", "").trim(),
                config.getString("password", ""),
                Math.max(1, config.getInt("connect-timeout-seconds", 10)),
                Math.max(1, config.getInt("query-timeout-seconds", 30)),
                Math.max(1, config.getInt("max-result-rows", 500))
        );
    }

    public boolean isConfigured() {
        return !jdbcUrl.isBlank() && !username.isBlank();
    }
}
