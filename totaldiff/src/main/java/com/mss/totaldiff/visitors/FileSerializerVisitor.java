package com.mss.totaldiff.visitors;


import com.mss.totaldiff.DirItem;
import com.mss.totaldiff.FileItem;
import com.mss.totaldiff.InfoTree;

import java.io.*;
import java.util.logging.Logger;

public class FileSerializerVisitor implements ItemVisitor, Closeable {

    private static Logger logger = Logger.getLogger(FileSerializerVisitor.class.getName());

    private BufferedWriter writer;

    public static FileItem deserializeFileItemFromString(String serializedString, InfoTree infoTree) {
        String[] parts = serializedString.split("\\|\\|");
        if (parts.length != 7 && parts.length != 6 && parts.length != 5) throw new RuntimeException("Cannot deserialize this input: " + serializedString);
        if (!parts[0].equals("f")) throw new RuntimeException("Cannot deserialize this input, input is not a FileItem: " + serializedString);
        FileItem result = new FileItem(Integer.valueOf(parts[1]), parts[2], infoTree.getDirItemById(Integer.valueOf(parts[3])));
        result.size = Long.valueOf(parts[4]);
        if (parts.length == 6) {
            result.extension = parts[5];
        } else if (parts.length == 7) {
            result.extension = parts[5];
            result.fileDigest = parts[6];
        }
        return result;
    }

    public static DirItem deserializeDirItemFromString(String serializedString, InfoTree infoTree) {
        String[] parts = serializedString.split("\\|\\|");
        if (parts.length != 4) throw new RuntimeException("Cannot deserialize this input: " + serializedString);
        if (!parts[0].equals("d")) throw new RuntimeException("Cannot deserialize this input, input is not a DirItem: " + serializedString);

        DirItem result = new DirItem(Integer.valueOf(parts[1]), parts[2], infoTree.getDirItemById(Integer.valueOf(parts[3])));
        return result;
    }

    public FileSerializerVisitor(File file) throws IOException {
        writer = new BufferedWriter(new FileWriter(file));
    }

    private void writeToWriter(String line) {
        try {
            writer.write(line);
            writer.newLine();
        } catch (IOException ex) {
            logger.severe("Error writing to file!" + ex);
            ex.printStackTrace();
        }
    }

    @Override
    public void processFileItem(FileItem fileItem) {
        String line = String.format("f||%d||%s||%d||%d||%s||%s", fileItem.getId(), fileItem.getName(), fileItem.getParentDir().getId(), fileItem.size, fileItem.extension, fileItem.fileDigest);
        writeToWriter(line);
    }

    @Override
    public void processDirItem(DirItem dirItem) {
        String line = String.format("d||%d||%s||%d", dirItem.getId(), dirItem.getName(), dirItem.getParentDir().getId());
        writeToWriter(line);
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}
