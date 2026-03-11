package j2j;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import j2j.annotation.Id;
import j2j.annotation.Persistent;
import j2j.id.CounterIdStrategy;
import j2j.id.IdGenerationStrategy;
import j2j.id.UuidIdStrategy;
import j2j.serializer.J2JSerializer;
import j2j.serializer.J2JSerializationException;
import j2j.storage.FileStorage;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Central facade of the J2J framework.
 * Workflow:
 *   save(obj)  → assigns ID if null → serializes → puts into buffer
 *   flush()    → writes entire buffer to file via FileStorage
 * Example:
 *   PersistenceManager manager = new PersistenceManager("storage.json");
 *   manager.save(user);
 *   manager.flush();
 */
public class PersistenceManager {

    private final J2JSerializer serializer;
    private final FileStorage storage;
    private final IdGenerationStrategy idStrategy;

    private final List<String> buffer = new ArrayList<>();

    /**
     * Creates a PersistenceManager with UuidIdStrategy.
     *
     * @param filePath path to the JSONL storage file
     */
    public PersistenceManager(String filePath) {
        this(filePath, new UuidIdStrategy());
    }

    /**
     * Creates a PersistenceManager with a custom ID generation strategy.
     * If CounterIdStrategy is passed — automatically detects max existing ID
     * in the file and starts the counter from maxId + 1.
     *
     * @param filePath   path to the JSONL storage file
     * @param idStrategy strategy to use for ID generation
     */
    public PersistenceManager(String filePath, IdGenerationStrategy idStrategy) {
        this.storage = new FileStorage(Path.of(filePath));
        this.serializer = new J2JSerializer();

        if (idStrategy instanceof CounterIdStrategy) {
            this.idStrategy = new CounterIdStrategy(detectMaxId() + 1);
        } else {
            this.idStrategy = idStrategy;
        }
    }

    /**
     * Prepares an object for persistence:
     *   1. Validates @Persistent annotation
     *   2. Assigns an ID if the @Id field is null
     *   3. Serializes to JSON and puts into the buffer
     * Changes are NOT written to file until flush() is called.
     *
     * @param obj object to save; must be @Persistent with an @Id Long field
     * @throws J2JSerializationException if the object is invalid
     */
    public void save(Object obj) {
        if (obj == null) {
            throw new IllegalArgumentException("Cannot save null object");
        }

        validatePersistent(obj.getClass());
        assignIdIfAbsent(obj);

        String json = serializer.serialize(obj);
        buffer.add(json);
    }

    /**
     * Writes all buffered objects to the storage file and clears the buffer.
     * Does nothing if the buffer is empty.
     */
    public void flush() {
        if (buffer.isEmpty()) return;

        storage.appendLines(new ArrayList<>(buffer));
        buffer.clear();
    }

    private void validatePersistent(Class<?> clazz) {
        if (!clazz.isAnnotationPresent(Persistent.class)) {
            throw new J2JSerializationException(
                    "Class " + clazz.getName() + " is not annotated with @Persistent"
            );
        }
    }

    private void assignIdIfAbsent(Object obj) {
        Class<?> clazz = obj.getClass();

        for (Field field : clazz.getDeclaredFields()) {
            if (!field.isAnnotationPresent(Id.class)) continue;

            try {
                field.setAccessible(true);
                if (field.get(obj) == null) {
                    field.set(obj, idStrategy.generateId());
                }
            } catch (IllegalAccessException e) {
                throw new J2JSerializationException(
                        "Failed to assign ID to " + clazz.getSimpleName(), e
                );
            }
            return;
        }
    }

    /**
     * Reads the storage file and finds the maximum "id" value across all lines.
     * Returns 0 if the file is empty or doesn't exist.
     */
    private long detectMaxId() {
        List<String> lines = storage.readAllLines();
        if (lines.isEmpty()) return 0L;

        ObjectMapper mapper = new ObjectMapper();
        long maxId = 0L;

        for (String line : lines) {
            try {
                JsonNode node = mapper.readTree(line);
                JsonNode idNode = node.get("id");
                if (idNode != null && idNode.isNumber()) {
                    maxId = Math.max(maxId, idNode.longValue());
                }
            } catch (Exception ignored) {
            }
        }

        return maxId;
    }

}