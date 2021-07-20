package io.github.notstirred.mawm.commands.editing;

import cubicchunks.converter.lib.util.edittask.EditTask;
import io.github.notstirred.mawm.MAWM;
import io.github.notstirred.mawm.asm.mixininterfaces.IFreezableWorld;
import io.github.notstirred.mawm.converter.task.MergeTaskRequest;
import io.github.notstirred.mawm.converter.task.TaskRequest;
import io.github.notstirred.mawm.converter.task.source.BackupTaskSource;
import io.github.notstirred.mawm.converter.task.source.WorldTaskSource;
import io.github.notstirred.mawm.util.LimitedFifoQueue;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.WorldServer;

import java.util.ArrayList;
import java.util.List;

public class CommandRedo extends CommandBase {
    @Override
    public String getName() {
        return "redo";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "mawm.command.redo.usage";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (sender.getCommandSenderEntity() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) sender.getCommandSenderEntity();

            LimitedFifoQueue<TaskRequest> tasksForPlayer = MAWM.INSTANCE.getPlayerTaskHistory().computeIfAbsent(player.getUniqueID(), (uuid) -> new LimitedFifoQueue<>(10));
            if(tasksForPlayer.hasNext()) {
                if(args.length == 0) {
                    List<EditTask> inverseTasks = getInverseForTasks(tasksForPlayer.getNext().getTasks());

                    ((IFreezableWorld) sender.getEntityWorld()).addUndoRedoTask(new MergeTaskRequest(sender, inverseTasks, false,
                        new BackupTaskSource(player), new WorldTaskSource(((WorldServer) player.world)), new WorldTaskSource(((WorldServer) player.world))));
                } else {
                    try {
                        int numSteps = Integer.parseInt(args[0]);

                        for (int i = 0; i < numSteps; i++) {
                            if(!tasksForPlayer.hasNext()) {
                                sender.sendMessage(new TextComponentTranslation("mawm.command.redo.none"));
                                break;
                            }

                            List<EditTask> inverseTasks = getInverseForTasks(tasksForPlayer.getNext().getTasks());

                            ((IFreezableWorld) sender.getEntityWorld()).addUndoRedoTask(new MergeTaskRequest(sender, inverseTasks, false,
                                new BackupTaskSource(player), new WorldTaskSource(((WorldServer) player.world)), new WorldTaskSource(((WorldServer) player.world))));;
                            ((IFreezableWorld) sender.getEntityWorld()).requestUndoRedoTasksExecute();
                            sender.sendMessage(new TextComponentTranslation("mawm.command.redo.completed_i", i));
                        }
                        return;

                    } catch (NumberFormatException e) {
                        sender.sendMessage(new TextComponentTranslation("mawm.command.redo.invalid_steps"));
                        return;
                    }
                }
            } else {
                sender.sendMessage(new TextComponentTranslation("mawm.command.redo.none"));
                return;
            }
        }
        if(MAWM.isQueueMode) {
            sender.sendMessage(new TextComponentTranslation("mawm.command.queued"));
        } else {
            ((IFreezableWorld) sender.getEntityWorld()).requestUndoRedoTasksExecute();
        }
    }

    public static List<EditTask> getInverseForTasks(List<EditTask> tasks) {
        List<EditTask> inverseTasks = new ArrayList<>();
        tasks.forEach(task -> inverseTasks.addAll(task.getInverse()));
        return inverseTasks;
    }
}