package com.mss.totaldiff;

import com.mss.totaldiff.visitors.ItemVisitor;

public class DirItem extends FileItemBase{

    public DirItem(int aId, String aName, DirItem aParentDir) {
        super(aId, aName, aParentDir);
    }

    public boolean isRoot() {
        return id == -1;
    }
    public boolean notRoot() {
        return id != -1;
    }

    @Override
    public void visitItem(ItemVisitor visitor) {
        visitor.processDirItem(this);
    }
}
