package dev.yin.process;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.yin.lib.LogMessage;
import dev.yin.lib.Command;
import dev.yin.lib.CountMessage;
import dev.yin.lib.RingBuffer;
import dev.yin.thread.GeneratorThread;

public class ChildProcess {
    private final int processNo;
    private final int threadCount;
    private final int threadGenerateIntervalsMs;
    private RingBuffer<Integer>[] buffers;

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
            int nextThreadNo = 0;
            try {
                while ((event = in.readLine()) != null) {
                    Command cmd = Command.fromString(event);
                    switch (cmd) {
                        case START:
                            System.out.println(new LogMessage(processNo, "Received START").toJson());
                            for (int i = 0; i < threadCount; i++, nextThreadNo++)
                                startGeneratorThread(nextThreadNo, threadGenerateIntervalsMs, buffers[i]);
                            break;

                        case COUNT:
                            //System.out.println(new LogMessage(processNo, "Received COUNT").toJson());
                            for (int i = 0; i < threadCount; i++, nextThreadNo++)
                                startCounterThread(nextThreadNo, buffers[i]);
                            break;
                    
                        case STOP:
                            System.out.println(new LogMessage(processNo, "Received STOP").toJson());
                            System.exit(0);
                            return;

                        default:
                            break;
                    }
                }

                // stdin closed => parent died. Exit this process.
                System.exit(0);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void startGeneratorThread(int no, int genIntervalMs, RingBuffer<Integer> buffer) {
        Thread t = new Thread(new GeneratorThread(no, genIntervalMs, buffer));
        t.start();
    }

    private void startCounterThread(int no, RingBuffer<Integer> buffer) {
        new Thread(() -> {
            List<Integer> numbers = buffer.flush();
            Map<Integer, Integer> counter = new HashMap<>();

            for (int v : numbers) {
                counter.merge(v, 1, Integer::sum);
            }

            CountMessage msg = new CountMessage(processNo, counter);
            //System.out.println(msg.toJson());
            System.out.println(msg.toIpc());
        }).start();
    }

    private static int computeBufferSize(int readIntervalMs, int generateIntervalMs) {
        int itemsPerInterval = readIntervalMs / generateIntervalMs;
        int safety = itemsPerInterval * 4; // 4x safety margin
        return safety;
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