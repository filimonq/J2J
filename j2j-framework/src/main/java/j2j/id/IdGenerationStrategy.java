package j2j.id;

/**
 * Strategy for generating unique IDs for persistent objects.
 * Implement this interface to provide a custom ID generation algorithm.
 * The default implementation is {@link CounterIdStrategy}.
 */
public interface IdGenerationStrategy {
    /**
     * Generates the next unique ID.
     *
     * @return a unique Long ID, never null
     */
    Long generateId();
}