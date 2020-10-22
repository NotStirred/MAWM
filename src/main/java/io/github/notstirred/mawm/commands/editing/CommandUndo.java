package io.github.notstirred.mawm.commands.editing;

import cubicchunks.converter.lib.util.BlockEditTask;
import cubicchunks.converter.lib.util.BoundingBox;
import cubicchunks.converter.lib.util.EditTask;
import cubicchunks.converter.lib.util.Vector3i;
import io.github.notstirred.mawm.MAWM;
import io.github.notstirred.mawm.asm.mixininterfaces.IFreezableWorld;
import io.github.notstirred.mawm.input.CubeWandHandler;
import io.github.notstirred.mawm.util.MutablePair;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentTranslation;

import java.util.ArrayList;
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

            ((IFreezableWorld) sender.getEntityWorld()).addUndoTask(sender, getInverseForTask(MAWM.INSTANCE.getPlayerTaskHistory().get(player.getUniqueID()).pop()));
        }
        if(MAWM.isQueueMode) {
            sender.sendMessage(new TextComponentTranslation("mawm.command.queued"));
        } else {
            ((IFreezableWorld) sender.getEntityWorld()).requestUndoTasksExecute();
        }
    }

    private static List<EditTask> getInverseForTask(EditTask task) {
        List<EditTask> tasks = new ArrayList<>();
        switch (task.getType()) {
            case COPY:
                tasks.add(new EditTask(task.getSourceBox().add(task.getOffset()), new Vector3i(0, 0, 0), EditTask.Type.MOVE));
                break;

            case CUT:
                tasks.add(new EditTask(task.getSourceBox(), new Vector3i(0, 0, 0), EditTask.Type.MOVE));
                if(task.getOffset() != null)
                    tasks.add(new EditTask(task.getSourceBox().add(task.getOffset()), new Vector3i(0, 0, 0), EditTask.Type.MOVE));
                break;

            case MOVE:
                tasks.add(new EditTask(task.getSourceBox(), new Vector3i(0, 0, 0), EditTask.Type.MOVE));
                tasks.add(new EditTask(task.getSourceBox().add(task.getOffset()), new Vector3i(0, 0, 0), EditTask.Type.MOVE));
                break;

            case REMOVE:
                tasks.add(new EditTask(task.getSourceBox(), new Vector3i(0, 0, 0), EditTask.Type.MOVE));
                break;

            case SET:
                tasks.add(new EditTask(task.getSourceBox(), new Vector3i(0, 0, 0), EditTask.Type.MOVE));
                break;

            case REPLACE:
                tasks.add(new EditTask(task.getSourceBox(), new Vector3i(0, 0, 0), EditTask.Type.MOVE));
                break;

            case KEEP:
            case NONE:
            default:
                break;
        }
        return tasks;
    }
}
