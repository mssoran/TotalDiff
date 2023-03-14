package com.mss.totaldiff;

import com.mss.totaldiff.filters.FilterForDirItem;
import com.mss.totaldiff.filters.FilterForFileItem;
import com.mss.totaldiff.filters.SimpleFilterForDirItem;
import com.mss.totaldiff.filters.SimpleFilterForFileItem;
import com.mss.totaldiff.visitors.FileSerializerVisitor;
import com.mss.totaldiff.visitors.ItemVisitor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Logger;

public class InfoTree {
    private static Logger logger = Logger.getLogger(InfoTree.class.getName());

    private DirItem root = new DirItem(-1, "root", null);
    private int itemCount = 0;
    private long totalProcessedFileSize = 0;
    private LinkedList<FileItem> files = new LinkedList<>();
    private LinkedList<DirItem> dirs = new LinkedList<>();
    private HashMap<Integer, DirItem> dirsMap = new HashMap<>();
    private HashMap<String, DirItem> dirsNameMap = new HashMap<>();
    private HashMap<String, FileItem> filesNameMap = new HashMap<>();

    private TotalDiffConfig config;

    private FilterForFileItem filterForFileItem;
    private FilterForDirItem filterForDirItem;

    public InfoTree(TotalDiffConfig aConfig) {
        dirs.addLast(root);
        dirsMap.put(root.id, root);
        root.parentDir = root;
        config = aConfig;
        filterForFileItem = new SimpleFilterForFileItem(config);
        filterForDirItem = new SimpleFilterForDirItem(config);
    }

    public void addFromFile(String inputFileName, Iterable<ItemVisitor> visitors) throws IOException {
        if (inputFileName == null || inputFileName.isEmpty()) return;
        File inputFile = new File(inputFileName);
        if (!inputFile.exists() || !inputFile.isFile()) throw new IOException("Input file '" + inputFile.getAbsolutePath() +"' doesn't exist");

        FileReader fileReader = null;
        BufferedReader bufferedReader = null;
        try {
            try {
                fileReader = new FileReader(inputFileName);
                bufferedReader = new BufferedReader(fileReader);
                String line;

                while ((line = bufferedReader.readLine()) != null) {
                    if (line.startsWith("d")) {
                        DirItem dirItem = FileSerializerVisitor.deserializeDirItemFromString(line, this);
                        if (dirItem.id >= itemCount) itemCount = dirItem.id + 1;
                        addDir(dirItem, visitors);
                    } else if (line.startsWith("f")) {
                        FileItem fileItem = FileSerializerVisitor.deserializeFileItemFromString(line, this);
                        if (fileItem.id >= itemCount) itemCount = fileItem.id + 1;
                        addFile(fileItem, visitors);
                    } else {
                        throw new RuntimeException("Unknown line is read from input file: " + line);
                    }
                }
            } finally {
                if (bufferedReader != null) bufferedReader.close();
                if (fileReader != null) fileReader.close();
            }
        } catch (IOException ex) {
            logger.severe("Failed to read from input..." + inputFile.getAbsolutePath());
            ex.printStackTrace();
            throw new IOException("Failed to read from input file " + inputFile.getAbsolutePath(), ex);
        }
    }

    private DirItem ensureDirItem(String dirName, DirItem parent, Iterable<ItemVisitor> visitors) {
        // check if we already have this dirItem (probably from an input file)
        DirItem dirItem = dirsNameMap.get(parent.getId()+"|"+dirName);
        if (dirItem == null) {
            dirItem = new DirItem(itemCount++, dirName, parent);
            addDir(dirItem, visitors);
        }
        return dirItem;
    }

    public void addToRoot(String dirName, Iterable<ItemVisitor> visitors) {
        DirItem dir = ensureDirItem(dirName, root, visitors);
        File dirFile = new File(dirName);
        addTree(dir, dirFile, visitors);
    }

