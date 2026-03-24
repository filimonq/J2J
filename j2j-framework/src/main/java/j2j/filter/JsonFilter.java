package j2j.filter;

import com.fasterxml.jackson.databind.JsonNode;

public interface JsonFilter {
    boolean matches(JsonNode node);
}