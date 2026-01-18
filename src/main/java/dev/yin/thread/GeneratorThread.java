package dev.yin.thread;

import dev.yin.lib.LogMessage;
import dev.yin.lib.RingBuffer;

public class GeneratorThread implements Runnable {
    private final int processNo;
    private final int threadNo;
    private final int generateIntervalMs;
    private final RingBuffer<?> buffer;

    public GeneratorThread(int threadNo, int generateIntervalMs, RingBuffer<?> buffer) {
        this(0, threadNo, generateIntervalMs, buffer);
    }

    public GeneratorThread(int processNo, int threadNo, int generateIntervalMs, RingBuffer<?> buffer) {
        this.processNo = processNo;
        this.threadNo = threadNo;
        this.generateIntervalMs = generateIntervalMs;
        this.buffer = buffer;
    }

    @Override
    public void run() {
        System.out.println(new LogMessage(processNo, threadNo, "Hello, generator thread.").toJson());
    }
}