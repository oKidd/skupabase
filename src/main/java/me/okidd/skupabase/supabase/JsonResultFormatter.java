package me.okidd.skupabase.supabase;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

public final class JsonResultFormatter {
    private JsonResultFormatter() {
    }

    public static String toJson(ResultSet resultSet, int maxRows) throws SQLException {
        StringBuilder out = new StringBuilder();
        out.append("[");

        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();
        int rowCount = 0;
        boolean firstRow = true;

        while (resultSet.next()) {
            if (rowCount >= maxRows) {
                break;
            }

            if (!firstRow) {
                out.append(",");
            }
            firstRow = false;

            out.append("{");
            for (int column = 1; column <= columnCount; column++) {
                if (column > 1) {
                    out.append(",");
                }

                String label = metaData.getColumnLabel(column);
                Object value = resultSet.getObject(column);
                out.append("\"").append(escape(label)).append("\":");
                out.append(toJsonValue(value));
            }
            out.append("}");
            rowCount++;
        }

        out.append("]");
        return out.toString();
    }

    private static String toJsonValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        return "\"" + escape(String.valueOf(value)) + "\"";
    }

    private static String escape(String input) {
        StringBuilder out = new StringBuilder(input.length() + 16);
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }
}
