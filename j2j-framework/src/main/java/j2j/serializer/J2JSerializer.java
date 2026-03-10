package j2j.serializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import j2j.annotation.Id;
import j2j.annotation.Persistent;

import java.lang.reflect.Field;

/**
 * Serializes a @Persistent object into a single JSON line (JSONL format).
 *
 * Output example:
 *   {"type":"User","id":1,"name":"Anna","age":19,"active":true}
 *
 * Responsibilities of THIS class:
 *   - Validate that the class is @Persistent                  → exception if not
 *   - Validate that exactly one @Id field exists (type Long)  → exception if not
 *   - Convert the object fields to a JSON string
 *
 * NOT a responsibility of this class:
 *   - Generating or validating the ID value — that's the PersistenceManager's job
 *   - Writing to file — that's the FileStorage's job
 */
public class J2JSerializer {

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Serializes the given @Persistent object to a JSONL string.
     *
     * @param obj must be an instance of a @Persistent class with exactly one @Id Long field
     * @return one-line JSON string, e.g. {"type":"User","id":1,"name":"Anna","age":19,"active":true}
     * @throws J2JSerializationException if class is not @Persistent, has no/multiple @Id fields,
     *                                   or @Id field type is not Long
     */
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
                putValue(node, field.getName(), field.get(obj));
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

    private void putValue(ObjectNode node, String key, Object value) {
        if      (value == null)              node.putNull(key);
        else if (value instanceof String s)  node.put(key, s);
        else if (value instanceof Integer i) node.put(key, i);
        else if (value instanceof Long l)    node.put(key, l);
        else if (value instanceof Double d)  node.put(key, d);
        else if (value instanceof Float f)   node.put(key, f);
        else if (value instanceof Boolean b) node.put(key, b);
        else {
            node.put(key, value.toString());
        }
    }
}