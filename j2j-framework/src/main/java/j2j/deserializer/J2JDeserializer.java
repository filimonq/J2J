package j2j.deserializer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import j2j.PersistenceManager;
import j2j.annotation.Reference;

import java.lang.reflect.Field;

public class J2JDeserializer {

    private final PersistenceManager manager;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String basePackage = "j2j.model";

    public J2JDeserializer(PersistenceManager manager) {
        this.manager = manager;
    }

    public Object createShallow(JsonNode node) throws Exception {
        String type = node.get("type").asText();
        Class<?> clazz = Class.forName(basePackage + "." + type);

        Object instance = clazz.getDeclaredConstructor().newInstance();

        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);

            if (field.isAnnotationPresent(Reference.class)) {
                continue;
            }

            JsonNode valueNode = node.get(field.getName());
            if (valueNode == null || valueNode.isNull()) continue;

            Object value = deserializeValue(field.getType(), valueNode);
            field.set(instance, value);
        }

        return instance;
    }

    public void resolveReferences(Object obj, JsonNode node) {
        Class<?> clazz = obj.getClass();

        for (Field field : clazz.getDeclaredFields()) {
            if (!field.isAnnotationPresent(Reference.class)) continue;

            field.setAccessible(true);

            try {
                String refKey = field.getName() + "Id";
                JsonNode refNode = node.get(refKey);

                if (refNode == null || refNode.isNull()) continue;

                Long refId = refNode.longValue();
                Object refObj = manager.getById(refId);

                field.set(obj, refObj);

            } catch (Exception e) {
                throw new RuntimeException(
                        "Failed to resolve reference: " + field.getName(), e
                );
            }
        }
    }

    private Object deserializeValue(Class<?> fieldType, JsonNode node) throws Exception {

        if (node.isTextual()) return node.asText();
        if (node.isInt()) return node.asInt();
        if (node.isLong()) return node.asLong();
        if (node.isDouble()) return node.asDouble();
        if (node.isBoolean()) return node.asBoolean();

        if (node.isObject()) {
            return createShallow(node);
        }

        throw new RuntimeException("Unsupported type: " + node);
    }
}