package dev.yin.process;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import dev.yin.thread.GeneratorThread;

public class ChildProcess {

    private final int childId;
    private final int threadCount;
    private final int threadGenerateIntervalsMs;

    public ChildProcess(int childId, int threadCount, int threadGenerateIntervalsMs) {
        this.childId = childId;
        this.threadCount = threadCount;
        this.threadGenerateIntervalsMs = threadGenerateIntervalsMs;
    }

    public void start() {
        startGeneratorThreads();
        startStdinListener();
    }

    private void startGeneratorThreads() {
        for (int i = 0; i < threadCount; i++) {
            Thread t = new Thread(new GeneratorThread(this.childId, i, this.threadGenerateIntervalsMs));
            t.start();
        }
    }

    private void startStdinListener() {
        new Thread(() -> { // Create Runnable by lambda expression (anonymous function)
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            String event;
            try {
                while ((event = in.readLine()) != null) {
                    Command cmd = Command.fromString(event);
                    switch (cmd) {
                        case START:
                            System.out.println("Child " + childId + " received START");
                            break;
                    
                        case STOP:
                            System.out.println("Child " + childId + " received STOP");
                            return;

                        default:
                            break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static void main(String[] args) {
        int childId = Integer.parseInt(args[0]);
        int threadCount = Integer.parseInt(args[1]);
        int intervalMs = Integer.parseInt(args[2]);

        ChildProcess cp = new ChildProcess(childId, threadCount, intervalMs);
        cp.start();
    }
}