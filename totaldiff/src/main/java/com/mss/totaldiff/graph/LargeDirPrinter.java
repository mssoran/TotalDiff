package com.mss.totaldiff.graph;

import com.mss.totaldiff.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class LargeDirPrinter implements AnalyzeOutputGenerator {


    private TotalDiffConfig config;

    DuplicatesGraph duplicatesGraph = null;

    @Override
    public void processResult(DuplicateAnalyzer aAnalyzer, TotalDiffConfig aConfig) {
        throw new RuntimeException("not supported");
    }
    @Override
    public void processResult(DuplicatesGraph aDuplicatesGraph, TotalDiffConfig aConfig) {
        config = aConfig;
        duplicatesGraph = aDuplicatesGraph;
    }

    @Override
    public void printOutput() {
        // print ordered by the largest group that can save the most memory
        GraphDuplicateGroupNode[] groupArray = duplicatesGraph.getAllGraphDuplicateGroupNodes().toArray(new GraphDuplicateGroupNode[0]);
        Arrays.sort(groupArray, new SavedSizeComparator().reversed());
        for(int i = 0; i < config.printTopCount && i < groupArray.length; i++) {
            groupArray[i].prettyPrint();
        }

        printGraph(groupArray, config.printTopCount);
    }

    private boolean worthPrintingNode(GraphNode node) {
        return !config.ignorePrintingNodesInSinglePath ||
                (node != null && node.inpathCount > 1);
    }

    public void printGraph(GraphDuplicateGroupNode[] groupArray, int topCount) {
        if (config.graphOutputFile == null || config.graphOutputFile.isEmpty()) return;

        // We created what we want to write. Now write it to the file
        FileWriter fileWriter = null;
        BufferedWriter bufferedWriter = null;
        try {
            try {
                fileWriter = new FileWriter("ld"+config.graphOutputFile);
                bufferedWriter = new BufferedWriter(fileWriter);
                bufferedWriter.write("digraph {");
                bufferedWriter.newLine();
                bufferedWriter.write("overlap=false");
                bufferedWriter.newLine();


                // Write all nodes
                HashSet<Integer> printedNodeIds = new HashSet<>();
                for(int i = 0; i < topCount && i < groupArray.length; i++) {
                    GraphDuplicateGroupNode duplicateGroupNode = groupArray[i];
                    Iterator<GraphNode> it = duplicateGroupNode.parents.iterator();
                    while (it.hasNext()) {
                        GraphNode parentNode = it.next();
                        GraphNode prevNode = null;
                        GraphNode node = parentNode;
                        while (node != null && node.dirItem.notRoot()) {
                            // If prevNode is null, then we have to write this node, because this is a parent of duplicatenode
                            if (worthPrintingNode(node) || prevNode == null) {
                                if (!printedNodeIds.contains(node.dirItem.getId())) {
                                    double sizeRatio = ((double) node.duplicateSize) / node.dirItem.getTotalSize();
                                    String color;
                                    if ( node.duplicateSize == node.dirItem.getTotalSize()) {
                                        // ratio is %100, use a special color
                                        color = "#ff0000d0";
                                    } else if (sizeRatio > 0.9) {
                                        // linearly map (0.9, 1) into (0, 0xc0), so max alpha is 0xc0
                                        color = String.format("#ff6600%02X", (int)((sizeRatio - 0.9) * 10 * 0xc0));
                                    } else {
                                        // use a different color
                                        // linearly map (0, 0.9) into (0x20, 0xff), so min alpha is 0x20
                                        color = String.format("#fac507%02X", (int)(0x20 + (sizeRatio/0.9) * 0xdf));
                                    }
                                    bufferedWriter.write(String.format("n%d [label=\"%s\", duplicateSizeMb=%d, totalSize=%s, sizeRatio=%2.2f, style=filled, color=\"%s\"]",
                                            node.dirItem.getId(),
                                            node.toLabel(),
                                            node.duplicateSize / 1024 / 1024,
                                            node.dirItem.getTotalSize() / 1024 / 1024,
                                            sizeRatio,
                                            color
                                    ));
                                    bufferedWriter.newLine();
                                    printedNodeIds.add(node.dirItem.getId());
                                }
                                // write edge for path
                                if (prevNode != null) {
                                    bufferedWriter.write(String.format("n%d -> n%d [label=\"%d\", weight=%d, class=path]", node.dirItem.getId(), prevNode.dirItem.getId(), 1, 1));
                                    bufferedWriter.newLine();
                                }
                                prevNode = node;
                            }
                            node = duplicatesGraph.getParentNodeOf(node);
                        }
                        if (node != null && printedNodeIds.contains(node.dirItem.getId())) {
                            // then node is printed earlier, but still we need to add the edge
                            if (prevNode != null) {
                                bufferedWriter.write(String.format("n%d -> n%d [label=\"%d\", weight=%d, class=path]", node.dirItem.getId(), prevNode.dirItem.getId(), 1, 1));
                                bufferedWriter.newLine();
                            }
                        }
                    }
                    bufferedWriter.write(String.format("n%s [label=\"%s\", shape=rectangle, duplicateSizeMb=%d, totalSize=%s, sizeRatio=%2.2f,]",
                            duplicateGroupNode.duplicateInfoKey,
                            duplicateGroupNode.toLabel(),
                            duplicateGroupNode.duplicateSize / 1024 / 1024,
                            duplicateGroupNode.singleSize / 1024 / 1024,
                            ((double)duplicateGroupNode.duplicateSize) / duplicateGroupNode.singleSize
                    ));
                    bufferedWriter.newLine();
                }

                // Write final edges
                for(int i = 0; i < topCount && i < groupArray.length; i++) {
                    GraphDuplicateGroupNode duplicateGroupNode = groupArray[i];
                    Iterator<GraphNode> it = duplicateGroupNode.parents.iterator();
                    while (it.hasNext()) {
                        GraphNode parentNode = it.next();
                        bufferedWriter.write(String.format("n%d -> n%s [label=%d, weight=%d, class=duplicate]", parentNode.dirItem.getId(), duplicateGroupNode.duplicateInfoKey, 1, 1));
                        bufferedWriter.newLine();
                    }
                }

                bufferedWriter.write("}");
                bufferedWriter.newLine();
            } finally {
                if (bufferedWriter != null) bufferedWriter.close();
                if (fileWriter != null) fileWriter.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new RuntimeException("Failed to read from input file ");
        }
    }
}
