package com.mss.totaldiff.graph;

import java.util.Comparator;

/**
 * Saved size is the difference between the total size used and the single size
 */
public class SavedSizeComparator implements Comparator<GraphDuplicateGroupNode> {
    @Override
    public int compare(GraphDuplicateGroupNode first, GraphDuplicateGroupNode second) {
        long firstSave = first.duplicateSize - first.singleSize;
        long secondSave = second.duplicateSize - second.singleSize;
        if (firstSave > secondSave) return 1;
        else if (firstSave < secondSave) return -1;
        else return 0;
    }
}
