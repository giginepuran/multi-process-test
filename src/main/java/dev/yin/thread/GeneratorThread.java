package dev.yin.thread;

import dev.yin.lib.LogMessage;
import dev.yin.lib.RingBuffer;

public class GeneratorThread implements Runnable {
    private final int processNo;
    private final int threadNo;
    private final int generateIntervalMs;
    private final RingBuffer<Integer> buffer;

    public GeneratorThread(int threadNo, int generateIntervalMs, RingBuffer<Integer> buffer) {
        this(0, threadNo, generateIntervalMs, buffer);
    }

    public GeneratorThread(int processNo, int threadNo, int generateIntervalMs, RingBuffer<Integer> buffer) {
        this.processNo = processNo;
        this.threadNo = threadNo;
        this.generateIntervalMs = generateIntervalMs;
        this.buffer = buffer;
    }

    @Override
    public void run() {
        System.out.println(new LogMessage(processNo, threadNo, "Generator thread started").toJson());

        var random = new java.util.Random();

        // Create a scheduler for periodic generation
        java.util.concurrent.ScheduledExecutorService exec =
            java.util.concurrent.Executors.newSingleThreadScheduledExecutor();

        exec.scheduleAtFixedRate(() -> {
            int value = random.nextInt(10);

            if (!buffer.write(value)) {
                System.out.println(new LogMessage(processNo, threadNo, "Ring Buffer is full!").toJson());
            }

        }, 0, generateIntervalMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }
}