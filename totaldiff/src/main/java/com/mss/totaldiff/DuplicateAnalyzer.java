package com.mss.totaldiff;

import java.util.HashMap;
import java.util.LinkedList;

public class DuplicateAnalyzer {
    HashMap<String, FileItem> fileMap = new HashMap<>();
    private HashMap<String, DuplicateInfo> duplicateMap = new HashMap<>();

    public void analyze(InfoTree tree) {
        for (FileItem f : tree.getFiles()) {
            String key = f.getKeyName();
            FileItem fileItem = fileMap.get(key);
            if (fileItem != null) {
                // found a duplicate
                DuplicateInfo dupInfo = duplicateMap.get(key);
                if(dupInfo == null) {
                    dupInfo = new DuplicateInfo(key);
                    dupInfo.addFileItem(fileItem);
                    duplicateMap.put(key, dupInfo);
                }
                dupInfo.addFileItem(f);
            } else {
               fileMap.put(key, f);
            }

            // calculate total size of all dirs
            f.addToTotalSize(f.size);
            DirItem dirItem = f.getParentDir();
            while (dirItem.notRoot()) {
                dirItem.addToTotalSize(f.size);
                dirItem = dirItem.getParentDir();
            }
        }
    }

    public void printAllDuplicates() {
        System.out.println("Duplicate key count is: " + duplicateMap.size());
        for (DuplicateInfo di : duplicateMap.values()) {
            LinkedList<FileItem> duplicates = di.getFiles();
            System.out.println("Duplicate key:" + duplicates.getFirst().getKeyName());
            for (FileItem f : duplicates) {
                System.out.print("   ");
                f.printLine();
            }
        }
    }

    public HashMap<String, DuplicateInfo> getDuplicateMap() {
        return duplicateMap;
    }
}
