package dev.yin.lib;

public class AtomicInteger {

    private int value;
    private final Object lock = new Object();

    public AtomicInteger() {
        this(0);
    }

    public AtomicInteger(int initialValue) {
        this.value = initialValue;
    }

    public void set(int newValue) {
        synchronized (lock) {
            this.value = newValue;
        }
    }

    public int get() {
        synchronized (lock) {
            return value;
        }
    }

    public int increment() {
        synchronized (lock) {
            return ++value;
        }
    }

    public int decrement() {
        synchronized (lock) {
            return --value;
        }
    }

    public int add(int delta) {
        synchronized (lock) {
            value += delta;
            return value;
        }
    }
}