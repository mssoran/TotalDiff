package com.mss.totaldiff.graph;

import com.mss.totaldiff.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class LargeDirPrinter implements AnalyzeOutputGenerator {


    private class Node {
        public DirItem dirItem;
        public LinkedList<DuplicateGroupNode> duplicates = new LinkedList<>();

        public long duplicateSize = 0;
        public int duplicateFileCount = 0;
        public int inpathCount = 0;
        public Node(DirItem aDirItem) {
            dirItem = aDirItem;
        }
        public String toLabel() {
            return String.format("%s\nduplicate size %s\ntotal size %s\ndup/total %%%f",
                    dirItem.extractFileName(),
                    Utils.bytesToHumanReadable(duplicateSize),
                    Utils.bytesToHumanReadable(dirItem.getTotalSize()),
                    100 * ((double) duplicateSize) / dirItem.getTotalSize() );
        }
    }

    private class DuplicateGroupNode {
        public long duplicateSize = 0;
        public long singleSize = 0;
        public int duplicateFileCount = 0;
        public LinkedList<Node> parents = new LinkedList<>();

        public String duplicateInfoKey;
        public DuplicateGroupNode(String aDuplicateInfoKey) {
            duplicateInfoKey = aDuplicateInfoKey;
        }

        public void prettyPrint() {
            System.out.println("Key: " + duplicateInfoKey);
            System.out.println("    duplicateSize:" + duplicateSize + " singleSize:" + singleSize + " duplicateFileCount" + duplicateFileCount);
            System.out.println("    Expected savings:" + String.format("%s", Utils.bytesToHumanReadable(duplicateSize - singleSize)));
            System.out.println("    Directories:");
            Iterator<Node> it = parents.iterator();
            while(it.hasNext()) {
                System.out.println("        "+it.next().dirItem.extractFileName());
            }
        }
        public String toLabel() {
            return String.format("%s\nsingle size %s\nduplicate size %s\nsingle/duplicate %%%f",
                    duplicateInfoKey,
                    Utils.bytesToHumanReadable(singleSize),
                    Utils.bytesToHumanReadable(duplicateSize),
                    100 * ((double) singleSize) / duplicateSize );
        }
    }

    private DuplicateAnalyzer analyzer;
    private TotalDiffConfig config;


    private HashMap<String, DuplicateGroupNode> duplicateNodes = new HashMap<>();
    private HashMap<String, Node> nodes = new HashMap<>();

    private DuplicateGroupNode ensureDuplicateNode(DuplicateInfo duplicateInfo) {
        // normalize the name for this duplicateInfo
        // if all the parent dirs are the same for two different duplicateInfo objects, we want them to be in the same Node
        Iterator<FileItem> it = duplicateInfo.getFiles().iterator();
        int[] ids = new int [duplicateInfo.getFiles().size()];
        int index = 0;
        while (it.hasNext()) ids[index++] = it.next().getParentDir().getId();
        Arrays.sort(ids);
        StringBuilder sb = new StringBuilder();
        sb.append("dup_");
        sb.append(ids[0]);
        for(index = 1; index < ids.length; index++) {
            sb.append("_");
            sb.append(ids[index]);
        }

        String key = sb.toString();
        DuplicateGroupNode node = duplicateNodes.get(key);
        if (node == null) {
            node = new DuplicateGroupNode(key);
            duplicateNodes.put(key, node);
            it = duplicateInfo.getFiles().iterator();
            while (it.hasNext()) {
                node.parents.addLast(ensurePathNode(it.next().getParentDir()));
                node.duplicateFileCount++;
            }
        }
        node.singleSize += duplicateInfo.getFiles().getFirst().size;
        return node;
    }


    private Node ensurePathNode(DirItem dirItem) {
        String key = Integer.toString(dirItem.getId());
        Node node = nodes.get(key);
        if (node == null) {
            node = new Node(dirItem);
            nodes.put(key, node);
            if (config.includePathInGraph && dirItem.getParentDir().getId() != -1) {
                ensurePathNode(dirItem.getParentDir());
            }
        }
        node.inpathCount ++;

        return node;
    }

    @Override
    public void processResult(DuplicateAnalyzer aAnalyzer, TotalDiffConfig aConfig) {
               analyzer = aAnalyzer;
        config = aConfig;

        // TODO must be called only once. should I check? what?

        for (DuplicateInfo info : analyzer.getDuplicateMap().values()) {
            FileItem[] list = info.getFiles().toArray(new FileItem[0]);
            if(list[0].size < config.weightThreshold) continue;

            DuplicateGroupNode duplicateGroupNode = ensureDuplicateNode(info);
            for (int i = 0; i < list.length; i++) {
                FileItem fileItem = list[i];
                Node node = nodes.get(Integer.toString(fileItem.getParentDir().getId()));

                duplicateGroupNode.duplicateSize += fileItem.size;

                node.duplicateSize += fileItem.size;
                { // here we update duplicateSize of all the path
                    String key = Integer.toString(node.dirItem.getParentDir().getId());
                    Node parentNode = nodes.get(key);
                    while(parentNode != null) {
                        parentNode.duplicateSize += fileItem.size;
                        key = Integer.toString(parentNode.dirItem.getParentDir().getId());
                        parentNode = nodes.get(key);
                    }
                }
                node.duplicateFileCount ++;
                node.duplicates.addLast(duplicateGroupNode);
            }
        }

    }

    public class GroupSavedSizeComparator implements Comparator<DuplicateGroupNode> {

        @Override
        public int compare(DuplicateGroupNode first, DuplicateGroupNode second) {
            long firstSave = first.duplicateSize - first.singleSize;
            long secondSave = second.duplicateSize - second.singleSize;
            if (firstSave > secondSave) return 1;
            else if (firstSave < secondSave) return -1;
            else return 0;
        }
    }

    @Override
    public void printOutput() {
        // print ordered by the largest group that can save the most memory
        DuplicateGroupNode[] groupArray = duplicateNodes.values().toArray(new DuplicateGroupNode[0]);
        Arrays.sort(groupArray, new GroupSavedSizeComparator().reversed());
        for(int i = 0; i < config.printTopCount && i < groupArray.length; i++) {
            groupArray[i].prettyPrint();
        }

        printGraph(groupArray, config.printTopCount);
    }

    private boolean worthPrintingNode(Node node) {
        return !config.ignorePrintingNodesInSinglePath ||
                (node != null && node.inpathCount > 1);
    }

    public void printGraph(DuplicateGroupNode[] groupArray, int topCount) {
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
                    DuplicateGroupNode duplicateGroupNode = groupArray[i];
                    Iterator<Node> it = duplicateGroupNode.parents.iterator();
                    while (it.hasNext()) {
                        Node parentNode = it.next();
                        Node prevNode = null;
                        Node node = parentNode;
                        while (node != null && node.dirItem.getId() != -1) {
                            // If prevNode is null, then we have to write this node, because this is a parent of duplicatenode
                            if (worthPrintingNode(node) || prevNode == null) {
                                if (!printedNodeIds.contains(node.dirItem.getId())) {
                                    bufferedWriter.write(String.format("n%d [label=\"%s\", duplicateSizeMb=%d, totalSize=%s, sizeRatio=%2.2f]",
                                            node.dirItem.getId(),
                                            node.toLabel(),
                                            node.duplicateSize / 1024 / 1024,
                                            node.dirItem.getTotalSize() / 1024 / 1024,
                                            ((double) node.duplicateSize) / node.dirItem.getTotalSize()
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
                            node = nodes.get(Integer.toString(node.dirItem.getParentDir().getId()));
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
                    DuplicateGroupNode duplicateGroupNode = groupArray[i];
                    Iterator<Node> it = duplicateGroupNode.parents.iterator();
                    while (it.hasNext()) {
                        Node parentNode = it.next();
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
