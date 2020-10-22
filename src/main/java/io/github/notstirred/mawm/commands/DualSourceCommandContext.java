package io.github.notstirred.mawm.commands;

import java.nio.file.Path;

public class DualSourceCommandContext {

    private Path priorityWorld;
    private Path fallbackWorld;
    private Path dstWorld;

    public Path getDstWorld() {
        return dstWorld;
    }
    public void setDstWorld(Path dstWorld) {
        this.dstWorld = dstWorld;
    }

    public Path getPriorityWorld() { return priorityWorld; }
    public void setPriorityWorld(Path priorityWorld) { this.priorityWorld = priorityWorld; }

    public Path getFallbackWorld() { return fallbackWorld; }
    public void setFallbackWorld(Path fallbackWorld) { this.fallbackWorld = fallbackWorld; }
}
