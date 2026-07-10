package me.felipe.skupabase.supabase;

public final class QueryJob {
    public enum State {
        PENDING,
        RUNNING,
        SUCCESS,
        ERROR
    }

    private final String id;
    private final String sql;
    private volatile State state;
    private volatile String result;
    private volatile String error;
    private volatile long submittedAt;
    private volatile long completedAt;

    public QueryJob(String id, String sql) {
        this.id = id;
        this.sql = sql;
        this.state = State.PENDING;
        this.submittedAt = System.currentTimeMillis();
    }

    public String id() {
        return id;
    }

    public String sql() {
        return sql;
    }

    public State state() {
        return state;
    }

    public String result() {
        return result;
    }

    public String error() {
        return error;
    }

    public long submittedAt() {
        return submittedAt;
    }

    public long completedAt() {
        return completedAt;
    }

    public boolean isDone() {
        return state == State.SUCCESS || state == State.ERROR;
    }

    void markRunning() {
        this.state = State.RUNNING;
    }

    void markSuccess(String result) {
        this.state = State.SUCCESS;
        this.result = result;
        this.completedAt = System.currentTimeMillis();
    }

    void markError(String error) {
        this.state = State.ERROR;
        this.error = error;
        this.completedAt = System.currentTimeMillis();
    }
}
