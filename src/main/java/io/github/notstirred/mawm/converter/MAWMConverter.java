package io.github.notstirred.mawm.converter;

import cubicchunks.converter.lib.conf.ConverterConfig;
import cubicchunks.converter.lib.convert.WorldConverter;
import cubicchunks.converter.lib.convert.cc2ccmerging.CC2CCDualSourceMergingDataConverter;
import cubicchunks.converter.lib.convert.cc2ccmerging.CC2CCDualSourceMergingLevelInfoConverter;
import cubicchunks.converter.lib.convert.cc2ccrelocating.CC2CCRelocatingDataConverter;
import cubicchunks.converter.lib.convert.cc2ccrelocating.CC2CCRelocatingLevelInfoConverter;
import cubicchunks.converter.lib.convert.io.*;
import cubicchunks.converter.lib.util.edittask.EditTask;
import io.github.notstirred.mawm.MAWM;
import io.github.notstirred.mawm.converter.task.MergeTaskRequest;
import io.github.notstirred.mawm.converter.task.RelocateTaskRequest;
import io.github.notstirred.mawm.converter.task.TaskRequest;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

public class MAWMConverter {
    public static void convert(TaskRequest taskRequest, Runnable onDone, Consumer<Throwable> onFail) {
        List<EditTask> tasks = taskRequest.getTasks();

        ConverterConfig conf = new ConverterConfig(new HashMap<>());
        conf.set("relocations", tasks);

//        tasks.forEach((task) -> MAWM.LOGGER.debug(task.getSourceBox().toString() + (task.getOffset() != null ? task.getOffset().toString() : "") + task.getType().toString()));
        tasks.forEach(MAWM.LOGGER::debug);

        WorldConverter<?, ?> converter;

        if(taskRequest instanceof RelocateTaskRequest) {
            RelocateTaskRequest relocateTaskRequest = (RelocateTaskRequest) taskRequest;
            Path srcPath = relocateTaskRequest.getSrcTaskSource().getPath();
            Path dstPath = relocateTaskRequest.getDstTaskSource().getPath();

             converter = new WorldConverter<>(
                new NoopCC2CCRelocatingLevelInfoConverter(),
                new PriorityCubicChunkReader(srcPath, conf),
                new CC2CCRelocatingDataConverter(conf),
                new PriorityCubicChunkWriter(dstPath)
            );
        } else {
            MergeTaskRequest mergeTaskRequest = (MergeTaskRequest) taskRequest;
            Path priorityPath = mergeTaskRequest.getPriorityTaskSource().getPath();
            Path fallbackPath = mergeTaskRequest.getSrcTaskSource().getPath();
            Path dstPath = mergeTaskRequest.getDstTaskSource().getPath();

            converter = new WorldConverter<>(
                new NoopCC2CCDualSourceMergingLevelInfoConverter(),
                new DualSourceCubicChunkReader(priorityPath, fallbackPath, conf),
                new CC2CCDualSourceMergingDataConverter(conf),
                new CubicChunkWriter(dstPath)
            );
        }

        MAWMConverterWorker w = new MAWMConverterWorker(converter, onDone, onFail);
        try {
            w.convert();
        } catch (IOException e) {
            MAWM.LOGGER.error(e);
        }
    }

    public static void convertUndoRedo(MergeTaskRequest taskRequest, Runnable onDone, Consumer<Throwable> onFail) {
        ConverterConfig conf = new ConverterConfig(new HashMap<>());

        List<EditTask> tasks = taskRequest.getTasks();
        conf.set("relocations", tasks);

//        tasks.forEach((task) -> MAWM.LOGGER.debug(task.getSourceBox().toString() + (task.getOffset() != null ? task.getOffset().toString() : "") + task.getType().toString()));
        tasks.forEach(MAWM.LOGGER::debug);

        Path priorityPath = taskRequest.getPriorityTaskSource().getPath();
        Path fallbackPath = taskRequest.getSrcTaskSource().getPath();
        Path dstPath = taskRequest.getDstTaskSource().getPath();

        WorldConverter<?, ?> converter = new WorldConverter<>(
            new NoopCC2CCDualSourceMergingLevelInfoConverter(),
            new DualSourceCubicChunkReader(priorityPath, fallbackPath, conf),
            new CC2CCDualSourceMergingDataConverter(conf),
            new CubicChunkWriter(dstPath)
        );

        MAWMConverterWorker w = new MAWMConverterWorker(converter, onDone, onFail);
        try {
            w.convert();
        } catch (IOException e) {
            MAWM.LOGGER.error(e);
        }
    }
}
