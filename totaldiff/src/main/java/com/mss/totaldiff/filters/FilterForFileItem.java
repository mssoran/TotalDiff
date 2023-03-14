package com.mss.totaldiff.filters;

import com.mss.totaldiff.FileItem;

public interface FilterForFileItem {
    boolean isValidToConsider(FileItem fileItem);
    boolean isValidToAnalyze(FileItem fileItem);
}
