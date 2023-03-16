package com.mss.totaldiff;

import com.mss.totaldiff.graph.AnalyzeOutputGenerator;
import com.mss.totaldiff.graph.LargeDirPrinter;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class Main {
    private static Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        InputStream logPropertiesStream;
        try {
            // try to read log config file from the working dir
            logPropertiesStream = new FileInputStream("log.properties");
        } catch(IOException e) {
            // ignore the exception
            logPropertiesStream = null;
        }
        if (logPropertiesStream == null) {
            // If there is not log config file, read the default from the jar
            logPropertiesStream = Main.class.getResourceAsStream("/totaldiff/resources/log.properties");
        }

        if (logPropertiesStream != null) {
            try {
            LogManager.getLogManager().readConfiguration(logPropertiesStream);
            } catch(IOException e) {
                e.printStackTrace();
            }
        }

        try {
            TotalDiffConfig config = new TotalDiffConfig();
            config.parseArgs(args);

            logger.info("Arguments are parsed");
            DataCollector collector = new DataCollector(config);
            collector.constructTree(config.infoTreeInputFileName);

            if (config.infoTreeSecondaryInputFileName != null && !config.infoTreeSecondaryInputFileName.trim().isEmpty()) {
                // TODO should skip dirs
                DataCollector secondCollector = new DataCollector(config);
                secondCollector.constructTree(config.infoTreeSecondaryInputFileName);
                logger.info(String.format("Filtering all the files from '%s', those are in '%s'", config.infoTreeSecondaryInputFileName, config.infoTreeInputFileName));
                secondCollector.getInfoTree().printFilesNotIn(collector.getInfoTree());
            }

            if (config.analyze) {
                logger.info("Analyze...");
                DuplicateAnalyzer analyzer = new DuplicateAnalyzer();
                analyzer.analyze(collector.getInfoTree());
                if (config.printDuplicates) {
                    analyzer.printAllDuplicates();
                }

                //AnalyzeOutputGenerator gg = new DuplicatesAsEdgeGraphGenerator();
                //AnalyzeOutputGenerator gg = new DuplicatesAsNodeGraphGenerator();
                AnalyzeOutputGenerator gg = new LargeDirPrinter();

                gg.processResult(analyzer, config);
                gg.printOutput();
            } else {
                logger.info("Skipping Analyze");
            }

            logger.info(String.format("Total time spend for hash computation is %.3f", (double)FileItem.totalHashTimeNanos/1_000_000_000));
        } catch (Throwable t) {
            logger.severe("Unexpected error:" + t);
            t.printStackTrace();
            throw new RuntimeException("unexpected exception: " + t);
        }

    }
}
