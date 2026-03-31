package j2j.filter;

import com.fasterxml.jackson.databind.JsonNode;
import j2j.PersistenceManager;
import j2j.annotation.Reference;

import java.lang.reflect.Field;

public class PathEqualsFilter implements JsonFilter {

    private final String[] path;
    private final String expectedValue;
    private final PersistenceManager manager;

    public PathEqualsFilter(String path, String expectedValue, PersistenceManager manager) {
        this.path = path.split("\\.");
        this.expectedValue = expectedValue;
        this.manager = manager;
    }

    @Override
    public boolean matches(JsonNode node) {
        try {
            JsonNode currentNode = node;
            Class<?> currentClass = resolveClass(node);

            for (int i = 0; i < path.length; i++) {
                String fieldName = path[i];

                Field field = currentClass.getDeclaredField(fieldName);
                field.setAccessible(true);

                // 🔥 если reference
                if (field.isAnnotationPresent(Reference.class)) {
                    String refKey = fieldName + "Id";
                    JsonNode refNode = currentNode.get(refKey);

                    if (refNode == null || refNode.isNull()) return false;

                    Long refId = refNode.asLong();

                    currentNode = manager.findNodeById(refId);
                    currentClass = resolveClass(currentNode);

                } else {
                    // обычное поле
                    currentNode = currentNode.get(fieldName);
                    if (currentNode == null) return false;
                }
            }

            return expectedValue.equals(currentNode.asText());

        } catch (Exception e) {
            return false;
        }
    }

    private Class<?> resolveClass(JsonNode node) throws ClassNotFoundException {
        String type = node.get("type").asText();
        return Class.forName("j2j.model." + type);
    }
}