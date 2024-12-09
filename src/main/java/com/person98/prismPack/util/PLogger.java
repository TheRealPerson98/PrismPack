package com.person98.prismPack.util;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A custom logging utility that adds color and prefix support to standard Java logging.
 */
public class PLogger {
    private static Logger logger;
    private static String prefix;
    private static ConsoleColor color;

    public PLogger() {
    }

    /**
     * Initializes the logger with the specified parameters.
     * @param baseLogger The base Java logger to use
     * @param loggerPrefix The prefix to prepend to all log messages
     * @param loggerColor The color to use for the prefix
     */
    public static void setup(Logger baseLogger, String loggerPrefix, ConsoleColor loggerColor) {
        logger = baseLogger;
        prefix = loggerPrefix;
        color = loggerColor;
    }

    /**
     * Logs an informational message.
     * @param message The message to log
     */
    public static void info(String message) {
        log(Level.INFO, message);
    }

    /**
     * Logs a warning message.
     * @param message The message to log
     */
    public static void warning(String message) {
        log(Level.WARNING, message);
    }

    /**
     * Logs a severe error message with an optional stack trace.
     * @param message The message to log
     * @param throwable The throwable containing the stack trace (can be null)
     */
    public static void severe(String message, Throwable throwable) {
        if (throwable != null) {
            log(Level.SEVERE, message + "\n" + getStackTraceAsString(throwable));
        } else {
            log(Level.SEVERE, message);
        }
    }

    /**
     * Logs a severe error message.
     * @param message The message to log
     */
    public static void severe(String message) {
        severe(message, null);
    }

    /**
     * Logs a debug message.
     * @param message The message to log
     */
    public static void debug(String message) {
        log(Level.FINE, message);
    }

    private static void log(Level level, String message) {
        if (logger == null) {
            throw new IllegalStateException("PLogger has not been set up. Call setup() first.");
        } else {
            String coloredMessage = color + "[" + prefix + "] " + ConsoleColor.WHITE + message + "\u001b[0m";
            logger.log(level, coloredMessage);
        }
    }

    private static String getStackTraceAsString(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append("    at ").append(element.toString()).append("\n");
        }
        return sb.toString();
    }

    public static enum ConsoleColor {
        BLACK("\u001b[30m"),
        RED("\u001b[31m"),
        GREEN("\u001b[32m"),
        YELLOW("\u001b[33m"),
        BLUE("\u001b[34m"),
        PURPLE("\u001b[35m"),
        CYAN("\u001b[36m"),
        WHITE("\u001b[37m"),
        BRIGHT_RED("\u001b[91m"),
        BRIGHT_GREEN("\u001b[92m");

        private final String code;

        private ConsoleColor(String code) {
            this.code = code;
        }

        public String toString() {
            return this.code;
        }
    }
}