    private long lastReportTime = 0;
    private long lastReportSize = 0;
    private int lastReportCount = 0;
    // report every 20 mins
    private static final long timeReportInterval = 10 * 60 * 1000;
    // report every 10GB
    private static final long sizeReportInterval = 10L * 1024 * 1024 * 1024;
    // report every 10000 file
    private int countReportInterval = 10000;
    private void reportProgress() {
        // TODO intervals can be configurable
        long now = System.currentTimeMillis();
        if (now - lastReportTime >= timeReportInterval ||
                totalProcessedFileSize  - lastReportSize >= sizeReportInterval ||
                itemCount - lastReportCount >= countReportInterval
        ) {
            lastReportTime = now;
            lastReportSize = totalProcessedFileSize;
            lastReportCount = itemCount;
            logger.info(String.format("itemCount:%d TotalProcessedFileSize:%s", itemCount, Utils.bytesToHumanReadable(totalProcessedFileSize)));
        }
    }

    private void addTree(DirItem dirItem, File dirItemFile, Iterable<ItemVisitor> visitors) {
        try {
            File[] allFilesInDir = dirItemFile.listFiles();
            if (allFilesInDir == null) {
                logger.severe("Unexpectedly cannot read files from the directory " + dirItemFile.getAbsolutePath());
                return;
            }
            for (File f : allFilesInDir) {
                reportProgress();
                if (f.isFile()) {
                    FileItem fileItem = new FileItem(itemCount++, f.getName(), dirItem);
                    fileItem.size = f.length();
                    //find extension
                    int dotIndex = fileItem.name.lastIndexOf('.');
                    if(dotIndex > 0) {
                        fileItem.extension = fileItem.name.substring(dotIndex+1);
                    }
                    //find hash
                    if (config.compareHashes && isOkToAddFile(fileItem)) {
                        fileItem.computeHash(f, config.fileReadBufferSize);
                    }
                    addFile(fileItem, visitors);
                } else if (f.isDirectory()) {
                    DirItem dirItem1 = ensureDirItem(f.getName(), dirItem, visitors);
                    addTree(dirItem1, f, visitors);
                } else {
                    System.out.println("Log: Unknown file type, skipping... :" + f.getAbsolutePath());
                }
            }
        } catch (Throwable t) {
            logger.severe("Fatal error when adding tree with id " + dirItem.getId() + " and name " + dirItem.getName() + " and parent dir is " + dirItem.getParentDir() + " skipping this tree for now.");
            t.printStackTrace();
        }
    }

    public DirItem getDirItemById(int aID) {
        return dirsMap.get(aID);
    }

    private void addDir(DirItem dirItem, Iterable<ItemVisitor> visitors) {
        if (!filterForDirItem.isValidToConsider(dirItem) || dirItem.getParentDir() == null) return;
        dirs.addLast(dirItem);
        dirsMap.put(dirItem.id, dirItem);
        dirsNameMap.put(dirItem.getParentDir().getId()+"|"+dirItem.name, dirItem);
        for (ItemVisitor visitor : visitors) {
            dirItem.visitItem(visitor);
        }
    }

    private boolean isOkToAddFile(FileItem fileItem) {
        if(fileItem.getParentDir() == null) return false;
        if (filesNameMap.containsKey(fileItem.getParentDir().getId()+"|"+fileItem.name)) return false;
        return (filterForFileItem.isValidToConsider(fileItem));
    }
    private void addFile(FileItem fileItem, Iterable<ItemVisitor> visitors) {
        if (!isOkToAddFile(fileItem)) return;
        files.addLast(fileItem);
        totalProcessedFileSize += fileItem.size;
        filesNameMap.put(fileItem.getParentDir().getId()+"|"+fileItem.name, fileItem);
        for(ItemVisitor visitor : visitors) {
            fileItem.visitItem(visitor);
        }
    }

    public int itemCount() {
        return itemCount;
    }

    public void printFilesNotIn(InfoTree otherInfoTree) {
        HashSet<String> existingKeysInOther = new HashSet<>();
        // first find all keys
        for (FileItem f: otherInfoTree.files) {
            existingKeysInOther.add(f.getKeyName());
        }
        // now print the diff
        for (FileItem f: files) {
            if(!existingKeysInOther.contains(f.getKeyName())) {
                System.out.println(f.extractFileName() + " " + f);
            }
        }
    }

    public void printAll() {
        System.out.println("Total item count:" + itemCount);
        System.out.println("Dirs:");
        for (DirItem dir: dirs) {
            System.out.println(String.format("%2d: under dir %d with name %s", dir.id, dir.parentDir.id, dir.name));
        }
        System.out.println("Files:");
        for (FileItem f: files) {
            f.printLine();
        }
    }

    public LinkedList<FileItem> getFiles() {
        return files;
    }
}
