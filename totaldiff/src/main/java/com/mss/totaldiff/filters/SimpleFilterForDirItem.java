package com.mss.totaldiff.filters;

import com.mss.totaldiff.DirItem;
import com.mss.totaldiff.TotalDiffConfig;

public class SimpleFilterForDirItem implements FilterForDirItem {

    private TotalDiffConfig config;

    public SimpleFilterForDirItem(TotalDiffConfig aConfig) {
        config = aConfig;
    }

    @Override
    public boolean isValidToConsider(DirItem dirItem) {
        for (String excludeDir : config.considerDirExcludes) {
            if (dirItem.getName().equals(excludeDir)) return false;
        }
        return true;
    }

    @Override
    public boolean isValidToAnalyze(DirItem dirItem) {
        return true;
    }
}
