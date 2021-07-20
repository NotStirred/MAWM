package io.github.notstirred.mawm.converter.task;

import cubicchunks.converter.lib.util.edittask.EditTask;
import net.minecraft.command.ICommandSender;

import java.util.List;

public class MergeTaskRequest implements TaskRequest {
    private final ICommandSender sender;
    private final List<EditTask> tasks;

    private final boolean shouldMoveFilesBackToSrc;

    private final TaskSource priorityTaskSource;
    private final TaskSource fallbackTaskSource;
    private final TaskSource dstTaskSource;

    public MergeTaskRequest(ICommandSender sender, List<EditTask> tasks, boolean shouldMoveFilesBackToSrc,
            TaskSource priorityTaskSource, TaskSource fallbackTaskSource, TaskSource dstTaskSource) {

        this.sender = sender;
        this.tasks = tasks;

        this.shouldMoveFilesBackToSrc = shouldMoveFilesBackToSrc;

        this.priorityTaskSource = priorityTaskSource;
        this.fallbackTaskSource = fallbackTaskSource;
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

    public TaskSource getPriorityTaskSource() {
        return priorityTaskSource;
    }

    @Override
    public TaskSource getSrcTaskSource() {
        return fallbackTaskSource;
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
