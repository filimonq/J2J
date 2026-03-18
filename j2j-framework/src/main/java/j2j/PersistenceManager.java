package j2j;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import j2j.annotation.Id;
import j2j.annotation.Persistent;
import j2j.deserializer.J2JDeserializer;
import j2j.id.CounterIdStrategy;
import j2j.id.IdGenerationStrategy;
import j2j.id.UuidIdStrategy;
import j2j.serializer.J2JSerializationException;
import j2j.serializer.J2JSerializer;
import j2j.storage.FileStorage;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * Central facade of the J2J framework.
 * Workflow:
 * save(obj)  → assigns ID if null → updates cache → marks ID as dirty
 * flush()    → serializes only dirty objects → appends to file via FileStorage
 */
public class PersistenceManager {

    private final J2JSerializer serializer;
    private final FileStorage storage;
    private final IdGenerationStrategy idStrategy;
    private final J2JDeserializer deserializer;

    private final Map<Long, Object> cache = new HashMap<>();

    private final Set<Long> dirtyIds = new HashSet<>();

    public PersistenceManager(String filePath) {
        this(filePath, new UuidIdStrategy());
    }

    public PersistenceManager(String filePath, IdGenerationStrategy idStrategy) {
        this.storage = new FileStorage(Path.of(filePath));
        this.serializer = new J2JSerializer();
        this.deserializer = new J2JDeserializer(this);
        if (idStrategy instanceof CounterIdStrategy) {
            this.idStrategy = new CounterIdStrategy(detectMaxId() + 1);
        } else {
            this.idStrategy = idStrategy;
        }
    }

    public void save(Object obj) {
        if (obj == null) {
            throw new IllegalArgumentException("Cannot save null object");
        }
        validatePersistent(obj.getClass());

        Long id = extractIdSafely(obj);
        if (id == null) {
            id = idStrategy.generateId();
            setId(obj, id);
        }

        cache.put(id, obj);
        dirtyIds.add(id);
    }

    private void validatePersistent(Class<?> clazz) {
        if (!clazz.isAnnotationPresent(Persistent.class)) {
            throw new J2JSerializationException(
                    "Class " + clazz.getName() + " is not annotated with @Persistent"
            );
        }
    }

    private Long extractIdSafely(Object obj) {
        for (Field field : obj.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                field.setAccessible(true);
                try {
                    return (Long) field.get(obj);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Failed to access @Id field", e);
                }
            }
        }
        return null;
    }

    public void flush() {
        if (dirtyIds.isEmpty()) return;

        List<String> linesToWrite = new ArrayList<>();
        for (Long id : dirtyIds) {
            Object obj = cache.get(id);
            if (obj != null) {
                String json = serializer.serialize(obj);
                linesToWrite.add(json);
            }
        }

        storage.appendLines(linesToWrite);
        dirtyIds.clear();
    }

    public void loadAll() {
        List<String> lines = storage.readAllLines();
        if (lines.isEmpty()) return;

        ObjectMapper mapper = new ObjectMapper();

        Map<Long, JsonNode> latestNodes = new HashMap<>();

        try {
            for (String line : lines) {
                if (!line.isBlank()) {
                    JsonNode node = mapper.readTree(line);
                    JsonNode idNode = node.get("id");
                    if (idNode != null && !idNode.isNull()) {
                        latestNodes.put(idNode.asLong(), node);
                    }
                }
            }

            for (JsonNode node : latestNodes.values()) {
                Object obj = deserializer.createShallow(node);
                Long id = node.get("id").asLong();
                cache.put(id, obj);
            }

            for (JsonNode node : latestNodes.values()) {
                Long id = node.get("id").asLong();
                Object obj = cache.get(id);
                deserializer.resolveReferences(obj, node);
            }

        } catch (Exception e) {
            throw new RuntimeException("loadAll failed", e);
        }
    }

    public Object getById(Long id) {
        return cache.get(id);
    }

    private void setId(Object obj, Long id) {
        for (Field field : obj.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                field.setAccessible(true);
                try {
                    field.set(obj, id);
                    return;
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Failed to assign ID to " + obj.getClass().getSimpleName(), e);
                }
            }
        }
    }

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
            } catch (Exception ignored) {}
        }
        return maxId;
    }
}
