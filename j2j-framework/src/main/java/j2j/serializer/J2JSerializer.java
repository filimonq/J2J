package j2j.serializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import j2j.annotation.Id;
import j2j.annotation.Persistent;
import j2j.annotation.Reference;

import java.lang.reflect.Field;

/**
 * Serializes a @Persistent object into a single JSON line (JSONL format).
 *
 * Now supports:
 *   - Embedded objects (default)
 *   - References via @Reference → stored as fieldNameId
 */
public class J2JSerializer {

    private final ObjectMapper mapper = new ObjectMapper();

    public String serialize(Object obj) {
        if (obj == null) {
            throw new J2JSerializationException("Cannot serialize null");
        }

        Class<?> clazz = obj.getClass();

        if (!clazz.isAnnotationPresent(Persistent.class)) {
            throw new J2JSerializationException(
                    "Class " + clazz.getName() + " is not annotated with @Persistent"
            );
        }

        Field idField = resolveIdField(clazz);

        try {
            ObjectNode node = mapper.createObjectNode();
            node.put("type", clazz.getSimpleName());

            idField.setAccessible(true);
            Long idValue = (Long) idField.get(obj);

            if (idValue == null) {
                node.putNull("id");
            } else {
                node.put("id", idValue);
            }

            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(Id.class)) continue;

                field.setAccessible(true);
                Object value = field.get(obj);

                if (field.isAnnotationPresent(Reference.class)) {

                    String refKey = field.getName() + "Id";

                    if (value == null) {
                        node.putNull(refKey);
                    } else {
                        Long refId = extractId(value);
                        node.put(refKey, refId);
                    }

                } else {
                    putValue(node, field.getName(), value);
                }
            }

            return mapper.writeValueAsString(node);

        } catch (J2JSerializationException e) {
            throw e;
        } catch (Exception e) {
            throw new J2JSerializationException(
                    "Failed to serialize " + clazz.getSimpleName(), e
            );
        }
    }

    private Field resolveIdField(Class<?> clazz) {
        Field found = null;

        for (Field field : clazz.getDeclaredFields()) {
            if (!field.isAnnotationPresent(Id.class)) continue;

            if (found != null) {
                throw new J2JSerializationException(
                        clazz.getSimpleName() + " has more than one @Id field"
                );
            }

            if (!field.getType().equals(Long.class)) {
                throw new J2JSerializationException(
                        "@Id field '" + field.getName() + "' in " + clazz.getSimpleName()
                                + " must be Long, got: " + field.getType().getSimpleName()
                );
            }

            found = field;
        }

        if (found == null) {
            throw new J2JSerializationException(
                    clazz.getSimpleName() + " has no @Id field — add a Long field annotated with @Id"
            );
        }

        return found;
    }

    private ObjectNode serializeObject(Object obj) {
        ObjectNode node = mapper.createObjectNode();
        Class<?> clazz = obj.getClass();

        node.put("type", clazz.getSimpleName());

        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) continue;

            field.setAccessible(true);
            try {
                putValue(node, field.getName(), field.get(obj));
            } catch (IllegalAccessException e) {
                throw new J2JSerializationException("Failed to serialize nested object", e);
            }
        }

        return node;
    }

    private void putValue(ObjectNode node, String key, Object value) {
        if (value == null) {
            node.putNull(key);
            return;
        }

        if (value instanceof String s) {
            node.put(key, s);
        } else if (value instanceof Integer i) {
            node.put(key, i);
        } else if (value instanceof Long l) {
            node.put(key, l);
        } else if (value instanceof Double d) {
            node.put(key, d);
        } else if (value instanceof Float f) {
            node.put(key, f);
        } else if (value instanceof Boolean b) {
            node.put(key, b);
        } else {
            // 🔁 вложенный объект
            node.set(key, serializeObject(value));
        }
    }
    
    private Long extractId(Object obj) {
        for (Field field : obj.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                field.setAccessible(true);
                try {
                    return (Long) field.get(obj);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        throw new RuntimeException("Referenced object has no @Id");
    }


}