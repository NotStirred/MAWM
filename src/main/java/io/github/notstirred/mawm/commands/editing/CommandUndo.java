package io.github.notstirred.mawm.commands.editing;

import cubicchunks.converter.lib.util.BoundingBox;
import cubicchunks.converter.lib.util.EditTask;
import cubicchunks.converter.lib.util.Vector3i;
import io.github.notstirred.mawm.MAWM;
import io.github.notstirred.mawm.asm.mixininterfaces.IFreezableWorld;
import io.github.notstirred.mawm.util.LimitedFifoQueue;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.server.command.TextComponentHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
                    ((IFreezableWorld) sender.getEntityWorld()).addUndoTask(sender, getInverseForTask(tasksForPlayer.getPrev()));
                else {
                    try {
                        int numSteps = Integer.parseInt(args[0]);
                        if(args.length >= 2) {
                            if(args[1].equalsIgnoreCase("true")) {
                                for (int i = 0; i < numSteps; i++) {
                                    if(!tasksForPlayer.hasPrev())
                                        break;
                                    ((IFreezableWorld) sender.getEntityWorld()).addUndoTask(sender, getInverseForTask(tasksForPlayer.getPrev()));
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
                            ((IFreezableWorld) sender.getEntityWorld()).addUndoTask(sender, getInverseForTask(tasksForPlayer.getPrev()));
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

    public static List<EditTask> getInverseForTask(EditTask task) {
        List<EditTask> tasks = new ArrayList<>();
        BoundingBox sourceBox = task.getSourceBox();
        switch (task.getType()) {
            case COPY:
                tasks.add(new EditTask(sourceBox.add(task.getOffset()), new Vector3i(0, 0, 0), EditTask.Type.MOVE));
                break;

            case CUT:
                tasks.add(new EditTask(sourceBox, new Vector3i(0, 0, 0), EditTask.Type.MOVE));
                if(task.getOffset() != null)
                    tasks.add(new EditTask(sourceBox.add(task.getOffset()), new Vector3i(0, 0, 0), EditTask.Type.MOVE));
                break;

            case MOVE:
                tasks.add(new EditTask(sourceBox, new Vector3i(0, 0, 0), EditTask.Type.MOVE));
                tasks.add(new EditTask(sourceBox.add(task.getOffset()), new Vector3i(0, 0, 0), EditTask.Type.MOVE));
                break;

            case REMOVE:
                tasks.add(new EditTask(sourceBox, new Vector3i(0, 0, 0), EditTask.Type.MOVE));
                break;

            case SET:
                tasks.add(new EditTask(sourceBox, new Vector3i(0, 0, 0), EditTask.Type.MOVE));
                break;

            case REPLACE:
                tasks.add(new EditTask(sourceBox, new Vector3i(0, 0, 0), EditTask.Type.MOVE));
                break;

            case KEEP:
            case NONE:
            default:
                break;
        }
        return tasks;
    }
}
