package me.okidd.skupabase.supabase;

import org.bukkit.configuration.file.FileConfiguration;

import java.net.URLEncoder;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

public record SupabaseConfig(
        String dbHost,
        String dbPort,
        String dbName,
        String dbUser,
        String dbPassword,
        String supabaseUrl,
        String secretKey,
        String legacyJdbcUrl,
        String legacyUsername,
        String legacyPassword,
        int connectTimeoutSeconds,
        int queryTimeoutSeconds,
        int maxResultRows,
        int realtimeHeartbeatSeconds,
        String realtimeLogLevel
) {
    public static SupabaseConfig from(FileConfiguration config) {
        return new SupabaseConfig(
                firstNonBlank(
                        config.getString("DB_HOST", ""),
                        config.getString("db-host", ""),
                        config.getString("host", "")
                ),
                firstNonBlank(
                        config.getString("DB_PORT", ""),
                        config.getString("db-port", ""),
                        config.getString("port", "")
                ),
                firstNonBlank(
                        config.getString("DB_NAME", ""),
                        config.getString("db-name", ""),
                        config.getString("database", "")
                ),
                firstNonBlank(
                        config.getString("DB_USER", ""),
                        config.getString("db-user", ""),
                        config.getString("user", "")
                ),
                firstNonBlank(
                        config.getString("DB_PASSWORD", ""),
                        config.getString("db-password", ""),
                        config.getString("password", ""),
                        config.getString("SUPABASE_DB_PASSWORD", ""),
                        config.getString("supabase-db-password", "")
                ),
                firstNonBlank(
                        config.getString("SUPABASE_URL", ""),
                        config.getString("supabase-url", ""),
                        config.getString("project-url", "")
                ),
                firstNonBlank(
                        config.getString("SUPABASE_SECRET_KEY", ""),
                        config.getString("supabase-secret-key", ""),
                        config.getString("realtime-api-key", ""),
                        config.getString("SUPABASE_PUBLISHABLE_KEY", ""),
                        config.getString("supabase-publishable-key", ""),
                        config.getString("anon-key", ""),
                        config.getString("service-role-key", "")
                ),
                config.getString("jdbc-url", "").trim(),
                config.getString("username", "").trim(),
                config.getString("password", ""),
                Math.max(1, config.getInt("connect-timeout-seconds", 10)),
                Math.max(1, config.getInt("query-timeout-seconds", 30)),
                Math.max(1, config.getInt("max-result-rows", 500)),
                Math.max(5, config.getInt("realtime-heartbeat-seconds", 25)),
                config.getString("realtime-log-level", "info").trim()
        );
    }

    public boolean isConfigured() {
        return !jdbcUrl().isBlank() && !username().isBlank() && !password().isBlank();
    }

    public boolean isRealtimeConfigured() {
        return !realtimeUrl().isBlank() && !realtimeApiKey().isBlank();
    }

    public String jdbcUrl() {
        if (!legacyJdbcUrl.isBlank()) {
            return legacyJdbcUrl;
        }

        if (dbHost.isBlank() || dbPort.isBlank() || dbName.isBlank()) {
            return "";
        }

        String user = !dbUser.isBlank() ? dbUser : legacyUsername;
        String password = !dbPassword.isBlank() ? dbPassword : legacyPassword;

        StringBuilder url = new StringBuilder("jdbc:postgresql://")
                .append(dbHost)
                .append(':')
                .append(dbPort)
                .append('/')
                .append(dbName)
                .append("?sslmode=require");

        if (!user.isBlank()) {
            url.append("&user=").append(encodeJdbcValue(user));
        }

        if (!password.isBlank()) {
            url.append("&password=").append(encodeJdbcValue(password));
        }

        return url.toString();
    }

    public String username() {
        if (!dbUser.isBlank()) {
            return dbUser;
        }

        return legacyUsername;
    }

    public String password() {
        if (!dbPassword.isBlank()) {
            return dbPassword;
        }
        return legacyPassword;
    }

    public String realtimeUrl() {
        String ref = projectRef();
        if (ref.isBlank()) {
            return "";
        }

        return "wss://" + ref + ".supabase.co/realtime/v1/websocket";
    }

    public String realtimeApiKey() {
        return secretKey;
    }

    public String projectRef() {
        String ref = extractProjectRef(supabaseUrl);
        if (!ref.isBlank()) {
            return ref;
        }

        ref = extractProjectRef(legacyJdbcUrl);
        if (!ref.isBlank()) {
            return ref;
        }

        if (!legacyUsername.isBlank()) {
            int dot = legacyUsername.indexOf('.');
            if (dot > 0 && dot < legacyUsername.length() - 1) {
                return legacyUsername.substring(dot + 1).trim();
            }
        }

        return "";
    }

    private static String extractProjectRef(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        String trimmed = input.trim();
        String host = trimmed;

        try {
            if (trimmed.contains("://")) {
                URI uri = new URI(trimmed);
                host = uri.getHost() == null ? "" : uri.getHost();
            }
        } catch (URISyntaxException ignored) {
        }

        if (host.startsWith("db.")) {
            host = host.substring("db.".length());
        }

        if (host.startsWith("aws-") && host.contains(".pooler.supabase.com")) {
            return "";
        }

        if (host.endsWith(".supabase.co")) {
            return host.substring(0, host.length() - ".supabase.co".length());
        }

        if (host.endsWith(".supabase.com")) {
            return host.substring(0, host.length() - ".supabase.com".length());
        }

        return "";
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private static String encodeJdbcValue(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
