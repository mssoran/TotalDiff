package com.mss.totaldiff.visitors;

import com.mss.totaldiff.DirItem;
import com.mss.totaldiff.FileItem;

public interface ItemVisitor {
    void processFileItem(FileItem fileItem);
    void processDirItem(DirItem dirItem);
}
