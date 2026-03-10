package j2j.id;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple incrementing counter strategy: 1, 2, 3, ...
 * The counter starts at the given initialValue (default: 1).
 */
public class CounterIdStrategy implements IdGenerationStrategy {

    private final AtomicLong counter;

    /** Starts from 1. */
    public CounterIdStrategy() {
        this(1L);
    }

    /** Starts from the given initial value. */
    public CounterIdStrategy(Long initialValue) {
        if (initialValue == null || initialValue < 1) {
            throw new IllegalArgumentException("Initial ID value must be >= 1, got: " + initialValue);
        }
        this.counter = new AtomicLong(initialValue);
    }

    @Override
    public Long generateId() {
        return counter.getAndIncrement();
    }
}