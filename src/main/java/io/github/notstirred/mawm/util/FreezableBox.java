package io.github.notstirred.mawm.util;

import cubicchunks.converter.lib.util.BoundingBox;
import cubicchunks.converter.lib.util.Vector3i;
import cubicchunks.regionlib.impl.EntryLocation2D;
import cubicchunks.regionlib.impl.EntryLocation3D;

public class FreezableBox {
    public enum Type { Read, Write }

    private BoundingBox box;
    private Type type;

    public FreezableBox(Type freezeType, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        box = new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
        type = freezeType;
    }

    public FreezableBox(Type freezeType, Vector3i minPos, Vector3i maxPos) {
        box = new BoundingBox(minPos, maxPos);
        type = freezeType;
    }

    public Type getType() {
        return type;
    }

    public BoundingBox getBox() {
        return box;
    }

    public boolean isCubeReadFrozen(int x, int y, int z) {
        if(type == Type.Read) {
            return box.intersects(x, y, z);
        }
        return false;
    }
    public boolean isCubeWriteFrozen(int x, int y, int z) {
        if(type == Type.Write) {
            return box.intersects(x, y, z);
        }
        return false;
    }
    public boolean isCubeFrozen(int x, int y, int z) {
        return box.intersects(x, y, z);
    }

    public boolean isColumnReadFrozen(int x, int z) {
        if(type == Type.Read) {
            return box.columnIntersects(x, z);
        }
        return false;
    }
    public boolean isColumnWriteFrozen(int x, int z) {
        if(type == Type.Write) {
            return box.columnIntersects(x, z);
        }
        return false;
    }
    public boolean isColumnFrozen(int x, int z) {
        return box.columnIntersects(x, z);
    }

    public boolean is2dRegionReadFrozen(EntryLocation2D entry) {
        if(type == Type.Read) {
            return box.asRegionCoords(new Vector3i(32, 32, 32)).columnIntersects(entry.getEntryX(), entry.getEntryZ());
        }
        return false;
    }
    public boolean is2dRegionWriteFrozen(EntryLocation2D entry) {
        if(type == Type.Write) {
            return box.asRegionCoords(new Vector3i(32, 32, 32)).columnIntersects(entry.getEntryX(), entry.getEntryZ());
        }
        return false;
    }

    public boolean is3dRegionReadFrozen(EntryLocation3D entry) {
        if(type == Type.Read) {
            return box.asRegionCoords(new Vector3i(16, 16, 16)).intersects(entry.getEntryX(), entry.getEntryY(), entry.getEntryZ());
        }
        return false;
    }
    public boolean is3dRegionWriteFrozen(EntryLocation3D entry) {
        if(type == Type.Write) {
            return box.asRegionCoords(new Vector3i(16, 16, 16)).intersects(entry.getEntryX(), entry.getEntryY(), entry.getEntryZ());
        }
        return false;
    }
}
