package j2j.filter;

import com.fasterxml.jackson.databind.JsonNode;

public class NotFilter implements JsonFilter {

    private final JsonFilter inner;

    public NotFilter(JsonFilter inner) {
        this.inner = inner;
    }

    @Override
    public boolean matches(JsonNode node) {
        return !inner.matches(node);
    }
}
