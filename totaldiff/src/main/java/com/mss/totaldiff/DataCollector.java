package com.mss.totaldiff;

import com.mss.totaldiff.visitors.ItemVisitor;
import com.mss.totaldiff.visitors.JsonSerializerVisitor;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.logging.Logger;

public class DataCollector {
    private Logger logger = Logger.getLogger(DataCollector.class.getName());
    private InfoTree infoTree;
    private TotalDiffConfig config;

    public DataCollector(TotalDiffConfig aConfig) {
        config = aConfig;
        infoTree = new InfoTree(config);
    }

    public void constructTree(String inputFileName) throws IOException {
        ItemVisitor fileSerializerVisitor = null;
        try {
            // build visitors
            LinkedList<ItemVisitor> visitors = new LinkedList<>();

            if (config.infoTreeOutputFileName != null && !config.infoTreeOutputFileName.isEmpty()) {
                try {
                    File outputFile = new File(config.infoTreeOutputFileName);
                    if (!config.overwriteInfoTreeOutputFile && outputFile.exists())
                        throw new RuntimeException("Output file already exists:" + outputFile.getAbsolutePath());
                    //fileSerializerVisitor = new FileSerializerVisitor(outputFile);
                    fileSerializerVisitor = new JsonSerializerVisitor(outputFile);
                    visitors.addLast(fileSerializerVisitor);
                } catch (IOException ex) {
                    throw new RuntimeException("Output file problem!" + ex);
                }
            }

            InfoTree lookupInfoTree = null;
            if (config.incrementalAddDirs) {
                // If it's incremental, read from file, and add anything new from dirs
                // FileSerializerVisitor.addFromFile(inputFileName, visitors, infoTree);
                JsonSerializerVisitor.addFromFile(inputFileName, visitors, infoTree);
            } else {
                // If not incremental, then use the input file only for a shortcut. If any file
                // already exists in the input file (path, name and size matches), we can
                // reuse hash.
                lookupInfoTree = new InfoTree(config);
                JsonSerializerVisitor.addFromFile(inputFileName, new LinkedList<>(), lookupInfoTree);
            }

            long startTime = System.nanoTime();
            for (String topDir : config.dirs) {
                logger.info("Ready to add " + topDir);
                infoTree.addToRoot(topDir, visitors, lookupInfoTree);
            }
            logger.info(String.format("Total time spend for adding dirs is %.3f sec", (double)(System.nanoTime() - startTime)/1_000_000_000));
        } finally {
            if (fileSerializerVisitor != null) fileSerializerVisitor.close();
        }


        logger.info("InfoTree is generated. Number of items:" + infoTree.itemCount());
        infoTree.logSummary();
        //infoTree.printAll();
    }

    public InfoTree getInfoTree() {
        return infoTree;
    }
}
