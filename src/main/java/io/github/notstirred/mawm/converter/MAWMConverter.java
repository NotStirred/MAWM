package io.github.notstirred.mawm.converter;

import cubicchunks.converter.headless.command.HeadlessCommandContext;
import cubicchunks.converter.lib.Registry;
import cubicchunks.converter.lib.conf.ConverterConfig;
import cubicchunks.converter.lib.convert.WorldConverter;
import cubicchunks.converter.lib.util.EditTask;
import io.github.notstirred.mawm.MAWM;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

public class MAWMConverter {
    public static void convert(HeadlessCommandContext context, List<EditTask> tasks, Runnable onDone, Consumer<Throwable> onFail) {
        ConverterConfig conf = new ConverterConfig(new HashMap<>());

        conf.set("relocations", tasks);

        tasks.forEach((task) -> MAWM.LOGGER.debug(task.getSourceBox().toString() + (task.getOffset() != null ? task.getOffset().toString() : "") + task.getType().toString()));

        WorldConverter<?, ?> converter = new WorldConverter<>(
                Registry.getLevelConverter(context.getInFormat(), context.getOutFormat(), context.getConverterName()).apply(context.getSrcWorld(), context.getDstWorld()),
                Registry.getReader(context.getInFormat()).apply(context.getSrcWorld(), conf),
                Registry.getConverter(context.getInFormat(), context.getOutFormat(), context.getConverterName()).apply(conf),
                Registry.getWriter(context.getOutFormat()).apply(context.getDstWorld())
        );

        MAWMConverterWorker w = new MAWMConverterWorker(converter, onDone, onFail);
        try {
            w.convert();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
