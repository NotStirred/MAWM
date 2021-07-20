package io.github.notstirred.mawm.commands.editing;

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

public class CommandUndo extends CommandBase {
    @Override
    public String getName() {
        return "undo";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "mawm.command.undo.usage";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (sender.getCommandSenderEntity() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) sender.getCommandSenderEntity();

            LimitedFifoQueue<TaskRequest> tasksForPlayer = MAWM.INSTANCE.getPlayerTaskHistory().computeIfAbsent(player.getUniqueID(), (uuid) -> new LimitedFifoQueue<>(10));
            if(tasksForPlayer.hasPrev()) {
                if(args.length == 0)
                    ((IFreezableWorld) sender.getEntityWorld()).addUndoRedoTask(new MergeTaskRequest(sender, CommandRedo.getInverseForTasks(tasksForPlayer.getPrev().getTasks()), false,
                        new BackupTaskSource(player), new WorldTaskSource(((WorldServer) player.getEntityWorld())), new WorldTaskSource((WorldServer) player.getEntityWorld())));
                else {
                    try {
                        int numSteps = Integer.parseInt(args[0]);

                        for (int i = 0; i < numSteps; i++) {
                            if(!tasksForPlayer.hasPrev()) {
                                sender.sendMessage(new TextComponentTranslation("mawm.command.undo.none"));
                                break;
                            }
                            ((IFreezableWorld) sender.getEntityWorld()).addUndoRedoTask(new MergeTaskRequest(sender, CommandRedo.getInverseForTasks(tasksForPlayer.getPrev().getTasks()), false,
                                new BackupTaskSource(player), new WorldTaskSource(((WorldServer) player.getEntityWorld())), new WorldTaskSource((WorldServer) player.getEntityWorld())));
                            sender.sendMessage(new TextComponentTranslation("mawm.command.undo.completed_i", i));
                            ((IFreezableWorld) sender.getEntityWorld()).requestUndoRedoTasksExecute();
                        }
                        return;

                    } catch (NumberFormatException e) {
                        sender.sendMessage(new TextComponentTranslation("mawm.command.undo.invalid_steps"));
                        return;
                    }
                }
            } else {
                sender.sendMessage(new TextComponentTranslation("mawm.command.undo.none"));
                return;
            }
        }
        if(MAWM.isQueueMode) {
            sender.sendMessage(new TextComponentTranslation("mawm.command.queued"));
        } else {
            ((IFreezableWorld) sender.getEntityWorld()).requestUndoRedoTasksExecute();
        }
    }
}
