package io.github.notstirred.mawm.util;

import cubicchunks.converter.lib.util.BoundingBox;
import cubicchunks.converter.lib.util.Vector3i;
import cubicchunks.regionlib.impl.EntryLocation2D;
import cubicchunks.regionlib.impl.EntryLocation3D;

public class FreezableBox {

    private final BoundingBox box;

    public FreezableBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        box = new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public FreezableBox(Vector3i minPos, Vector3i maxPos) {
        box = new BoundingBox(minPos, maxPos);
    }

    public BoundingBox getBox() {
        return box;
    }

    public boolean isCubeFrozen(int x, int y, int z) {
        return box.intersects(x, y, z);
    }

    public boolean isColumnFrozen(int x, int z) {
        return box.columnIntersects(x, z);
    }

    public boolean is2dRegionFrozen(EntryLocation2D entry) {
        return box.asRegionCoords(new Vector3i(32, 32, 32)).columnIntersects(entry.getEntryX(), entry.getEntryZ());
    }

    public boolean is3dRegionFrozen(EntryLocation3D entry) {
        return box.asRegionCoords(new Vector3i(16, 16, 16)).intersects(entry.getEntryX(), entry.getEntryY(), entry.getEntryZ());
    }
}
