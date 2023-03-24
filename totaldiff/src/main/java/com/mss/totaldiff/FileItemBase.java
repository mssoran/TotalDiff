package com.mss.totaldiff;

import com.mss.totaldiff.visitors.ItemVisitor;

import java.util.Iterator;
import java.util.LinkedList;

public abstract class FileItemBase {
    protected int id;
    protected String name;
    protected DirItem parentDir;

    protected long totalSize = 0;

    protected FileItemBase(int aId, String aName, DirItem aParentDir) {
        id = aId;
        name = aName;
        parentDir = aParentDir;
    }

    abstract public void visitItem(ItemVisitor visitor);

    public String extractFileName() {
        LinkedList<String> path = new LinkedList<>();
        path.addFirst(name);
        DirItem nextDir = parentDir;
        while (nextDir.notRoot()) {
            path.addFirst(nextDir.getName());
            nextDir = nextDir.getParentDir();
        }
        StringBuilder sb = new StringBuilder();
        Iterator<String> it = path.iterator();
        while (it.hasNext()) {
            sb.append(it.next());
            sb.append("/");
        }
        return sb.toString();
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public DirItem getParentDir() {
        return parentDir;
    }

    public long getTotalSize() { return totalSize; }

    public void addToTotalSize(long inc) { totalSize += inc; }
}
