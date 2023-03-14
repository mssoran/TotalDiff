package com.mss.totaldiff;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.logging.Logger;

public class TotalDiffConfig {
    private static Logger logger = Logger.getLogger(TotalDiffConfig.class.getName());

    public String[] dirs = new String[0];

    public boolean compareHashes = true;
    public boolean analyze = false;
    public boolean printDuplicates = false;

    // default buffer size
    public int fileReadBufferSize = 128 * 1024;
    public int considerFileSize = 100 * (2 << 10);
    public String[] considerDirExcludes = new String[] {
            ".git",
            "@eaDir",
            "archivedupDest",
    };

    //public String infoTreeOutputFileName = "infotreeoutputfile.txt";
    public String infoTreeOutputFileName = null;

    public String infoTreeInputFileName = "infotreeinputfile.txt";
    //public String infoTreeInputFileName = "infotreeoutputfile.txt";
    //public String infoTreeInputFileName = null;
    public String infoTreeSecondaryInputFileName = null;
    public boolean overwriteInfoTreeOutputFile = false;


    // Config for graph generation and printing
    public int weightThreshold = 10;
    public boolean includePathInGraph = true;
    public boolean ignorePrintingNodesInSinglePath = true;

    public String graphOutputFile = "graphOut.dot";

    public int printTopCount = 4;

    private static String[] cleanDirs(LinkedList<File> readDirs) {
        File[] rawDirs = readDirs.toArray(new File[0]);
        if (rawDirs.length == 0) return new String[0];

        String[] sortedDirs = new String[rawDirs.length];
        for (int i = 0; i < rawDirs.length; i++) {
            sortedDirs[i] = rawDirs[i].getAbsolutePath();
        }
        Arrays.sort(sortedDirs);
        int dirCount = 1;
        for (int i = 1; i < sortedDirs.length; i++) {
            if (!sortedDirs[i].startsWith(sortedDirs[dirCount - 1])) {
                sortedDirs[dirCount++] = sortedDirs[i];
            }
        }
        if (dirCount == sortedDirs.length) return sortedDirs;
        return Arrays.copyOf(sortedDirs, dirCount);
    }

    public void parseArgs(String [] args) {

        ArgIterator argIterator = new ArgIterator(args);
        while (argIterator.hasNext()) {
            logger.info("Processing arg '" + argIterator.peek() + "'");
            int remainingArgCount = argIterator.getRemainingArgCount();
            if (!argIterator.peek().startsWith("--")) {
                throw new RuntimeException("Unknown arg: " + argIterator.next());
            }
            argIterator.updateBooleanValue("compareHashes", b -> compareHashes = b);
            argIterator.updateBooleanValue("analyze", b -> analyze = b);
            argIterator.updateBooleanValue("printDuplicates", b -> printDuplicates = b);

            argIterator.updateIntValue("fileReadBufferSize", x -> fileReadBufferSize = x);
            argIterator.updateIntValue("considerFileSize", x -> considerFileSize = x);
            argIterator.updateStringArrayValue("considerDirExcludes", excludedDirs -> considerDirExcludes = excludedDirs);

            argIterator.updateStringValue("infoTreeOutputFileName", x -> infoTreeOutputFileName = x);
            argIterator.updateStringValue("infoTreeInputFileName", x -> infoTreeInputFileName = x);
            argIterator.updateStringValue("infoTreeSecondaryInputFileName", x -> infoTreeSecondaryInputFileName = x);
            argIterator.updateBooleanValue("overwriteInfoTreeOutputFile", b -> overwriteInfoTreeOutputFile = b);

            argIterator.updateIntValue("weightThreshold", x -> weightThreshold = x);
            argIterator.updateBooleanValue("includePathInGraph", b -> includePathInGraph = b);
            argIterator.updateBooleanValue("ignorePrintingNodesInSinglePath ", b -> ignorePrintingNodesInSinglePath = b);
            argIterator.updateStringValue("graphOutputFile", x -> graphOutputFile = x);
            argIterator.updateIntValue("printTopCount", x -> printTopCount = x);

            argIterator.updateStringArrayValue("dirs", d -> {
                LinkedList<File> dirsList = new LinkedList<>();
                for (int i = 0; i < d.length; i++) {
                    File dir = new File(d[i]);
                    if (!dir.isDirectory()) {
                        throw new RuntimeException("Arg is not a directory: " + d[i]);
                    } else {
                        dirsList.addLast(dir);
                    }
                }
                dirs = cleanDirs(dirsList);
            });

            if (remainingArgCount == argIterator.getRemainingArgCount()) {
                // This means I'm not able to consume the arg
                throw new RuntimeException("Unknown argument, cannot consume argument '" + argIterator.peek() + "'");
            }
        }
    }
}
