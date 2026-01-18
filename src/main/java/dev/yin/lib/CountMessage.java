package dev.yin.lib;

import java.util.Map;

public class CountMessage {
    private final int process;
    private final Map<Integer, Integer> counts;

    public CountMessage(int process, Map<Integer, Integer> counts) {
        this.process = process;
        this.counts = counts;
    }

    /**
     * Compact IPC representation of this process's count results.
     *
     * Format:
     *   COUNT|P|c0,c1,c2,c3,c4,c5,c6,c7,c8,c9|T
     *
     * Where:
     *   COUNT  — message type
     *   P      — process number
     *   c0..c9 — counts of integers 0 through 9
     *   T      — total count (sum of all c0..c9)
     *
     * Example:
     *   COUNT|1|9,4,7,20,12,4,19,9,10,8|102
     *
     * This format is compact, easy to parse, and optimized for fast
     * inter‑process communication.
     */
    public String toIpc() {
        StringBuilder sb = new StringBuilder();
        sb.append("MESSAGE|COUNT|");
        sb.append(process).append("|");

        int total = 0;
        for (int i = 0; i < 10; i++) {
            int v = counts.getOrDefault(i, 0);
            sb.append(v);
            total += v;
            if (i < 9) sb.append(",");
        }

        sb.append("|").append(total);
        return sb.toString();
    }
}