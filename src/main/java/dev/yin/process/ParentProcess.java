package dev.yin.process;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import dev.yin.lib.Command;

public class ParentProcess {
    private final int countIntervalMs;
    private final int childProcessCount;
    private final int threadCount;
    private final int threadGenerateIntervalsMs;
    private Process[] children;
    private PrintWriter[] childWriters;
    private BufferedReader[] childReaders;

    public ParentProcess(int countIntervalMs, int childProcessCount, int threadCount, int threadGenerateIntervalsMs) {
        this.countIntervalMs = countIntervalMs;
        this.childProcessCount = childProcessCount;
        this.threadCount = threadCount;
        this.threadGenerateIntervalsMs = threadGenerateIntervalsMs;
        
    }

    public void start() {
        System.out.println( "Hello World! From ParentProcess" );

        // spawn M child processes
        children = new Process[childProcessCount];
        childWriters = new PrintWriter[childProcessCount];
        childReaders = new BufferedReader[childProcessCount];
        for (int i = 0; i < childProcessCount; i++) {
            startChild(i);
        }
        broadcastCommand(Command.START);
        startCountScheduler();
        /*
        try { Thread.sleep(3000); } catch (InterruptedException e) {}
        broadcastCommand(Command.COUNT);
        try { Thread.sleep(3000); } catch (InterruptedException e) {}
        broadcastCommand(Command.STOP);
        */
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
            childWriters[i].println(cmd.name());
        }
    }

    private void startChildReaderThread(int childNo) {
        new Thread(() -> {
            try {
                String line;
                while ((line = childReaders[childNo].readLine()) != null) {
                    System.out.println(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void startCountScheduler() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(countIntervalMs);
                } catch (InterruptedException e) {
                    return; // exit scheduler
                }

                broadcastCommand(Command.COUNT);
            }
        }).start();
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

}
