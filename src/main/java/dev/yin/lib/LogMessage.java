package dev.yin.lib;

public class LogMessage {
    private final int process;
    private final Integer thread;   // use Integer so null is possible
    private final String msg;

    public LogMessage(int process, Integer thread, String msg) {
        this.process = process;
        this.thread = thread;
        this.msg = msg;
    }

    public LogMessage(int process, String msg) {
        this(process, null, msg);
    }

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"process\":").append(process);

        if (thread != null) {
            sb.append(",\"thread\":").append(thread);
        }

        sb.append(",\"msg\":\"").append(msg).append("\"}");
        return sb.toString();
    }
}