package com.mss.totaldiff.filters;

import com.mss.totaldiff.FileItem;
import com.mss.totaldiff.TotalDiffConfig;
import com.mss.totaldiff.filters.FilterForFileItem;

public class SimpleFilterForFileItem implements FilterForFileItem {
    private TotalDiffConfig config;

    public SimpleFilterForFileItem(TotalDiffConfig aConfig) {
        config = aConfig;
    }
    @Override
    public boolean isValidToConsider(FileItem fileItem) {
        if (fileItem.getName().equals(".DS_Store")) return false;
        return fileItem.size > config.considerFileSize;
    }

    @Override
    public boolean isValidToAnalyze(FileItem fileItem) {
        return true;
    }
}
