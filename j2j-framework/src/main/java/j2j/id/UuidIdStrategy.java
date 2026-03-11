package j2j.id;

import java.util.UUID;

/**
 * ID generation strategy based on UUID.
 */
public class UuidIdStrategy implements IdGenerationStrategy {

    @Override
    public Long generateId() {
        return Math.abs(UUID.randomUUID().getMostSignificantBits());
    }
}