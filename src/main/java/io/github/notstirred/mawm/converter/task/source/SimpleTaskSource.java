package io.github.notstirred.mawm.converter.task.source;

import io.github.notstirred.mawm.converter.task.TaskRequest;

import java.nio.file.Path;

public class SimpleTaskSource implements TaskRequest.TaskSource {
    private final Path path;

    public SimpleTaskSource(Path path) {
        this.path = path;
    }

    public Path getPath() {
        return path;
    }
}
