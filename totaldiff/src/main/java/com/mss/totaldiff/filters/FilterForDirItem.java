package com.mss.totaldiff.filters;

import com.mss.totaldiff.DirItem;

public interface FilterForDirItem  {
    boolean isValidToConsider(DirItem dirItem);
    boolean isValidToAnalyze(DirItem dirItem);
}
