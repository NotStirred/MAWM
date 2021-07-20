package io.github.notstirred.mawm.converter.task;

import cubicchunks.converter.lib.util.edittask.EditTask;
import net.minecraft.command.ICommandSender;

import java.util.List;

public class RelocateTaskRequest implements TaskRequest {
    private final ICommandSender sender;
    private final List<EditTask> tasks;

    private final TaskSource srcTaskSource;
    private final TaskSource dstTaskSource;

    private final boolean shouldMoveFilesBackToSrc;

    public RelocateTaskRequest(ICommandSender sender, List<EditTask> tasks, boolean shouldMoveFilesBackToSrc,
           TaskSource srcTaskSource, TaskSource dstTaskSource) {

        this.sender = sender;
        this.tasks = tasks;

        this.shouldMoveFilesBackToSrc = shouldMoveFilesBackToSrc;

        this.srcTaskSource = srcTaskSource;
        this.dstTaskSource = dstTaskSource;
    }

    @Override
    public ICommandSender getSender() {
        return sender;
    }

    @Override
    public List<EditTask> getTasks() {
        return tasks;
    }

    public TaskSource getSrcTaskSource() {
        return srcTaskSource;
    }

    @Override
    public TaskSource getDstTaskSource() {
        return dstTaskSource;
    }

    @Override
    public boolean shouldMoveFilesBackToSrc() {
        return shouldMoveFilesBackToSrc;
    }
}
