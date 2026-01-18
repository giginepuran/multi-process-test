package dev.yin.lib;

import java.util.ArrayList;
import java.util.List;

public class FlushQueue<T> {

    private static class Node<E> {
        final E value;
        Node<E> next;

        Node(E value) {
            this.value = value;
        }
    }

    private final Object lock = new Object();
    private Node<T> head;
    private Node<T> tail;

    public FlushQueue() {
        Node<T> dummy = new Node<>(null);
        head = dummy;
        tail = dummy;
    }

    // Many producers
    public void offer(T value) {
        synchronized (lock) {
            Node<T> node = new Node<>(value);
            tail.next = node;
            tail = node;
        }
    }

    // Single consumer: flush all items at once
    public List<T> flush() {
        List<T> result = new ArrayList<>();
        Node<T> curr = null;

        synchronized (lock) {
            // head SHOULD be a dummy Node
            curr = head.next;
            if (curr == null) {
                return result;
            }

            // Reset queue to empty state
            Node<T> dummy = new Node<>(null);
            head = dummy;
            tail = dummy;
        }

        // Collect all values
        while (curr != null) {
            result.add(curr.value);
            curr = curr.next;
        }

        return result;
    }
}