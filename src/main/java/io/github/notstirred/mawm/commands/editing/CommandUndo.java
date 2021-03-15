package io.github.notstirred.mawm.commands.editing;

import cubicchunks.converter.lib.util.edittask.EditTask;
import io.github.notstirred.mawm.MAWM;
import io.github.notstirred.mawm.asm.mixininterfaces.IFreezableWorld;
import io.github.notstirred.mawm.util.LimitedFifoQueue;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentTranslation;

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

            LimitedFifoQueue<EditTask> tasksForPlayer = MAWM.INSTANCE.getPlayerTaskHistory().computeIfAbsent(player.getUniqueID(), (uuid) -> new LimitedFifoQueue<>(10));
            if(tasksForPlayer.hasPrev()) {
                if(args.length == 0)
                    ((IFreezableWorld) sender.getEntityWorld()).addUndoTask(sender, tasksForPlayer.getPrev().getInverse());
                else {
                    try {
                        int numSteps = Integer.parseInt(args[0]);
                        if(args.length >= 2) {
                            if(args[1].equalsIgnoreCase("true")) {
                                for (int i = 0; i < numSteps; i++) {
                                    if(!tasksForPlayer.hasPrev())
                                        break;
                                    ((IFreezableWorld) sender.getEntityWorld()).addUndoTask(sender, tasksForPlayer.getPrev().getInverse());
                                }
                                sender.sendMessage(new TextComponentTranslation("mawm.command.undo.completed_all", numSteps));
                            }
                            return;
                        }
                        for (int i = 0; i < numSteps; i++) {
                            if(!tasksForPlayer.hasPrev()) {
                                sender.sendMessage(new TextComponentTranslation("mawm.command.undo.none"));
                                break;
                            }
                            ((IFreezableWorld) sender.getEntityWorld()).addUndoTask(sender, tasksForPlayer.getPrev().getInverse());
                            ((IFreezableWorld) sender.getEntityWorld()).requestUndoTasksExecute();
                            sender.sendMessage(new TextComponentTranslation("mawm.command.undo.completed_i", i));
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
            ((IFreezableWorld) sender.getEntityWorld()).requestUndoTasksExecute();
        }
    }
}
