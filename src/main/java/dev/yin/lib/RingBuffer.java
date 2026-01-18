package dev.yin.lib;

import java.util.ArrayList;
import java.util.List;

/**
 * A high-performance, lock-free Single-Producer Single-Consumer (SPSC) ring buffer.
 * <p>
 * This buffer is designed for scenarios where:
 * <ul>
 *     <li>Exactly one thread writes (producer)</li>
 *     <li>Exactly one thread reads (consumer)</li>
 *     <li>No blocking or locking is desired</li>
 *     <li>Dropping new data when the buffer is full is acceptable</li>
 * </ul>
 *
 * <p>
 * The buffer capacity is automatically rounded up to the next power of two,
 * enabling fast index wrapping using bit masking instead of modulo operations.
 * </p>
 *
 * <h3>Overflow Policy</h3>
 * If the producer writes faster than the consumer flushes, and the buffer becomes full,
 * the newest value is silently dropped. Older unread values are preserved.
 *
 * <h3>Thread Safety</h3>
 * This implementation is safe <b>only</b> for SPSC usage:
 * <ul>
 *     <li>One producer thread calling {@link #write(Object)}</li>
 *     <li>One consumer thread calling {@link #flush()}</li>
 * </ul>
 * Using multiple producers or multiple consumers will result in undefined behavior.
 *
 * @param <T> the type of elements stored in the buffer
 */
public class RingBuffer<T> implements Buffer<T> {

    private final Object[] buffer;
    private final int mask;

    // Single-producer index
    private long writeIndex = 0;

    // Single-consumer index
    private long readIndex = 0;

    /**
     * Creates a new ring buffer with at least the requested capacity.
     * The actual capacity is rounded up to the next power of two.
     *
     * @param requestCapacity minimum desired capacity
     */
    public RingBuffer(int requestCapacity) {
        int cap = 1;
        while (cap < requestCapacity) {
            cap <<= 1;
        }
        this.buffer = new Object[cap];
        this.mask = cap - 1;
    }

    /**
     * Writes a value into the buffer.
     * <p>
     * If the buffer is full, the new value is dropped and not stored.
     *
     * @param value the value to write
     */
    @Override
    public boolean write(T value) {
        long ri = this.readIndex;
        long wi = this.writeIndex;

        // Buffer full → drop new data
        if (wi - ri >= buffer.length) {
            return false;
        }

        buffer[(int) (wi & mask)] = value;
        writeIndex = wi + 1;
        return true;
    }

    /**
     * Flushes all unread values from the buffer.
     * <p>
     * This method returns a snapshot of all items written since the last flush.
     * The returned list is ordered from oldest to newest.
     *
     * @return a list of all unread values, or an empty list if none
     */
    @Override
    public List<T> flush() {
        long wi = this.writeIndex;
        long ri = this.readIndex;

        long available = wi - ri;
        if (available <= 0) {
            return List.of();
        }

        List<T> result = new ArrayList<>((int) available);

        for (long i = ri; i < wi; i++) {
            @SuppressWarnings("unchecked")
            T value = (T) buffer[(int) (i & mask)];
            result.add(value);
        }

        readIndex = wi;
        return result;
    }
}