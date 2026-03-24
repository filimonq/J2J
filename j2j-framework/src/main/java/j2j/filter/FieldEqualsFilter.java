package j2j.filter;

import com.fasterxml.jackson.databind.JsonNode;

public class FieldEqualsFilter implements JsonFilter {

    private final String field;
    private final String value;

    public FieldEqualsFilter(String field, String value) {
        this.field = field;
        this.value = value;
    }

    @Override
    public boolean matches(JsonNode node) {
        JsonNode fieldNode = node.get(field);
        if (fieldNode == null || fieldNode.isNull()) return false;

        return value.equals(fieldNode.asText());
    }
}