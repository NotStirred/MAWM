package io.github.notstirred.mawm.converter.task.source;

import io.github.notstirred.mawm.converter.task.TaskRequest;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.WorldServer;

import java.nio.file.Path;
import java.nio.file.Paths;

public class ClipboardTaskSource implements TaskRequest.TaskSource {
    private final WorldServer world;
    private final Path basePath;
    private final EntityPlayer player;

    public ClipboardTaskSource(WorldServer world, Path basePath, EntityPlayer player) {
        this.world = world;
        this.basePath = basePath;
        this.player = player;
    }

    @Override
    public Path getPath() {
        return Paths.get(basePath + "/mawm/clipboards/" + player.getUniqueID());
    }
}
