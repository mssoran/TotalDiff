package com.mss.totaldiff.graph;

import com.mss.totaldiff.*;

import java.io.*;
import java.util.*;

public class DuplicatesAsNodeGraphGenerator implements AnalyzeOutputGenerator {
    private class Node {
        public DirItem dirItem = null;
        public String duplicateInfoKey = null;
        public int inpathCount = 0;

        public long duplicateSize = 0;
        public long singleSize = 0;
        public Node(DirItem aDirItem) {
            dirItem = aDirItem;
        }

        public Node(String aDuplicateInfoKey) {
            duplicateInfoKey = aDuplicateInfoKey;
        }
    }

    private class Edge {
        Node src;
        Node dst;
        public int weight = 0;
        public Edge (Node aSrc, Node aDst) {
            src = aSrc;
            dst = aDst;
        }

        public void incWeight() {
            weight++;
        }
    }

    private DuplicateAnalyzer analyzer;
    private TotalDiffConfig config;


    private HashMap<String, Node> nodes = new HashMap<>();
    private HashMap<String, Edge> edges = new HashMap<>();

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

    private Node ensureDuplicateNode(DuplicateInfo duplicateInfo) {
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
        Node node = nodes.get(key);
        if (node == null) {
            node = new Node(key);
            nodes.put(key, node);
        }
        return node;
    }

//    private Edge ensurePathEdge(Node aSrc, Node aDst) {
//        Node src = aSrc;
//        Node dst = aDst;
//        if (aSrc.dirItem.getId() > aDst.dirItem.getId()) {
//            src = aDst;
//            dst = aSrc;
//        }
//        String edgeKey = src.dirItem.getId() + "_" + dst.dirItem.getId();
//        Edge edge = edges.get(edgeKey);
//        if (edge == null) {
//            edge = new Edge(src, dst);
//            edges.put(edgeKey, edge);
//        }
//        // update attributes
//        edge.incWeight();
//        return edge;
//    }

    private Edge ensureEdgeToDuplicate(Node aSrc, Node aDst) {
        String edgeKey = aSrc.dirItem.getId() + "_" + aDst.duplicateInfoKey;
        Edge edge = edges.get(edgeKey);
        if (edge == null) {
            edge = new Edge(aSrc, aDst);
            edges.put(edgeKey, edge);
        }
        return edge;
    }

    @Override
    public void processResult(DuplicateAnalyzer aAnalyzer, TotalDiffConfig aConfig) {
        analyzer = aAnalyzer;
        config = aConfig;

        // TODO must be called only once. should I check? what?

        for (DuplicateInfo info : analyzer.getDuplicateMap().values()) {
            FileItem[] list = info.getFiles().toArray(new FileItem[0]);

            Node duplicateNode = ensureDuplicateNode(info);
            duplicateNode.singleSize += list[0].size;
            for (int i = 0; i < list.length; i++) {
                duplicateNode.duplicateSize += list[i].size;
                Node node = ensurePathNode(list[i].getParentDir());
                node.inpathCount ++;
                ensureEdgeToDuplicate(node, duplicateNode).incWeight();

                node.duplicateSize += list[i].size;
                { // here we update duplicateSize of all the path
                    String key = Integer.toString(node.dirItem.getParentDir().getId());
                    Node parentNode = nodes.get(key);
                    while(parentNode != null) {
                        parentNode.duplicateSize += list[i].size;
                        key = Integer.toString(parentNode.dirItem.getParentDir().getId());
                        parentNode = nodes.get(key);
                    }
                }
            }
        }
    }

    private boolean worthPrintingNode(Node node) {
        return !config.ignorePrintingNodesInSinglePath ||
                (node != null && node.inpathCount > 1);
    }

    @Override
    public void printOutput() {
        if (config.graphOutputFile == null || config.graphOutputFile.isEmpty()) return;

        // We created what we want to write. Now write it to the file
        FileWriter fileWriter = null;
        BufferedWriter bufferedWriter = null;
        try {
            try {
                fileWriter = new FileWriter(config.graphOutputFile);
                bufferedWriter = new BufferedWriter(fileWriter);
                bufferedWriter.write("digraph {");
                bufferedWriter.newLine();

                HashSet<Integer> printedNodeIds = new HashSet<>();

                // Write all nodes
                for (Node node : nodes.values()) {
                    if (node.dirItem != null) {
                        if (worthPrintingNode(node)) {
                            bufferedWriter.write(String.format("n%d [label=\"%s\", duplicateSizeMb=\"%d\", fontsize=14.0]", node.dirItem.getId(), node.dirItem.extractFileName(), node.duplicateSize / 1024 / 1024)); //node.dirItem.name));
                            bufferedWriter.newLine();
                            printedNodeIds.add(node.dirItem.getId());
                        }
                    } else {
                        bufferedWriter.write(String.format("n%s [label=\"%s\", duplicateSizeMb=\"%d\", shape=rectangle]", node.duplicateInfoKey, "canbefilelist_"+node.duplicateInfoKey, node.duplicateSize / 1024 / 1024)); //node.dirItem.name));
                        bufferedWriter.newLine();
                    }
                }

                // Write edges
                for (Node node : nodes.values()) {
                    if (node.dirItem != null && printedNodeIds.contains(node.dirItem.getId())) {
                        DirItem parent = node.dirItem.getParentDir();
                        int weight = 1;
                        // find the first printed parent
                        while (parent != null && parent.getId() != -1 && !printedNodeIds.contains(parent.getId())) {
                            parent = parent.getParentDir();
                            weight++;
                        }
                        if (printedNodeIds.contains(parent.getId())) {
                            bufferedWriter.write(String.format("n%d -> n%d [label=\"%d\", weight=%d]", parent.getId(), node.dirItem.getId(), weight, weight));
                            bufferedWriter.newLine();
                        }
                    }
                }
                for (Edge edge : edges.values()) {
                    if(edge.dst.duplicateInfoKey != null) {
                        bufferedWriter.write(String.format("n%d -> n%s [label=\"%d\", weight=%d]", edge.src.dirItem.getId(), edge.dst.duplicateInfoKey, edge.weight, edge.weight));
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
