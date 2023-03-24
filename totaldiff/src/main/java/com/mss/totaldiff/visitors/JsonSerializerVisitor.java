package com.mss.totaldiff.visitors;


import com.fasterxml.jackson.core.*;
import com.mss.totaldiff.DirItem;
import com.mss.totaldiff.FileItem;
import com.mss.totaldiff.InfoTree;

import java.io.*;
import java.util.logging.Logger;

public class JsonSerializerVisitor implements ItemVisitor, Closeable {

    private static Logger logger = Logger.getLogger(FileSerializerVisitor.class.getName());

    private JsonGenerator jsonGenerator;

    public JsonSerializerVisitor(File file) throws IOException {
        JsonFactory jsonFactory = JsonFactory.builder().build();
        jsonGenerator = jsonFactory.createGenerator(file, JsonEncoding.UTF8);
        jsonGenerator.writeStartObject();
        jsonGenerator.writeFieldName("fileitems");
        jsonGenerator.writeStartArray();
    }

    @Override
    public void processFileItem(FileItem fileItem) {
        try {
            jsonGenerator.writeRaw('\n');
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("t", "f");
            jsonGenerator.writeNumberField("id", fileItem.getId());
            jsonGenerator.writeStringField("name", fileItem.getName());
            jsonGenerator.writeNumberField("pid", fileItem.getParentDir().getId());
            jsonGenerator.writeNumberField("size", fileItem.size);
            jsonGenerator.writeStringField("ext", fileItem.extension);
            jsonGenerator.writeStringField("hash", fileItem.fileDigest);
            jsonGenerator.writeEndObject();
        } catch (IOException ex) {
            logger.severe("Error writing to file!" + ex);
            ex.printStackTrace();
        }
    }

    @Override
    public void processDirItem(DirItem dirItem) {
        try {
            jsonGenerator.writeRaw('\n');
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("t", "d");
            jsonGenerator.writeNumberField("id", dirItem.getId());
            jsonGenerator.writeStringField("name", dirItem.getName());
            jsonGenerator.writeNumberField("pid", dirItem.getParentDir().getId());
            jsonGenerator.writeEndObject();
        } catch (IOException ex) {
            logger.severe("Error writing to file!" + ex);
            ex.printStackTrace();
        }
    }

    @Override
    public void close() throws IOException {
        jsonGenerator.writeRaw('\n');
        jsonGenerator.writeEndArray();
        jsonGenerator.writeEndObject();
        jsonGenerator.close();
    }

    private static void readFileItem (JsonParser jsonParser, Iterable<ItemVisitor> visitors, InfoTree infoTree) throws IOException {

        if (jsonParser.currentToken() != JsonToken.START_OBJECT) {
            throw new IOException("An object is expected as a file item");
        }
        boolean isDir = false;
        int id = -2;
        String name = null;
        int pid = -2;
        long size = -2;
        String ext = null;
        String hash = null;

        while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = jsonParser.getCurrentName();
            jsonParser.nextToken();
            if ("t".equals(fieldName)) {
                isDir = jsonParser.getValueAsString().equals("d");
            } else if ("id".equals(fieldName)) {
                id = jsonParser.getIntValue();
            } else if ("name".equals(fieldName)) {
                name = jsonParser.getValueAsString();
            } else if ("pid".equals(fieldName)) {
                pid = jsonParser.getIntValue();
            } else if ("size".equals(fieldName)) {
                size = jsonParser.getLongValue();
            } else if ("ext".equals(fieldName)) {
                ext = jsonParser.getValueAsString();
            } else if ("hash".equals(fieldName)) {
                hash = jsonParser.getValueAsString();
            } else {
                throw new IOException("Unknown field:" + fieldName);
            }
        }

        if (name == null || id == -2 || pid == -2) {
            throw new IOException("Missing mandatory field in the json");
        }

        if (isDir) {
            DirItem dirItem = new DirItem(id, name, infoTree.getDirItemById(pid));
            infoTree.addDeserializedDir(dirItem, visitors);
        } else {
            if (size == -2) {
                throw new IOException("Missing mandatory field size in the json");
            }
            FileItem fileItem = new FileItem(id, name, infoTree.getDirItemById(pid));
            fileItem.size = size;
            if (ext != null) {
                fileItem.extension = ext;
            }
            if (hash != null) {
                fileItem.fileDigest = hash;
            }
            infoTree.addDeserializedFile(fileItem, visitors);
        }
    }

    private static void addFromJsonParser(JsonParser jsonParser, Iterable<ItemVisitor> visitors, InfoTree infoTree) throws IOException {
        if (jsonParser.nextToken() != JsonToken.START_OBJECT) {
            throw new IOException("Expected data to start with an Object");
        }

        while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = jsonParser.getCurrentName();
            if ("fileitems".equals(fieldName)) {
                if (jsonParser.nextToken() != JsonToken.START_ARRAY) {
                    System.out.println("Next token is :" + jsonParser.currentToken());
                    throw new IOException("An array is expected");
                }
                while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
                    readFileItem(jsonParser, visitors, infoTree);
                }
            } else {
                throw new IOException("Unknown json field");
            }
        }
    }


    public static void addFromString(String content, Iterable<ItemVisitor> visitors, InfoTree infoTree) throws IOException {
        JsonFactory jsonFactory = JsonFactory.builder().build();
        JsonParser jsonParser = jsonFactory.createParser(content);

        try {
            addFromJsonParser(jsonParser, visitors, infoTree);
        } catch (IOException ex) {
            logger.severe("Failed to read from input string content");
            ex.printStackTrace();
            throw ex;
        } finally {
            jsonParser.close();
        }
    }

    public static void addFromFile(String inputFileName, Iterable<ItemVisitor> visitors, InfoTree infoTree) throws IOException {
        if (inputFileName == null || inputFileName.isEmpty()) return;
        File inputFile = new File(inputFileName);
        if (!inputFile.exists() || !inputFile.isFile()) throw new IOException("Input file '" + inputFile.getAbsolutePath() +"' doesn't exist");

        JsonFactory jsonFactory = JsonFactory.builder().build();
        JsonParser jsonParser = jsonFactory.createParser(inputFile);

        try {
            addFromJsonParser(jsonParser, visitors, infoTree);
        } catch (IOException ex) {
            logger.severe("Failed to read from input..." + inputFile.getAbsolutePath());
            ex.printStackTrace();
            throw new IOException("Failed to read from input file " + inputFile.getAbsolutePath(), ex);
        } finally {
            jsonParser.close();
        }
    }
}
