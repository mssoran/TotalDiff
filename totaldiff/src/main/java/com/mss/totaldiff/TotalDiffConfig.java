package com.mss.totaldiff;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.logging.Logger;

public class TotalDiffConfig {

    public enum RunningMode {
        None,
        BuildIndex,
        Analyze,
        Diff
    }

    private static Logger logger = Logger.getLogger(TotalDiffConfig.class.getName());

    public RunningMode runningMode = RunningMode.None;

    public String[] dirs = new String[0];

    public boolean compareHashes = true;
    public long maxSizeForHashComparison = 8 * 1024 * 1024;
    public boolean printDuplicates = false;

    // default buffer size
    public int fileReadBufferSize = 128 * 1024;
    //public int considerFileSize = 100 * (2 << 10);
    public int considerFileSize = 0;
    public String[] considerDirExcludes = new String[] {
            ".git",
            "@eaDir",
            ".@__thumb",
            "archivedupDest",
    };

    public String infoTreeOutputFileName = null;

    public String infoTreeInputFileName = null;
    public String infoTreeSecondaryInputFileName = null;
    public boolean overwriteInfoTreeOutputFile = false;


    // Config for graph generation and printing
    public boolean includePathInGraph = true;
    public boolean ignorePrintingNodesInSinglePath = true;

    public String graphOutputFile = "graphOut.dot";

    public int printTopCount = 4;

    public boolean incrementalAddDirs = true;

    public String interestedPath = null;

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

