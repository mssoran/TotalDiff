package com.mss.totaldiff.graph;

import com.mss.totaldiff.*;
import com.mss.totaldiff.visitors.JsonSerializerVisitor;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DuplicatesGraphTests {

    private String jsonStringContent = """
{"fileitems":[
 {"t":"d","id":0,"name":"/someroot/source","pid":-1}
,{"t":"f","id":2,"name":"a file.ext","pid":0,"size":548679,"ext":"ext","hash":"324a11111111b0696e0ec05445695b74"}
,{"t":"f","id":3,"name":"no extension file","pid":0,"size":741,"ext":"","hash":"324a965458c6b0696e0ec05445695b74"}
,{"t":"d","id":10,"name":"subdir1","pid":0}
,{"t":"f","id":11,"name":"other.ext","pid":10,"size":548679,"ext":"ext","hash":"324a11111111b0696e0ec05445695b74"}
,{"t":"d","id":12,"name":"subdir1_subdir","pid":10}
,{"t":"f","id":13,"name":"other.ext","pid":12,"size":548679,"ext":"ext","hash":"324a11111111b0696e0ec05445695b74"}
]}
""";

    private TotalDiffConfig config = new TotalDiffConfig();
    @Test
    void findDuplicates() throws IOException {
        DuplicateAnalyzer da = new DuplicateAnalyzer();
        InfoTree tree = new InfoTree(config);
        JsonSerializerVisitor.addFromString(jsonStringContent, new LinkedList<>(), tree);
        DirItem dir0 = tree.getDirItemById(0);
//        assertEquals(dir0.getTotalSize(), 1646778);
        DirItem dir10 = tree.getDirItemById(10);
//        assertEquals(dir10.getTotalSize(), 1097358);
        DirItem dir11 = tree.getDirItemById(11);
//        assertEquals(dir11.getTotalSize(), 548679);
        da.analyze(tree);
        HashMap<String, DuplicateInfo> dupMap = da.getDuplicateMap();
        // There should be onlye one duplicate info, and the size should be 548679
        assertEquals(dupMap.size(), 1);
        Iterator<FileItem> it = tree.getFiles().iterator();
        FileItem f = it.next();
        assertEquals(f.size, 548679);
        assertEquals(f.extension, "ext");
        assertEquals(f.fileDigest, "324a11111111b0696e0ec05445695b74");

        DuplicateInfo dupInfo = dupMap.get(f.getKeyName());
        dupInfo.getFiles().get(0).printLine();
        dupInfo.getFiles().get(1).printLine();
        assertEquals(dupInfo.getFiles().size(), 3);
    }
}
