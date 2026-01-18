package dev.yin.lib;

import java.util.List;

public interface Buffer<T> {
    void write(T value);       // producer writes one item
    List<T> flush();           // consumer gets all unread items
}