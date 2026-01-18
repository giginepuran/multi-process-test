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
     * Returns a compact IPC (inter‑process communication) string representing the
     * count results for this process. This format is optimized for machine parsing
     * and is significantly shorter than the JSON representation.
     *
     * <p><strong>Format:</strong></p>
     * <pre>
     * COUNT|P|c0,c1,c2,c3,c4,c5,c6,c7,c8,c9|T
     * </pre>
     *
     * Where:
     * <ul>
     *   <li><code>COUNT</code> — message type identifier</li>
     *   <li><code>P</code> — process number</li>
     *   <li><code>c0..c9</code> — counts of integers 0 through 9</li>
     *   <li><code>T</code> — total count (sum of all c0..c9)</li>
     * </ul>
     *
     * <p><strong>Example:</strong></p>
     * <pre>
     * COUNT|1|9,4,7,20,12,4,19,9,10,8|102
     * </pre>
     *
     * @return compact IPC string in the format described above
     */
    public String toIpc() {
        StringBuilder sb = new StringBuilder();
        sb.append("COUNT").append("|");
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

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        int total = 0;

        sb.append("{\"process\":").append(process)
          .append(",\"counts\":{");

        boolean first = true;
        for (var entry : counts.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":").append(entry.getValue());
            total += entry.getValue();
            first = false;
        }

        sb.append("}")
          .append(",\"total\":").append(total)
          .append("}");
        return sb.toString();
    }
}