package io.github.notstirred.mawm.converter.task.source;

import io.github.notstirred.mawm.converter.task.TaskRequest;
import net.minecraft.world.WorldServer;

import java.nio.file.Path;

public class WorldTaskSource implements TaskRequest.TaskSource {
    private final WorldServer world;

    public WorldTaskSource(WorldServer world) {
        this.world = world;
    }

    @Override
    public Path getPath() {
        return world.getSaveHandler().getWorldDirectory().toPath();
    }
}
