package io.github.notstirred.mawm.converter.task.extra;

import com.flowpowered.nbt.ByteTag;
import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import cubicchunks.converter.lib.util.BoundingBox;
import cubicchunks.converter.lib.util.ImmutablePair;
import cubicchunks.converter.lib.util.Vector3i;
import cubicchunks.converter.lib.util.edittask.BaseEditTask;
import cubicchunks.converter.lib.util.edittask.EditTask;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class ManyReplaceEditTask extends BaseEditTask {
    private final byte[] inBlockIDs;
    private final byte[] inBlockMetas;

    private final byte outBlockID;
    private final byte outBlockMeta;

    public ManyReplaceEditTask(@Nonnull BoundingBox srcBox, @Nonnull byte[] inBlockIDs, @Nonnull byte[] inBlockMetas,
            byte outBlockID, byte outBlockMeta) {
        Validate.isTrue(inBlockIDs.length == inBlockMetas.length);

        srcBoxes.add(srcBox);
        dstBoxes.add(srcBox);
        this.inBlockIDs = inBlockIDs;
        this.inBlockMetas = inBlockMetas;
        this.outBlockID = outBlockID;
        this.outBlockMeta = outBlockMeta;
    }

    @Nonnull
    @Override public List<ImmutablePair<Vector3i, ImmutablePair<Long, CompoundTag>>> actOnCube(Vector3i cubePos, CompoundTag cubeTag, long inCubePriority) {
        List<ImmutablePair<Vector3i, ImmutablePair<Long, CompoundTag>>> outCubes = new ArrayList<>();

        CompoundMap entryLevel = (CompoundMap) cubeTag.getValue().get("Level").getValue();
        entryLevel.put(new ByteTag("isSurfaceTracked", (byte) 0));
        entryLevel.put(new ByteTag("initLightDone", (byte) 0));
        entryLevel.put(new ByteTag("populated", (byte) 1));
        entryLevel.put(new ByteTag("fullyPopulated", (byte) 1));

        CompoundMap sectionDetails;
        try {
            sectionDetails = ((CompoundTag)((List<?>) (entryLevel).get("Sections").getValue()).get(0)).getValue(); //POSSIBLE ARRAY OUT OF BOUNDS EXCEPTION ON A MALFORMED CUBE
        } catch(NullPointerException | ArrayIndexOutOfBoundsException e) {
            LOGGER.warning("Malformed cube at position (" + cubePos.getX() + ", " + cubePos.getY() + ", " + cubePos.getZ() + "), skipping!");
            return outCubes;
        }

        byte[] blocks = (byte[]) sectionDetails.get("Blocks").getValue();
        byte[] meta = (byte[]) sectionDetails.get("Data").getValue();

        for (int idIdx = 0; idIdx < inBlockIDs.length; idIdx++) {
            byte inBlockID = inBlockIDs[idIdx];
            byte inBlockMeta = inBlockMetas[idIdx];
            if(inBlockMeta == -1) { //-1 is a sentinel flag, meaning "any block metadata"
                for (int i = 0; i < 4096; i++) {
                    if (blocks[i] == inBlockID) { //don't check metadata
                        blocks[i] = outBlockID;
                        EditTask.nibbleSetAtIndex(meta, i, outBlockMeta);
                    }
                }
            } else {
                for (int i = 0; i < 4096; i++) {
                    if (blocks[i] == inBlockID && EditTask.nibbleGetAtIndex(meta, i) == inBlockMeta) { //check metadata
                        blocks[i] = outBlockID;
                        EditTask.nibbleSetAtIndex(meta, i, outBlockMeta);
                    }
                }
            }
        }


        outCubes.add(new ImmutablePair<>(cubePos, new ImmutablePair<>(inCubePriority+1, cubeTag)));
        return outCubes;
    }
}