    private static String usage = (new StringBuilder()).append("")
            .append("usage:  <running mode> [Mode Options...]\n")
            .append("    Running mode can be one of:\n")
            .append("        buildindex\n")
            .append("        analyze\n")
            .append("        diff\n")
            .append("\n")
            .append("    Each mode has its own list of options. See below for details.\n")
            .append("\n")
            .append("\n")
            .append("    buildindex: Builds an index file to use for the other commands. It recursively crawls the\n")
            .append("                given directories, and collects information for each file.\n")
            .append("       --infoTreeInputFileName=<input file name>\n")
            .append("                Name of the input file, that's previously generated with buildindex command.\n")
            .append("                Files in this input is used as the basis. If the index building is incremental, \n")
            .append("                all files in the input are used and new files are added from new dirs. If index\n")
            .append("                building is not incremental, files in the input are used as lookup for expensive\n")
            .append("                calculations, like hashing. Instead of hashing again, value in the input is used.\n")
            .append("                Can be set empty, so no input is used. Default value is empty.\n")
            .append("       --dirs=<dir list>\n")
            .append("                List of directories to scan. All the files under each of these directories will be\n")
            .append("                included in the generated output file recursively. It's a list of directory names\n")
            .append("                separated by ':' character. Default value is empty\n")
            .append("       --[no]incrementalAddDirs\n")
            .append("                Determines how to use infoTreeInputFileName. If true, uses all the files in the \n")
            .append("                input file, and adds any other new file it finds in the directories scanned. If\n")
            .append("                false, uses input file only as a lookup for hash values. The output only contains\n")
            .append("                files from scanned directories.\n")
            .append("                incrementalAddDirs is useful when adding new directories to an existing built file\n")
            .append("                noincrementalAddDirs is useful when re-generating build file. In this case, deleted\n")
            .append("                files will not be in the new output, not changed files will be used from the input\n")
            .append("                and only new files will be hashed.\n")
            .append("       --infoTreeOutputFileName=<output file name>\n")
            .append("                Index file generated. All the found files will be written to this output.\n")
            .append("                Can be set empty, so no input is used. Default value is empty.\n")
            .append("       --[no]overwriteInfoTreeOutputFile\n")
            .append("                If true, overwrites the output file when it exists. If false, fails with an error\n")
            .append("                when output file already exists. Default value is false.\n")
            .append("       --[no]compareHashes\n")
            .append("                A boolean option. If true, uses file content for comparison. Default value is\n")
            .append("                true. For efficiency, only part of the content is used.\n")
            .append("                See --maxSizeForHashComparison.\n")
            .append("       --maxSizeForHashComparison=<size in bytes>\n")
            .append("                When calculating hash of a file, at most maxSizeForHashComparison bytes from the\n")
            .append("                beginning of the file is used. Default value is 8 MB\n")
            .append("       --fileReadBufferSize=<size in bytes>\n")
            .append("                Size of the buffer when reading a file. Default value is 128 KB. It's used during\n")
            .append("                hash calculation.\n")
            .append("       --considerFileSize=<size in bytes>\n")
            .append("                If size of a file is less than considerFileSize, the file is skipped, and it will\n")
            .append("                not be included in the index (or any computation). Default value is 0. This option\n")
            .append("                can be used to exclude small files.\n")
            .append("       --considerDirExcludes=<dir list>\n")
            .append("                This option can bu used to exclude directories/files with a specific name. It's a\n")
            .append("                list of names separated by ':' character. Default value contains some dirs like\n")
            .append("                .git\n")
            .append("\n")
            .append("    analyze: Analyzes the given input file (previously generated file with buildindex) and\n")
            .append("             generates corresponding output. *Details are still in progress*. Currently generates\n")
            .append("             a graph for the top printTopCount duplicated directories.\n")
            .append("       --infoTreeInputFileName=<input file name>\n")
            .append("             Data in this file is used for the analyze. See definition in buildindex\n")
            .append("       --graphOutputFile=<output file name>\n")
            .append("             Name of the output file for the generated graph.\n")
            .append("       --considerFileSize=<size in bytes>\n")
            .append("             See definition in buildindex\n")
            .append("       --considerDirExcludes=<dir list>\n")
            .append("             See definition in buildindex\n")
            .append("       --interestedPath=<full path prefix>\n")
            .append("             The output will include duplicates where at least one of the duplicated paths\n")
            .append("             start with the given interestedPath.\n")
            .append("       --[no]printDuplicates\n")
            .append("             If true, outputs a human readable list of all the duplicated files found. Default\n")
            .append("             value is false.\n")
            .append("       --[no]includePathInGraph\n")
            .append("             Work in progress.\n")
            .append("       --[no]ignorePrintingNodesInSinglePath\n")
            .append("             Work in progress.\n")
            .append("       --printTopCount=<count>\n")
            .append("             Generate the graph using the largest <count> duplicate groups.\n")
            .append("\n")
            .append("    diff:    Compares two given input files, and prints all the files in the first input, but not\n")
            .append("             in the second input.\n")
            .append("       --infoTreeInputFileName=<input file name>\n")
            .append("             Data in this file is used for the diff as the first input. See definition in buildindex\n")
            .append("       --infoTreeSecondaryInputFileName=<input file name>\n")
            .append("             Data in this file is used for the diff as the second input. Similar to\n")
            .append("             infoTreeInputFileName\n")
            .append("       --considerFileSize=<size in bytes>\n")
            .append("             See definition in buildindex\n")
            .append("       --considerDirExcludes=<dir list>\n")
            .append("             See definition in buildindex\n")
            .append("\n")
            .append("\n")
            .toString();

    private void setRunningMode(ArgIterator argIterator) {
        if (!argIterator.hasNext()) {
            throw new RuntimeException("Running mode is not specified!");
        }
        String runningModeStr = argIterator.next();

        switch (runningModeStr) {
            case "buildindex":
                runningMode = RunningMode.BuildIndex;
                readBuildIndexArgs(argIterator);
                break;
            case "analyze":
                runningMode = RunningMode.Analyze;
                readAnalyzeArgs(argIterator);
                break;
            case "diff":
                runningMode = RunningMode.Diff;
                readDiffArgs(argIterator);
                break;
            default:
                throw new RuntimeException("Unknown running mode:" + runningModeStr);
        }
    }

    public void printUsage() {
        System.out.println(usage);
    }

