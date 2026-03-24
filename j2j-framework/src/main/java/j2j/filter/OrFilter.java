package j2j.filter;

import com.fasterxml.jackson.databind.JsonNode;

public class OrFilter implements JsonFilter {

    private final JsonFilter left;
    private final JsonFilter right;

    public OrFilter(JsonFilter left, JsonFilter right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public boolean matches(JsonNode node) {
        return left.matches(node) || right.matches(node);
    }
}
