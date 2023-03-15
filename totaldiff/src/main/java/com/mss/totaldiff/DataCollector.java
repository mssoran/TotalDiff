package com.mss.totaldiff;

import com.mss.totaldiff.visitors.FileSerializerVisitor;
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

            // FileSerializerVisitor.addFromFile(inputFileName, visitors, infoTree);
            JsonSerializerVisitor.addFromFile(inputFileName, visitors, infoTree);

            long startTime = System.nanoTime();
            for (String topDir : config.dirs) {
                logger.info("Ready to add " + topDir);
                infoTree.addToRoot(topDir, visitors);
            }
            logger.info(String.format("Total time spend for adding dirs is %.3f sec", (double)(System.nanoTime() - startTime)/1_000_000_000));
        } finally {
            if (fileSerializerVisitor != null) fileSerializerVisitor.close();
        }


        logger.info("InfoTree is generated. Number of items:" + infoTree.itemCount());
        //infoTree.printAll();
    }

    public InfoTree getInfoTree() {
        return infoTree;
    }
}
