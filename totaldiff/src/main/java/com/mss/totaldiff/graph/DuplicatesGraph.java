package com.mss.totaldiff.graph;

import com.mss.totaldiff.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

public class DuplicatesGraph {
    private HashMap<String, GraphDuplicateGroupNode> duplicateNodes = new HashMap<>();
    private HashMap<String, GraphNode> nodes = new HashMap<>();

    public DuplicatesGraph (DuplicateAnalyzer aAnalyzer, TotalDiffConfig aConfig) {

        for (DuplicateInfo info : aAnalyzer.getDuplicateMap().values()) {
            FileItem[] list = info.getFiles().toArray(new FileItem[0]);
            GraphDuplicateGroupNode duplicateGroupNode = ensureDuplicateNode(info);
            for (int i = 0; i < list.length; i++) {
                FileItem fileItem = list[i];
                GraphNode node = nodes.get(Integer.toString(fileItem.getParentDir().getId()));

                duplicateGroupNode.duplicateSize += fileItem.size;

                node.duplicateSize += fileItem.size;
                { // here we update duplicateSize of all the path
                    String key = Integer.toString(node.dirItem.getParentDir().getId());
                    GraphNode parentNode = nodes.get(key);
                    while (parentNode != null) {
                        parentNode.duplicateSize += fileItem.size;
                        key = Integer.toString(parentNode.dirItem.getParentDir().getId());
                        parentNode = nodes.get(key);
                    }
                }
                node.duplicateFileCount++;
            }
        }

    }

    private GraphDuplicateGroupNode ensureDuplicateNode(DuplicateInfo duplicateInfo) {
        // normalize the name for this duplicateInfo
        // if all the parent dirs are the same for two different duplicateInfo objects, we want them to be in the same GraphNode
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
        GraphDuplicateGroupNode node = duplicateNodes.get(key);
        if (node == null) {
            node = new GraphDuplicateGroupNode(key);
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

    private GraphNode ensurePathNode(DirItem dirItem) {
        String key = Integer.toString(dirItem.getId());
        GraphNode node = nodes.get(key);
        if (node == null) {
            node = new GraphNode(dirItem);
            nodes.put(key, node);
            if (dirItem.getParentDir().notRoot()) {
                ensurePathNode(dirItem.getParentDir());
            }
        }
        node.inpathCount ++;

        return node;
    }

    public GraphNode getParentNodeOf(GraphNode node) {
        return nodes.get(Integer.toString(node.dirItem.getParentDir().getId()));
    }

    public Collection<GraphDuplicateGroupNode> getAllGraphDuplicateGroupNodes() {
        return duplicateNodes.values();
    }
}
