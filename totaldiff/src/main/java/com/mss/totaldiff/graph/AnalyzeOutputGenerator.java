package com.mss.totaldiff.graph;

import com.mss.totaldiff.DuplicateAnalyzer;
import com.mss.totaldiff.TotalDiffConfig;

public interface AnalyzeOutputGenerator {
    // TODO remove this method from the interface
    void processResult(DuplicateAnalyzer aAnalyzer, TotalDiffConfig aConfig);
    void processResult(DuplicatesGraph aDuplicatesGraph, TotalDiffConfig aConfig);
    void printOutput();
}
