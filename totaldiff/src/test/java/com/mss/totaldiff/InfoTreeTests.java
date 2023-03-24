package com.mss.totaldiff;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mss.totaldiff.visitors.JsonSerializerVisitor;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.LinkedList;

class InfoTreeTests {

    private TotalDiffConfig config = new TotalDiffConfig();

    @Test
    void parentIdNotExists() throws IOException {
        String jsonStringContent = """
{"fileitems":[
 {"t":"d","id":0,"name":"/someroot/source","pid":-1}
,{"t":"f","id":2,"name":"a file.ext","pid":1,"size":741,"ext":"ext","hash":"324a965458c6b0696e0ec05445695b74"}
]}
""";
        InfoTree tree = new InfoTree(config);
        JsonSerializerVisitor.addFromString(jsonStringContent, new LinkedList<>(), tree);
    }
    @Test
    void addition() {
        assertEquals(2, 1 + 1);
    }
}