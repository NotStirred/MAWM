package io.github.notstirred.mawm.converter.task;

import cubicchunks.converter.lib.util.edittask.EditTask;
import net.minecraft.command.ICommandSender;

import java.nio.file.Path;
import java.util.List;

public interface TaskRequest {
    ICommandSender getSender();

    List<EditTask> getTasks();

    TaskSource getSrcTaskSource();
    TaskSource getDstTaskSource();

    /** This is for things like in place operations, such as clipboard operations */
    boolean shouldMoveFilesBackToSrc();

    interface TaskSource {
        Path getPath();
    }
}
