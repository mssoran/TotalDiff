package com.mss.totaldiff;

import com.mss.totaldiff.visitors.ItemVisitor;

public class DirItem extends FileItemBase{

    public DirItem(int aId, String aName, DirItem aParentDir) {
        super(aId, aName, aParentDir);
    }

    @Override
    public void visitItem(ItemVisitor visitor) {
        visitor.processDirItem(this);
    }
}
