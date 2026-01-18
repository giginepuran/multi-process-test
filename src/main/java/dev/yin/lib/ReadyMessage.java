package dev.yin.lib;

public class ReadyMessage {

    private final int process;

    public ReadyMessage(int process) {
        this.process = process;
    }

    /**
     * Compact IPC representation.
     *
     * Format:
     *   READY|P
     *
     * Where:
     *   READY — message type
     *   P     — process number
     *
     * Example:
     *   READY|3
     */
    public String toIpc() {
        return "MESSAGE|READY|" + process;
    }

    public int getProcess() {
        return process;
    }
}