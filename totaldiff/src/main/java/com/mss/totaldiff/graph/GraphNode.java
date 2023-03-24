package com.mss.totaldiff.graph;

import com.mss.totaldiff.DirItem;
import com.mss.totaldiff.Utils;

public class GraphNode {
    public DirItem dirItem;
    public long duplicateSize = 0;
    public int duplicateFileCount = 0;
    public int inpathCount = 0;
    public GraphNode(DirItem aDirItem) {
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
