package com.mss.totaldiff.graph;


import com.mss.totaldiff.*;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

public class DuplicatesAsEdgeGraphGenerator implements AnalyzeOutputGenerator {

    private class Node {
        public DirItem dirItem;

        public long duplicateSize = 0;
        public Node(DirItem aDirItem) {
            dirItem = aDirItem;
        }
    }

    private class Edge {
        Node src;
        Node dst;
        public int weight = 0;
        public long duplicateSize = 0;
        public HashSet<String> extensions = new HashSet<>();
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

    private HashMap<Integer, Node> nodes = new HashMap<>();
    private HashMap<String, Edge> edges = new HashMap<>();

    public DuplicatesAsEdgeGraphGenerator() {

    }

    private Node ensureNode(DirItem fileItem, HashMap<Integer, Node> nodes) {
        Integer key = fileItem.getId();
        Node node = nodes.get(key);
        if (node == null) {
            node = new Node(fileItem);
            nodes.put(key, node);
        }
        if (config.includePathInGraph && fileItem.getParentDir().getId() != -1) {
            ensureNode(fileItem.getParentDir(), nodes);
        }
        return node;
    }

    private Edge ensureEdge(Node aSrc, Node aDst, HashMap<String, Edge> edges, FileItem edgeFromFile, FileItem edgeToFile) {
        Node src = aSrc;
        Node dst = aDst;
        if (aSrc.dirItem.getId() > aDst.dirItem.getId()) {
            src = aDst;
            dst = aSrc;
        }
        String edgeKey = src.dirItem.getId() + "-" + dst.dirItem.getId();
        Edge edge = edges.get(edgeKey);
        if (edge == null) {
            edge = new Edge(src, dst);
            edges.put(edgeKey, edge);
        }
        // update attributes
        edge.extensions.add(edgeFromFile.extension);
        edge.incWeight();
        edge.duplicateSize += edgeFromFile.size;
        src.duplicateSize += edgeFromFile.size;
        dst.duplicateSize += edgeToFile.size;
        return edge;
    }

    private void ensureFilteredNodes(DirItem dirItem, HashMap<Integer, Node> filteredNodes ) {
        Integer key = dirItem.getId();
        Node node = nodes.get(key);
        if (node == null) throw new RuntimeException("All nodes for all edges must exist in the graph!");
        filteredNodes.put(key, node);
        if (config.includePathInGraph && dirItem.getParentDir().getId() != -1) {
            ensureFilteredNodes(dirItem.getParentDir(), filteredNodes);
        }
    }

    private void ensurePathEdges(DirItem dirItem, HashMap<Integer, Node> filteredNodes, HashMap<String, Edge> pathEdges, long duplicateSize) {

        DirItem parentDirItem = dirItem.getParentDir();
        if (parentDirItem.getId() == -1) return;

        Node src = filteredNodes.get(dirItem.getId());
        Node dst = filteredNodes.get(parentDirItem.getId());
        String edgeKey = "parent:" + src.dirItem.getId() + "-" + dst.dirItem.getId();
        Edge edge = pathEdges.get(edgeKey);
        if (edge == null) {
            edge = new Edge(src, dst);
            pathEdges.put(edgeKey, edge);
            edge.incWeight();
        }
        dst.duplicateSize += duplicateSize;

        ensurePathEdges(parentDirItem, filteredNodes, pathEdges, duplicateSize);
    }

    public void printOutput() {
        if (config.graphOutputFile == null || config.graphOutputFile.isEmpty()) return;

        HashMap<Integer, Node> filteredNodes = new HashMap<>();
        LinkedList<Edge> filteredEdges = new LinkedList<>();
        HashMap<String, Edge> pathEdges = new HashMap<>();

        // filter edges based on config values
        for(Edge edge : edges.values()) {
            if (edge.weight > config.weightThreshold) {
                filteredEdges.addLast(edge);
            }
        }

        // find nodes
        for (Edge edge : filteredEdges) {
            ensureFilteredNodes(edge.src.dirItem, filteredNodes);
            ensureFilteredNodes(edge.dst.dirItem, filteredNodes);
        }

        // add path edges
        for (Edge edge : filteredEdges) {
            ensurePathEdges(edge.src.dirItem, filteredNodes, pathEdges, edge.duplicateSize);
            if (edge.src.dirItem.getId() != edge.dst.dirItem.getId()) {
                ensurePathEdges(edge.dst.dirItem, filteredNodes, pathEdges, edge.duplicateSize);
            }
        }

        // We created what we want to write. Now write it to the file
        FileWriter fileWriter = null;
        BufferedWriter bufferedWriter = null;
        try {
            try {
                fileWriter = new FileWriter(config.graphOutputFile);
                bufferedWriter = new BufferedWriter(fileWriter);
                bufferedWriter.write("digraph {");
                bufferedWriter.newLine();

                // First write all filteredNodes
                for (Node node : filteredNodes.values()) {
                    bufferedWriter.write(String.format("n%d [label=\"%s\", duplicateSizeMb=\"%d\", fontsize=14.0]", node.dirItem.getId(), node.dirItem.extractFileName(), node.duplicateSize/1024/1024)); //node.dirItem.name));
                    bufferedWriter.newLine();
                }

                // Write all duplicate filteredEdges
                for (Edge edge : filteredEdges) {
                    bufferedWriter.write(String.format("n%d -> n%d [label=\"%d\", weight=%d, duplicateSizeMb=\"%d\"]", edge.src.dirItem.getId(), edge.dst.dirItem.getId(), edge.weight, edge.weight, edge.duplicateSize));
                    bufferedWriter.newLine();
                }

                // Write path edges
                for (Edge edge : pathEdges.values()) {
                    bufferedWriter.write(String.format("n%d -> n%d [label=\"%d\", color=red, weight=%d, duplicateSizeMb=\"%d\"]", edge.src.dirItem.getId(), edge.dst.dirItem.getId(), edge.weight, edge.weight, edge.duplicateSize));
                    bufferedWriter.newLine();
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

    public void processResult(DuplicateAnalyzer aAnalyzer, TotalDiffConfig aConfig) {
        analyzer = aAnalyzer;
        config = aConfig;

        // TODO must be called only once. should I check? what?

        for (DuplicateInfo info : analyzer.getDuplicateMap().values()) {
            FileItem[] list = info.getFiles().toArray(new FileItem[0]);
            for (int i = 0; i < list.length - 1; i++) {
                Node node = ensureNode(list[i].getParentDir(), nodes);
                for (int j = i + 1; j < list.length; j++) {
                    Node otherNode = ensureNode(list[j].getParentDir(), nodes);
                    ensureEdge(node, otherNode, edges, list[i], list[j]);
                }
            }
        }
    }
}
