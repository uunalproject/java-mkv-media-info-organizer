package com.movieorganizer.util;

import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class ColorConsoleFormatter extends Formatter {

    private static final String YELLOW = "[33m";
    private static final String RED    = "[31m";
    private static final String RESET  = "[0m";

    @Override
    public String format(LogRecord record) {
        String message = formatMessage(record);
        if (record.getLevel().intValue() >= Level.SEVERE.intValue()) {
            return RED + record.getLevel().getName() + ": " + message + RESET + System.lineSeparator();
        }
        if (record.getLevel().intValue() >= Level.WARNING.intValue()) {
            return YELLOW + record.getLevel().getName() + ": " + message + RESET + System.lineSeparator();
        }
        return record.getLevel().getName() + ": " + message + System.lineSeparator();
    }
}