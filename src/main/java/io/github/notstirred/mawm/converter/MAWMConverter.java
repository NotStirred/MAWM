package io.github.notstirred.mawm.converter;

import cubicchunks.converter.headless.command.HeadlessCommandContext;
import cubicchunks.converter.lib.Registry;
import cubicchunks.converter.lib.conf.ConverterConfig;
import cubicchunks.converter.lib.convert.WorldConverter;
import cubicchunks.converter.lib.util.EditTask;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class MAWMConverter {
    public static void convert(HeadlessCommandContext context, List<EditTask> tasks, Runnable onDone) {
        AtomicBoolean failed = new AtomicBoolean(false);
        ConverterConfig conf = new ConverterConfig(new HashMap<>());

        conf.set("relocations", tasks);

        System.out.println(tasks.get(0).getSourceBox().toString() + tasks.get(0).getOffset().toString() + tasks.get(0).getType().toString());

        WorldConverter<?, ?> converter = new WorldConverter<>(
                Registry.getLevelConverter(context.getInFormat(), context.getOutFormat(), context.getConverterName()).apply(context.getSrcWorld(), context.getDstWorld()),
                Registry.getReader(context.getInFormat()).apply(context.getSrcWorld(), conf),
                Registry.getConverter(context.getInFormat(), context.getOutFormat(), context.getConverterName()).apply(conf),
                Registry.getWriter(context.getOutFormat()).apply(context.getDstWorld())
        );

        MAWMConverterWorker w = new MAWMConverterWorker(converter, onDone, () -> failed.set(true));
        try {
            w.convert();
        } catch (IOException e) {
            failed.set(true);
            e.printStackTrace();
        }
    }
}
