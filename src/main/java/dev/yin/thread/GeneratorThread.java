package dev.yin.thread;

public class GeneratorThread implements Runnable {
    private final int generatorId;
    private final int generateIntervalMs;
    private final int childId;

    public GeneratorThread(int childId, int generatorId, int generateIntervalMs) {
        this.childId = childId;
        this.generatorId = generatorId;
        this.generateIntervalMs = generateIntervalMs;
    }

    @Override
    public void run() {
        System.out.println("Hello World! From GeneratorThread" + generatorId);
    }
}