package com.mss.totaldiff;

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;

class ArgIterator implements Iterator<String> {

    private static Logger logger = Logger.getLogger(TotalDiffConfig.class.getName());

    private String[] args;
    private int currentIndex;

    public ArgIterator(String[] aArgs) {
        args = aArgs;
        if (args == null) args = new String[0];
        currentIndex = 0;
    }

    public String peek() {
        return args[currentIndex];
    }

    public int getRemainingArgCount() {
        if (!hasNext()) return 0;
        return args.length - currentIndex;
    }

    @Override
    public boolean hasNext() {
        return currentIndex < args.length;
    }

    @Override
    public String next() {
        return args[currentIndex++];
    }


    private <T> boolean updateGenericValue(String configName, Consumer<T> f, Function<String, T> converter) {
        if (!hasNext()) return false;
        if (peek().startsWith("--" + configName +"=")) {
            String[] parts = next().split("=", 2);
            logger.info("Setting " + configName + " to " + parts[1]);
            f.accept(converter.apply(parts[1]));
            return true;
        }
        return false;
    }

    public boolean updateIntValue(String configName, Consumer<Integer> f) {
        return updateGenericValue(configName, f, s -> Integer.valueOf(s));
    }

    public boolean updateLongValue(String configName, Consumer<Long> f) {
        return updateGenericValue(configName, f, s -> Long.valueOf(s));
    }

    public boolean updateStringValue(String configName, Consumer<String> f) {
        return updateGenericValue(configName, f, s -> s);
    }

    public boolean updateStringArrayValue(String configName, Consumer<String[]> f) {
        return updateGenericValue(configName, f, s -> s.split(":"));
    }

    public boolean updateBooleanValue(String configName, Consumer<Boolean> f) {
        if (!hasNext()) return false;
        if (peek().equals("--" + configName)) {
            next();
            logger.info("Setting " + configName + " to true");
            f.accept(true);
            return true;
        } else if (peek().equals("--no" + configName)) {
            next();
            logger.info("Setting " + configName + " to false");
            f.accept(false);
            return true;
        }
        return false;
    }
}
