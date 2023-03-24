package com.mss.totaldiff.graph;

import com.mss.totaldiff.Utils;

import java.util.Iterator;
import java.util.LinkedList;

public class GraphDuplicateGroupNode {
    public long duplicateSize = 0;
    public long singleSize = 0;
    public int duplicateFileCount = 0;
    public LinkedList<GraphNode> parents = new LinkedList<>();

    public String duplicateInfoKey;
    public GraphDuplicateGroupNode(String aDuplicateInfoKey) {
        duplicateInfoKey = aDuplicateInfoKey;
    }

    public void prettyPrint() {
        System.out.println("Key: " + duplicateInfoKey);
        System.out.println("    duplicateSize:" + duplicateSize + " singleSize:" + singleSize + " duplicateFileCount" + duplicateFileCount);
        System.out.println("    Expected savings:" + String.format("%s", Utils.bytesToHumanReadable(duplicateSize - singleSize)));
        System.out.println("    Directories:");
        Iterator<GraphNode> it = parents.iterator();
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
