package com.mss.totaldiff.graph;

import com.mss.totaldiff.DuplicateAnalyzer;
import com.mss.totaldiff.TotalDiffConfig;

public interface AnalyzeOutputGenerator {
    void processResult(DuplicateAnalyzer aAnalyzer, TotalDiffConfig aConfig);
    void printOutput();
}
