package com.mss.totaldiff;

import java.util.LinkedList;

public class DuplicateInfo {
    private LinkedList<FileItem> files = new LinkedList<>();
    private String uniqueKey;

    public DuplicateInfo(String aUniqueKey) {
        uniqueKey = aUniqueKey;
    }

    public void addFileItem(FileItem fileItem) {
        files.addLast(fileItem);
    }

    public LinkedList<FileItem> getFiles() {
        return files;
    }

    public String getUniqueKey() {
        return uniqueKey;
    }
}
