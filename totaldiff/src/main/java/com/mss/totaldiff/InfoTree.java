package com.mss.totaldiff;

import com.mss.totaldiff.filters.FilterForDirItem;
import com.mss.totaldiff.filters.FilterForFileItem;
import com.mss.totaldiff.filters.SimpleFilterForDirItem;
import com.mss.totaldiff.filters.SimpleFilterForFileItem;
import com.mss.totaldiff.visitors.ItemVisitor;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.logging.Logger;

public class InfoTree {
    private static Logger logger = Logger.getLogger(InfoTree.class.getName());

    private DirItem root = new DirItem(-1, "root", null);
    private int itemCount = 0;
    private long totalProcessedFileSize = 0;
    private long numberOfHashCalculations = 0;
    private long numberOfHashLookups = 0;
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

    private String getNameMapKey(DirItem parent, String name) {
        return parent.getId() + "|" + name;
    }
    private String getNameMapKey(FileItemBase file) {
        return getNameMapKey(file.parentDir, file.name);
    }

    private DirItem ensureDirItem(String dirName, DirItem parent, Iterable<ItemVisitor> visitors) {
        // check if we already have this dirItem (probably from an input file)
        DirItem dirItem = dirsNameMap.get(getNameMapKey(parent, dirName));
        if (dirItem == null) {
            dirItem = new DirItem(itemCount++, dirName, parent);
            addDir(dirItem, visitors);
        }
        return dirItem;
    }

    class LookupInfoTree {
        InfoTree lookupInfoTree;
        DirItem currentDir = null;

        public LookupInfoTree(InfoTree aLookupInfoTree, String aInitialDirName) {
            lookupInfoTree = aLookupInfoTree;
            if (lookupInfoTree != null) {
                currentDir = lookupInfoTree.dirsNameMap.get(lookupInfoTree.getNameMapKey(lookupInfoTree.root, aInitialDirName));
            }
        }

        public DirItem updateCurrentDirTo(String dirName) {
            if (currentDir == null || lookupInfoTree == null) return null;

            DirItem prevDir = currentDir;
            currentDir = lookupInfoTree.dirsNameMap.get(lookupInfoTree.getNameMapKey(currentDir, dirName));
            return prevDir;
        }

        public void updateCurrentDirTo(DirItem dirItem) {
            currentDir = dirItem;
        }

        public String getHashFor(FileItem fileItem) {
            if (currentDir == null || lookupInfoTree == null) return null;

            FileItem file = lookupInfoTree.filesNameMap.get(lookupInfoTree.getNameMapKey(currentDir, fileItem.name));
            if (file != null && file.size == fileItem.size) {
                // Here we can make the assumption that we'll not need this item anymore, so for
                // using memory more efficiently, we can remove the file from the lookupInfoTree
                // as much as possible
                String digest = file.fileDigest;
                lookupInfoTree.cleanup(file);
                return digest;
            }
            return null;
        }
    }

    protected void cleanup(FileItem fileItem) {
        // This doesn't release all the memory, but gives back at least some of the unused memory
        filesNameMap.remove(getNameMapKey(fileItem));
        fileItem.fileDigest = null;
        fileItem.extension = null;
    }

    public void addToRoot(String dirName, Iterable<ItemVisitor> visitors, InfoTree aLookupInfoTree) {
        LookupInfoTree lookupInfoTree = new LookupInfoTree(aLookupInfoTree, dirName);
        DirItem dir = ensureDirItem(dirName, root, visitors);
        File dirFile = new File(dirName);
        addTree(dir, dirFile, visitors, lookupInfoTree);
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
            logger.info(String.format("itemCount:%d TotalProcessedFileSize:%s hash calculations:%s hash lookups:%s", itemCount, Utils.bytesToHumanReadable(totalProcessedFileSize), numberOfHashCalculations, numberOfHashLookups));
        }
    }

    private void addTree(DirItem dirItem, File dirItemFile, Iterable<ItemVisitor> visitors, LookupInfoTree lookupInfoTree) {
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
                        String lookedUpHash = lookupInfoTree.getHashFor(fileItem);
                        if (lookedUpHash == null) {
                            numberOfHashCalculations++;
                            fileItem.computeHash(f, config.fileReadBufferSize, config.maxSizeForHashComparison);
                        } else {
                            numberOfHashLookups++;
                            fileItem.fileDigest = lookedUpHash;
                        }
                    }
                    addFile(fileItem, visitors);
                } else if (f.isDirectory()) {
                    DirItem dirItem1 = ensureDirItem(f.getName(), dirItem, visitors);
                    DirItem lookupPrev = lookupInfoTree.updateCurrentDirTo(f.getName());
                    addTree(dirItem1, f, visitors, lookupInfoTree);
                    lookupInfoTree.updateCurrentDirTo(lookupPrev);
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

    public void addDeserializedDir(DirItem dirItem, Iterable<ItemVisitor> visitors) {
        addDir(dirItem, visitors);
    }

    private void addDir(DirItem dirItem, Iterable<ItemVisitor> visitors) {
        if (dirItem.id >= itemCount) itemCount = dirItem.id + 1;
        if (!filterForDirItem.isValidToConsider(dirItem) || dirItem.getParentDir() == null) return;
        dirs.addLast(dirItem);
        dirsMap.put(dirItem.id, dirItem);
        dirsNameMap.put(getNameMapKey(dirItem), dirItem);
        for (ItemVisitor visitor : visitors) {
            dirItem.visitItem(visitor);
        }
    }

    private boolean isOkToAddFile(FileItem fileItem) {
        if(fileItem.getParentDir() == null) return false;
        if (filesNameMap.containsKey(getNameMapKey(fileItem))) return false;
        return (filterForFileItem.isValidToConsider(fileItem));
    }

    public void addDeserializedFile(FileItem fileItem, Iterable<ItemVisitor> visitors) {
        addFile(fileItem, visitors);
    }

    private void addFile(FileItem fileItem, Iterable<ItemVisitor> visitors) {
        if (fileItem.id >= itemCount) itemCount = fileItem.id + 1;
        if (!isOkToAddFile(fileItem)) return;
        totalProcessedFileSize += fileItem.size;
        filesNameMap.put(getNameMapKey(fileItem), fileItem);
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
        for (FileItem f: otherInfoTree.filesNameMap.values()) {
            existingKeysInOther.add(f.getKeyName());
        }
        // now print the diff
        for (FileItem f: filesNameMap.values()) {
            if(!existingKeysInOther.contains(f.getKeyName())) {
                System.out.println(f.extractFileName() + " " + f);
            }
        }
    }

    public void logSummary() {
        logger.info("--------- LogTree Summary:");
        logger.info("itemCount: " + itemCount);
        logger.info("totalProcessedFileSize: " + Utils.bytesToHumanReadable(totalProcessedFileSize));
        logger.info("numberOfHashCalculations : " + numberOfHashCalculations );
        logger.info("numberOfHashLookups : " + numberOfHashLookups );
        logger.info("file count: " + filesNameMap.size());
        logger.info("dir count: " + dirs.size());
    }

    public void printAll() {
        System.out.println("Total item count:" + itemCount);
        System.out.println("Dirs:");
        for (DirItem dir: dirs) {
            System.out.println(String.format("%2d: under dir %d with name %s", dir.id, dir.parentDir.id, dir.name));
        }
        System.out.println("Files:");
        for (FileItem f: filesNameMap.values()) {
            f.printLine();
        }
    }

    public Collection<FileItem> getFiles() {
        return filesNameMap.values();
    }
}
