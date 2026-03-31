package j2j.deserializer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import j2j.PersistenceManager;
import j2j.annotation.Reference;

import java.lang.reflect.Field;

public class J2JDeserializer {

    private final PersistenceManager manager;
    private final ObjectMapper mapper = new ObjectMapper();

    public J2JDeserializer(PersistenceManager manager) {
        this.manager = manager;
    }

    public Object createShallow(JsonNode node) {
        JsonNode typeNode = node.get("type");
        if (typeNode == null || typeNode.isNull()) {
            throw new J2JDeserializationException("Cannot deserialize JSON: missing 'type' field");
        }

        String type = typeNode.asText();
        Class<?> clazz;
        Object instance;

        try {
            clazz = Class.forName(type);
            instance = clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new J2JDeserializationException(
                    "Failed to instantiate class '" + type + "'. Ensure it has an empty constructor.", e
            );
        }

        try {
            for (Field field : clazz.getDeclaredFields()) {
                if (!field.isAnnotationPresent(j2j.annotation.Id.class)) continue;

                field.setAccessible(true);
                JsonNode idNode = node.get("id");
                if (idNode != null && !idNode.isNull()) {
                    Long idValue = idNode.asLong();
                    field.set(instance, idValue);
                }
            }

            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);

                if (field.isAnnotationPresent(j2j.annotation.Id.class)) continue;
                if (field.isAnnotationPresent(Reference.class)) continue;

                JsonNode valueNode = node.get(field.getName());
                if (valueNode == null || valueNode.isNull()) continue;

                Object value = deserializeValue(field.getType(), valueNode);
                field.set(instance, value);
            }
        } catch (Exception e) {
            throw new J2JDeserializationException(
                    "Error mapping fields for object of type '" + type + "'", e
            );
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

                Long refId = refNode.isLong() ? refNode.longValue() : refNode.asLong();
                Object refObj = manager.getById(refId);

                field.set(obj, refObj);

            } catch (Exception e) {
                throw new J2JDeserializationException(
                        "Failed to resolve @Reference '" + field.getName() + "' in class " + clazz.getSimpleName(), e
                );
            }
        }
    }

    private Object deserializeValue(Class<?> fieldType, JsonNode node) {
        if (fieldType == int.class || fieldType == Integer.class) {
            return node.asInt();
        }
        if (fieldType == long.class || fieldType == Long.class) {
            return node.asLong();
        }
        if (fieldType == double.class || fieldType == Double.class) {
            return node.asDouble();
        }
        if (fieldType == boolean.class || fieldType == Boolean.class) {
            return node.asBoolean();
        }
        if (fieldType == String.class) {
            return node.asText();
        }

        if (node.isObject()) {
            return createShallow(node);
        }

        throw new J2JDeserializationException("Unsupported JSON type for field " + fieldType.getSimpleName());
    }
}