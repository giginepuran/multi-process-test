package dev.yin.lib;

public enum Command {
    START,
    COUNT,
    STOP;

    public static Command fromString(String s) {
        return Command.valueOf(s.trim().toUpperCase());
    }
}