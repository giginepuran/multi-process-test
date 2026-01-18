package dev.yin.process;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;

import dev.yin.lib.LogMessage;
import dev.yin.lib.ReadyMessage;
import dev.yin.lib.Command;
import dev.yin.lib.CountMessage;
import dev.yin.lib.RingBuffer;

public class ChildProcess {
    private final int processNo;
    private final int threadCount;
    private final int threadGenerateIntervalsMs;
    private RingBuffer<Integer>[] buffers;
    private final List<ScheduledExecutorService> generators = new ArrayList<>();

    public ChildProcess(int processNo, int parentCountIntervalMs, int threadCount, int threadGenerateIntervalsMs) {
        this.processNo = processNo;
        this.threadCount = threadCount;
        this.threadGenerateIntervalsMs = threadGenerateIntervalsMs;

        @SuppressWarnings("unchecked")
        RingBuffer<Integer>[] tmp = (RingBuffer<Integer>[]) new RingBuffer<?>[threadCount];
        this.buffers = tmp;

        int bufferSize = computeBufferSize(parentCountIntervalMs, threadGenerateIntervalsMs);
        for (int i = 0; i < threadCount; i++) {
            buffers[i] = new RingBuffer<>(bufferSize);
        }
    }

    public void start() {
        startStdinListener();
    }

    private void startStdinListener() {
        new Thread(() -> { // Create Runnable by lambda expression (anonymous function)
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            String event;
            try {
                System.out.println(new ReadyMessage(processNo).toIpc()); // Greeting
                while ((event = in.readLine()) != null) {
                    String[] parts = event.split("\\|");
                    if (parts[0].equals("COMMAND"))
                        commandHandler(parts[1]);
                }

                // stdin closed => parent died. Exit.
                shutdown();
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void commandHandler(String commandType) {
        Command cmd = Command.fromString(commandType);
        switch (cmd) {
            case START:
                for (int i = 0; i < threadCount; i++)
                    startGeneratorThread(i, threadGenerateIntervalsMs, buffers[i]);
                return;

            case COUNT:
                for (int i = 0; i < threadCount; i++)
                    startCounterThread(buffers[i]);
                return;
        
            case STOP:
                System.out.println(new LogMessage(processNo, "Received STOP").toIpc());
                shutdown();
                return;

            default:
                return;
        }
    }

    private void startGeneratorThread(int threadNo, int genIntervalMs, RingBuffer<Integer> buffer) {
        /* 
        Thread t = new Thread(new GeneratorThread(processNo, no, genIntervalMs, buffer));
        t.start(); */
        var exec = Executors.newSingleThreadScheduledExecutor();
        generators.add(exec);

        var random = new java.util.Random();

        exec.scheduleAtFixedRate(() -> {
            int value = random.nextInt(10);
            if (!buffer.write(value)) {
                System.out.println(new LogMessage(processNo, threadNo, "Ring Buffer is full!").toIpc());
            }
        }, 0, threadGenerateIntervalsMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    private void startCounterThread(RingBuffer<Integer> buffer) {
        new Thread(() -> {
            List<Integer> numbers = buffer.flush();
            Map<Integer, Integer> counter = new HashMap<>();

            for (int v : numbers) {
                counter.merge(v, 1, Integer::sum);
            }

            CountMessage msg = new CountMessage(processNo, counter);
            System.out.println(msg.toIpc());
        }).start();
    }

    private static int computeBufferSize(int readIntervalMs, int generateIntervalMs) {
        int itemsPerInterval = readIntervalMs / generateIntervalMs;
        int safety = itemsPerInterval * 4; // 4x safety margin
        return safety;
    }

    private void shutdown() {
        for (var exec : generators) { exec.shutdownNow(); }
        System.exit(0);
    }

    public static void main(String[] args) {
        int childId = Integer.parseInt(args[0]);
        int parentCountintervalMs = Integer.parseInt(args[1]);
        int threadCount = Integer.parseInt(args[2]);
        int intervalMs = Integer.parseInt(args[3]);

        ChildProcess cp = new ChildProcess(childId, parentCountintervalMs, threadCount, intervalMs);
        cp.start();
    }
}