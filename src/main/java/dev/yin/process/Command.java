package dev.yin.process;

public enum Command {
    START,
    STOP;

    public static Command fromString(String s) {
        return Command.valueOf(s.trim().toUpperCase());
    }
}