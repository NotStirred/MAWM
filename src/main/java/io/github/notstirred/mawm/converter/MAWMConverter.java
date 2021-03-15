package io.github.notstirred.mawm.converter;

import cubicchunks.converter.headless.command.HeadlessCommandContext;
import cubicchunks.converter.lib.conf.ConverterConfig;
import cubicchunks.converter.lib.convert.WorldConverter;
import cubicchunks.converter.lib.convert.cc2ccmerging.CC2CCDualSourceMergingDataConverter;
import cubicchunks.converter.lib.convert.cc2ccmerging.CC2CCDualSourceMergingLevelInfoConverter;
import cubicchunks.converter.lib.convert.cc2ccrelocating.CC2CCRelocatingDataConverter;
import cubicchunks.converter.lib.convert.cc2ccrelocating.CC2CCRelocatingLevelInfoConverter;
import cubicchunks.converter.lib.convert.io.CubicChunkReader;
import cubicchunks.converter.lib.convert.io.CubicChunkWriter;
import cubicchunks.converter.lib.convert.io.DualSourceCubicChunkReader;
import cubicchunks.converter.lib.util.edittask.EditTask;
import io.github.notstirred.mawm.MAWM;
import io.github.notstirred.mawm.commands.DualSourceCommandContext;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

public class MAWMConverter {
    public static void convert(HeadlessCommandContext context, List<EditTask> tasks, Runnable onDone, Consumer<Throwable> onFail) {
        ConverterConfig conf = new ConverterConfig(new HashMap<>());

        conf.set("relocations", tasks);

//        tasks.forEach((task) -> MAWM.LOGGER.debug(task.getSourceBox().toString() + (task.getOffset() != null ? task.getOffset().toString() : "") + task.getType().toString()));
        tasks.forEach(MAWM.LOGGER::debug);

        WorldConverter<?, ?> converter = new WorldConverter<>(
            new CC2CCRelocatingLevelInfoConverter(context.getSrcWorld(), context.getDstWorld()),
            new CubicChunkReader(context.getSrcWorld(), conf),
            new CC2CCRelocatingDataConverter(conf),
            new CubicChunkWriter(context.getDstWorld())
        );

        MAWMConverterWorker w = new MAWMConverterWorker(converter, onDone, onFail);
        try {
            w.convert();
        } catch (IOException e) {
            MAWM.LOGGER.error(e);
        }
    }

    public static void convertDualSource(DualSourceCommandContext context, List<EditTask> tasks, Runnable onDone, Consumer<Throwable> onFail) {
        ConverterConfig conf = new ConverterConfig(new HashMap<>());

        conf.set("relocations", tasks);

//        tasks.forEach((task) -> MAWM.LOGGER.debug(task.getSourceBox().toString() + (task.getOffset() != null ? task.getOffset().toString() : "") + task.getType().toString()));
        tasks.forEach(MAWM.LOGGER::debug);

        WorldConverter<?, ?> converter = new WorldConverter<>(
            new CC2CCDualSourceMergingLevelInfoConverter(context.getPriorityWorld(), context.getFallbackWorld(), context.getDstWorld()),
            new DualSourceCubicChunkReader(context.getPriorityWorld(), context.getFallbackWorld(), conf),
            new CC2CCDualSourceMergingDataConverter(conf),
            new CubicChunkWriter(context.getDstWorld())
        );

        MAWMConverterWorker w = new MAWMConverterWorker(converter, onDone, onFail);
        try {
            w.convert();
        } catch (IOException e) {
            MAWM.LOGGER.error(e);
        }
    }
}
