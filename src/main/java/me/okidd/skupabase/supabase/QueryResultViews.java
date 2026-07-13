package me.okidd.skupabase.supabase;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.util.Map;

public final class QueryResultViews {
    private QueryResultViews() {
    }

    public static String toSingleRowJson(String json) {
        JsonElement element = parse(json);
        if (element == null || !element.isJsonArray()) {
            return json;
        }

        JsonArray rows = element.getAsJsonArray();
        if (rows.size() == 1) {
            JsonElement onlyRow = rows.get(0);
            if (onlyRow != null && onlyRow.isJsonObject()) {
                return onlyRow.toString();
            }
        }

        return json;
    }

    public static String toSingleValue(String json) {
        JsonElement element = parse(json);
        if (element == null) {
            return null;
        }

        return extractSingleValue(element);
    }

    private static JsonElement parse(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }

        try {
            return JsonParser.parseString(json);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String extractSingleValue(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }

        if (element.isJsonPrimitive()) {
            return primitiveToString(element.getAsJsonPrimitive());
        }

        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            if (array.isEmpty()) {
                return null;
            }
            return extractSingleValue(array.get(0));
        }

        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                return extractSingleValue(entry.getValue());
            }
            return null;
        }

        return element.toString();
    }

    private static String primitiveToString(JsonPrimitive primitive) {
        if (primitive.isString()) {
            return primitive.getAsString();
        }
        if (primitive.isBoolean()) {
            return Boolean.toString(primitive.getAsBoolean());
        }
        if (primitive.isNumber()) {
            return primitive.getAsNumber().toString();
        }
        return primitive.getAsString();
    }
}
