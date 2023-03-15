package com.mss.totaldiff.visitors;

import com.mss.totaldiff.DirItem;
import com.mss.totaldiff.FileItem;

import java.io.Closeable;

public interface ItemVisitor extends Closeable {
    void processFileItem(FileItem fileItem);
    void processDirItem(DirItem dirItem);
}
