package dev.yin.lib;

public class LogMessage {
    private final int process;
    private final Integer thread;   // nullable
    private final String msg;

    public LogMessage(int process, Integer thread, String msg) {
        this.process = process;
        this.thread = thread;
        this.msg = msg;
    }

    public LogMessage(int process, String msg) {
        this(process, null, msg);
    }

    /**
     * Compact IPC representation.
     *
     * Format:
     *   LOG|P|T|message
     *
     * Where:
     *   LOG     — message type
     *   P       — process number
     *   T       — thread number or '-' if none
     *   message — raw log message
     */
    public String toIpc() {
        StringBuilder sb = new StringBuilder();
        sb.append("MESSAGE|LOG|");
        sb.append(process).append("|");
        sb.append(thread == null ? "-" : thread).append("|");
        sb.append(msg);
        return sb.toString();
    }
}