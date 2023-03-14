package com.mss.totaldiff;

public class Utils {
    public static String bytesToHumanReadable(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";

        } else if (bytes < 1024L * 1024) {
            return String.format("%.2f KB", (double)bytes / 1024);

        } else if (bytes < 1024L * 1024 * 1024) {
            return String.format("%.2f MB", (double)bytes / 1024 / 1024);

        } else if (bytes < 1024L * 1024 * 1024 * 1024) {
            return String.format("%.2f GB", (double)bytes / 1024 / 1024 / 1024);

        } else {
            return String.format("%.2f TB", (double)bytes / 1024 / 1024 / 1024 / 1024);
        }
    }
}
