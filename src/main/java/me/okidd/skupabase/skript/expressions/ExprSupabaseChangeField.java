package me.okidd.skupabase.skript.expressions;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import me.okidd.skupabase.supabase.realtime.SupabasePostgresChangeEvent;
import org.bukkit.event.Event;

public final class ExprSupabaseChangeField extends SimpleExpression<String> {
    private enum Field {
        SCHEMA,
        TABLE,
        TYPE,
        PAYLOAD,
        NEW_RECORD,
        OLD_RECORD,
        SUBSCRIPTION_ID,
        CHANNEL,
        COMMIT_TIMESTAMP
    }

    private Field field;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.field = switch (matchedPattern) {
            case 0 -> Field.SCHEMA;
            case 1 -> Field.TABLE;
            case 2 -> Field.TYPE;
            case 3 -> Field.PAYLOAD;
            case 4 -> Field.NEW_RECORD;
            case 5 -> Field.OLD_RECORD;
            case 6 -> Field.SUBSCRIPTION_ID;
            case 7 -> Field.CHANNEL;
            case 8 -> Field.COMMIT_TIMESTAMP;
            default -> Field.PAYLOAD;
        };
        return true;
    }

    @Override
    protected String[] get(Event event) {
        if (!(event instanceof SupabasePostgresChangeEvent change)) {
            return new String[0];
        }

        String value = switch (field) {
            case SCHEMA -> change.getSchema();
            case TABLE -> change.getTable();
            case TYPE -> change.getEventType();
            case PAYLOAD -> change.getPayloadJson();
            case NEW_RECORD -> change.getNewRecordJson();
            case OLD_RECORD -> change.getOldRecordJson();
            case SUBSCRIPTION_ID -> change.getSubscriptionId();
            case CHANNEL -> change.getChannel();
            case COMMIT_TIMESTAMP -> change.getCommitTimestamp();
        };

        if (value == null) {
            return new String[0];
        }
        return new String[]{value};
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
        return "supabase change field";
    }

    @Override
    public String getSyntaxTypeName() {
        return "supabase change field";
    }
}
