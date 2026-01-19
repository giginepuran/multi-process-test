package dev.yin.process;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.List;
import java.util.function.BooleanSupplier;

import dev.yin.lib.Command;
import dev.yin.lib.FlushQueue;
import dev.yin.lib.AtomicInteger;

public class ParentProcess {
    private final int countIntervalMs;
    private final int childProcessCount;
    private final int threadCount;
    private final int threadGenerateIntervalsMs;
    private Process[] children;
    private PrintWriter[] childWriters;
    private BufferedReader[] childReaders;
    private final FlushQueue<String> countResultQueue = new FlushQueue<>();

    private final Object readyLock = new Object();
    private final AtomicInteger readyMsgCount = new AtomicInteger(0);
    private final Object countLock = new Object();
    private final AtomicInteger countMsgCount = new AtomicInteger(0);
    
    private static final boolean DEBUG =
        Boolean.parseBoolean(System.getProperty("debug.mode", "false"));

    public ParentProcess(int countIntervalMs, int childProcessCount, int threadCount, int threadGenerateIntervalsMs) {
        this.countIntervalMs = countIntervalMs;
        this.childProcessCount = childProcessCount;
        this.threadCount = threadCount;
        this.threadGenerateIntervalsMs = threadGenerateIntervalsMs;
    }

    public void start() {
        System.out.println( "Hello World! From ParentProcess" );
        if (DEBUG) 
        {
            // In theory, expectedTotal numbers are generated every countIntervalMs
            int expectedTotal = (childProcessCount * threadCount) // Total threads
                                * countIntervalMs                 // Count period
                                / threadGenerateIntervalsMs;      // Generate priod
            System.out.println( "expectedTotal of each counter JSON output string: " + expectedTotal);
        }

        // Register shutdown hook BEFORE starting children
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Parent received Ctrl+C, shutting down children ...");
            shutdownChildren();
        }));

        readyMsgCount.set(0);
        // spawn N child processes
        children = new Process[childProcessCount];
        childWriters = new PrintWriter[childProcessCount];
        childReaders = new BufferedReader[childProcessCount];
        for (int i = 0; i < childProcessCount; i++) {
            startChild(i);
        }
        
        waitUntil(readyLock, () -> readyMsgCount.get() >= childProcessCount);
        broadcastCommand(Command.START);
        startCountScheduler();
    }

    private void startChild(int childNo) {
        try {
            String cp = getCurrentClasspath();
            ProcessBuilder pb = new ProcessBuilder(
                "java",
                "-cp", cp, // classpath
                "dev.yin.process.ChildProcess",
                String.valueOf(childNo),
                String.valueOf(countIntervalMs),
                String.valueOf(this.threadCount),
                String.valueOf(this.threadGenerateIntervalsMs)
            );

            Process child = pb.start();
            this.children[childNo] = child;
            // Parent → Child (stdin)
            childWriters[childNo] = new PrintWriter(child.getOutputStream(), true);

            // Child (stdout) → Parent 
            childReaders[childNo] = new BufferedReader(
                new InputStreamReader(child.getInputStream())
            );

            // Start a thread to read child's stdout
            startChildReaderThread(childNo);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void broadcastCommand(Command cmd) {
        for (int i = 0; i < this.childProcessCount; i++) {
            childWriters[i].println("COMMAND|" + cmd.name());
        }
    }

    private void startChildReaderThread(int childNo) {
        new Thread(() -> {
            try {
                String line;
                while ((line = childReaders[childNo].readLine()) != null) {
                    if (line.startsWith("MESSAGE|COUNT|")) {
                        countResultQueue.offer(line); // enqueue
                        synchronized (countLock) { // Make "increment + notify" is atomicity
                            countMsgCount.increment();
                            countLock.notifyAll();
                        }
                    } else if (line.startsWith("MESSAGE|READY|")) {
                        synchronized (readyLock) { // Make "increment + notify" is atomicity
                            readyMsgCount.increment();
                            readyLock.notifyAll();
                        }
                    } else if (line.startsWith("MESSAGE|LOG|")) {
                        // do nothing, can output to log file
                        //System.out.println(line);
                    } else {
                        // do nothing
                        //System.out.println(line);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void shutdownChildren() {
        try {
            broadcastCommand(Command.STOP);

            for (int i = 0; i < childProcessCount; i++) {
                    children[i].waitFor();   // blocks until child exits
            }

            System.out.println("All children terminated.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startCountScheduler() {
        new Thread(() -> {
            long countTimeStamp = System.currentTimeMillis();

            while (true) {
                countTimeStamp += countIntervalMs;
                long sleep = countTimeStamp - System.currentTimeMillis();

                try {
                    if (sleep > 0) Thread.sleep(sleep);
                } catch (InterruptedException e) {
                    return;
                }

                countMsgCount.set(0);
                // Ask children to send COUNT messages
                broadcastCommand(Command.COUNT);
                // Wait all COUNT messages arrived
                waitUntil(countLock, () -> countMsgCount.get() >= childProcessCount);

                // Flush all COUNT messages
                List<String> msgs = countResultQueue.flush();

                // Combine them
                int[] combined = combineCounts(msgs);

                // Output JSON
                System.out.println(buildOutputJson(combined, countTimeStamp / 1000));
            }
        }).start();
    }

    private void waitUntil(Object lock, BooleanSupplier condition) {
        synchronized (lock) {
            while (!condition.getAsBoolean()) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private String getCurrentClasspath() {
        String classpath = ParentProcess.class
            .getProtectionDomain()
            .getCodeSource()
            .getLocation()
            .getPath();

        // If running from target/classes, classpath ends with "/classes/"
        // If running from a JAR, classpath ends with ".jar"
        return classpath;
    }

    private int[] parseCountResult(String msg) {
        /**
         * Format:
         *     MESSAGE|COUNT|P|c0,c1,c2,c3,c4,c5,c6,c7,c8,c9|T
         * Example:
         *     MESSAGE|COUNT|1|9,4,7,20,12,4,19,9,10,8|102
         */
        String[] parts = msg.split("\\|");
        String[] nums = parts[3].split(",");

        int[] arr = new int[10];
        for (int i = 0; i < 10; i++) {
            arr[i] = Integer.parseInt(nums[i]);
        }
        return arr;
    }

    private int[] combineCounts(List<String> countResults) {
        int[] total = new int[10];

        for (String countResult : countResults) {
            int[] arr = parseCountResult(countResult);
            for (int i = 0; i < 10; i++) {
                total[i] += arr[i];
            }
        }

        return total;
    }

    private String buildOutputJson(int[] counts, long unixSec) {
        StringBuilder sb = new StringBuilder();
        int total = 0; // For Test

        sb.append("{ \"time\": \"").append(unixSec).append("\",\"counts\": {");

        for (int i = 0; i < 10; i++) {
            total += counts[i];
            sb.append("\"").append(i).append("\": ").append(counts[i]);
            if (i < 9) sb.append(", ");
        }

        sb.append("}");
        // To check nums generated is expected or not
        if (DEBUG) { sb.append(", \"total\": ").append(total); }
        sb.append(" }");
        return sb.toString();
    }
}
