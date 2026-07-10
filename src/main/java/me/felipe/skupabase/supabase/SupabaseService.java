package me.felipe.skupabase.supabase;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public final class SupabaseService {
    private final JavaPlugin plugin;
    private final SupabaseConfig config;
    private final ExecutorService executor;
    private final Map<String, QueryJob> jobs = new ConcurrentHashMap<>();

    public SupabaseService(JavaPlugin plugin, SupabaseConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.executor = Executors.newFixedThreadPool(2, new SupabaseThreadFactory());

        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            plugin.getLogger().severe("PostgreSQL driver was not found inside the plugin jar.");
        }
    }

    public SupabaseConfig config() {
        return config;
    }

    public boolean isReady() {
        return config.isConfigured();
    }

    public QueryJob submit(String sql) {
        String id = UUID.randomUUID().toString();
        QueryJob job = new QueryJob(id, sql);
        jobs.put(id, job);

        executor.submit(() -> runJob(job));
        return job;
    }

    public QueryJob getJob(String id) {
        return jobs.get(id);
    }

    public void shutdown() {
        executor.shutdownNow();
        try {
            if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("Supabase executor did not stop cleanly.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void runJob(QueryJob job) {
        job.markRunning();

        if (!config.isConfigured()) {
            job.markError("Supabase JDBC is not configured. Set jdbc-url, username and password in config.yml.");
            return;
        }

        try (Connection connection = DriverManager.getConnection(config.jdbcUrl(), config.username(), config.password());
             Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(config.queryTimeoutSeconds());
            boolean hasResultSet = statement.execute(job.sql());

            if (hasResultSet) {
                try (ResultSet resultSet = statement.getResultSet()) {
                    job.markSuccess(JsonResultFormatter.toJson(resultSet, config.maxResultRows()));
                }
            } else {
                int updateCount = statement.getUpdateCount();
                job.markSuccess("{\"updateCount\":" + updateCount + "}");
            }
        } catch (Exception e) {
            job.markError(e.getClass().getSimpleName() + ": " + e.getMessage());
            Bukkit.getLogger().warning("[Skupabase] Query failed: " + e.getMessage());
        }
    }

    private static final class SupabaseThreadFactory implements ThreadFactory {
        private int count = 0;

        @Override
        public synchronized Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "Skupabase-" + (++count));
            thread.setDaemon(true);
            return thread;
        }
    }
}