    private void readBuildIndexArgs(ArgIterator argIterator) {
        while (argIterator.hasNext()) {
            int remainingArgCount = argIterator.getRemainingArgCount();
            if (!argIterator.peek().startsWith("--")) {
                throw new RuntimeException("Unknown arg: " + argIterator.next());
            }
            argIterator.updateBooleanValue("compareHashes", b -> compareHashes = b);
            argIterator.updateLongValue("maxSizeForHashComparison", x -> maxSizeForHashComparison = x);
            argIterator.updateIntValue("fileReadBufferSize", x -> fileReadBufferSize = x);
            argIterator.updateIntValue("considerFileSize", x -> considerFileSize = x);
            argIterator.updateStringArrayValue("considerDirExcludes", excludedDirs -> considerDirExcludes = excludedDirs);

            argIterator.updateStringValue("infoTreeInputFileName", x -> infoTreeInputFileName = x);
            argIterator.updateStringValue("infoTreeOutputFileName", x -> infoTreeOutputFileName = x);
            argIterator.updateBooleanValue("overwriteInfoTreeOutputFile", b -> overwriteInfoTreeOutputFile = b);

            argIterator.updateBooleanValue("incrementalAddDirs", b -> incrementalAddDirs = b);

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
                throw new RuntimeException("Unknown argument, buildindex cannot consume argument '" + argIterator.peek() + "'");
            }
        }
    }
    private void readAnalyzeArgs(ArgIterator argIterator) {
        while (argIterator.hasNext()) {
            int remainingArgCount = argIterator.getRemainingArgCount();
            if (!argIterator.peek().startsWith("--")) {
                throw new RuntimeException("Unknown arg: " + argIterator.next());
            }
            argIterator.updateStringValue("infoTreeInputFileName", x -> infoTreeInputFileName = x);

            argIterator.updateIntValue("considerFileSize", x -> considerFileSize = x);
            argIterator.updateStringArrayValue("considerDirExcludes", excludedDirs -> considerDirExcludes = excludedDirs);

            argIterator.updateBooleanValue("printDuplicates", b -> printDuplicates = b);

            argIterator.updateBooleanValue("includePathInGraph", b -> includePathInGraph = b);
            argIterator.updateBooleanValue("ignorePrintingNodesInSinglePath ", b -> ignorePrintingNodesInSinglePath = b);
            argIterator.updateStringValue("graphOutputFile", x -> graphOutputFile = x);
            argIterator.updateIntValue("printTopCount", x -> printTopCount = x);

            argIterator.updateStringValue("interestedPath", x -> interestedPath = x);

            if (remainingArgCount == argIterator.getRemainingArgCount()) {
                // This means I'm not able to consume the arg
                throw new RuntimeException("Unknown argument, analyze cannot consume argument '" + argIterator.peek() + "'");
            }
        }
    }

    private void readDiffArgs(ArgIterator argIterator) {
        while (argIterator.hasNext()) {
            int remainingArgCount = argIterator.getRemainingArgCount();
            if (!argIterator.peek().startsWith("--")) {
                throw new RuntimeException("Unknown arg: " + argIterator.next());
            }
            argIterator.updateStringValue("infoTreeInputFileName", x -> infoTreeInputFileName = x);
            argIterator.updateStringValue("infoTreeSecondaryInputFileName", x -> infoTreeSecondaryInputFileName = x);

            argIterator.updateIntValue("considerFileSize", x -> considerFileSize = x);
            argIterator.updateStringArrayValue("considerDirExcludes", excludedDirs -> considerDirExcludes = excludedDirs);

            if (remainingArgCount == argIterator.getRemainingArgCount()) {
                // This means I'm not able to consume the arg
                throw new RuntimeException("Unknown argument, diff cannot consume argument '" + argIterator.peek() + "'");
            }
        }
    }

    public void parseArgs(String [] args) {
        ArgIterator argIterator = new ArgIterator(args);
        setRunningMode(argIterator);
    }
}
