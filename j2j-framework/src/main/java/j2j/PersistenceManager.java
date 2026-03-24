package j2j;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import j2j.annotation.Id;
import j2j.annotation.Persistent;
import j2j.annotation.Reference;
import j2j.deserializer.J2JDeserializer;
import j2j.filter.JsonFilter;
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
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.HashSet;

/**
 * Central facade of the J2J framework.
 * Implements Unit of Work and Identity Map patterns for persistent objects.
 * * Key concepts:
 * 1. Identity Map: Ensures only one instance of an object exists in memory per ID.
 * 2. Dirty Tracking: Tracks modified objects and saves only changed data.
 * 3. Append-only Logging: Writes updates as new lines to the end of the file for performance.
 */
public class PersistenceManager {

    private final J2JSerializer serializer;
    private final FileStorage storage;
    private final IdGenerationStrategy idStrategy;
    private final J2JDeserializer deserializer;

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Cache of loaded/saved objects to maintain referential integrity.
     */
    private final Map<Long, Object> cache = new HashMap<>();

    /**
     * Set of IDs for objects modified since the last flush.
     */
    private final Set<Long> dirtyIds = new HashSet<>();

    /**
     * Initializes manager with a ID strategy.
     * Auto-detects the starting index if CounterIdStrategy is used.
     * @param filePath path to the storage file
     * @param idStrategy strategy for generating unique IDs
     */
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

    /**
     * Marks an object for persistence.
     * Generates a new ID if it's missing, puts the object in cache,
     * and flags it as "dirty" for the next flush.
     * @param obj object to save (must be annotated with @Persistent)
     */
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

    /**
     * Synchronizes all dirty objects with the file storage.
     * Uses append-only logic by writing updated states to the end of the file.
     */
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

    /**
     * Loads all data from storage into memory.
     * Implements deduplication: only the most recent version of an ID is kept.
     * Performs two-pass loading to resolve references between objects.
     */
    public void loadAll() {
        List<String> lines = storage.readAllLines();
        if (lines.isEmpty()) return;

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

    /**
     * Removes old object versions from the file.
     * Leaves only the most recent entry for each ID.
     */
    public void compact() {
        List<String> lines = storage.readAllLines();
        if (lines.isEmpty()) return;

        Map<Long, String> latestLines = new LinkedHashMap<>();

        try {
            for (String line : lines) {
                if (line.isBlank()) continue;

                JsonNode node = mapper.readTree(line);
                JsonNode idNode = node.get("id");

                if (idNode != null && !idNode.isNull()) {
                    latestLines.put(idNode.asLong(), line);
                }
            }

            storage.overwriteLines(new ArrayList<>(latestLines.values()));

        } catch (Exception e) {
            throw new RuntimeException("Compaction failed", e);
        }
    }

    /**
     * Retrieves an object from the internal cache by its ID.
     * @param id the unique identifier
     * @return the cached object or null if not found
     */
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

    public List<Object> loadWithFilter(JsonFilter filter) {
        Map<Long, JsonNode> allNodes = new HashMap<>();
        List<JsonNode> filteredNodes = new ArrayList<>();

        try (var lines = storage.streamLines()) {
            lines.filter(line -> !line.isBlank()).forEach(line -> {
                try {
                    JsonNode node = mapper.readTree(line);
                    allNodes.put(node.get("id").asLong(), node);
                    if (filter.matches(node)) {
                        filteredNodes.add(node);
                    }
                } catch (Exception e) {
                    throw new RuntimeException("JSON error", e);
                }
            });

            for (JsonNode node : filteredNodes) {
                Object obj = deserializer.createShallow(node);
                cache.put(node.get("id").asLong(), obj);
            }

            for (JsonNode node : filteredNodes) {
                resolveDependencies(node, allNodes);
            }

            List<Object> result = new ArrayList<>();
            for (JsonNode node : filteredNodes) {
                Long id = node.get("id").asLong();
                Object obj = cache.get(id);
                deserializer.resolveReferences(obj, node);
                result.add(obj);
            }
            return result;

        } catch (Exception e) {
            throw new RuntimeException("Filter loading failed", e);
        }
    }

    private void resolveDependencies(JsonNode node, Map<Long, JsonNode> allNodes) {
        try {
            for (Field field : getClassFields(node)) {
                if (!field.isAnnotationPresent(Reference.class)) continue;

                String refKey = field.getName() + "Id";
                JsonNode refNode = node.get(refKey);
                if (refNode == null || refNode.isNull()) continue;

                Long refId = refNode.asLong();

                if (this.getById(refId) == null) {
                    JsonNode refJson = allNodes.get(refId);
                    if (refJson == null) throw new RuntimeException("Ref " + refId + " not found in file");

                    Object refObj = deserializer.createShallow(refJson);
                    cache.put(refId, refObj);

                    resolveDependencies(refJson, allNodes); // Рекурсия по памяти
                    deserializer.resolveReferences(refObj, refJson);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Dependency resolution failed", e);
        }
    }

    private Field[] getClassFields(JsonNode node) {
        try {
            String type = node.get("type").asText();
            Class<?> clazz = Class.forName("j2j.model." + type);
            return clazz.getDeclaredFields();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get class fields", e);
        }
    }
}
