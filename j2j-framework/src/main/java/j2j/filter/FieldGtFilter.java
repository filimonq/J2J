package j2j.filter;

import com.fasterxml.jackson.databind.JsonNode;

public class FieldGtFilter implements JsonFilter {
    private final String field;
    private final double limit;

    public FieldGtFilter(String field, double limit) {
        this.field = field;
        this.limit = limit;
    }

    @Override
    public boolean matches(JsonNode node) {
        if (!node.has(field)) return false;

        JsonNode fieldNode = node.get(field);

        if (fieldNode.isNumber()) {
            return fieldNode.asDouble() > limit;
        }

        return false;
    }
}