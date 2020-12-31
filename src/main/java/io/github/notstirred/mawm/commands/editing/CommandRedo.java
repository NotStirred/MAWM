package io.github.notstirred.mawm.commands.editing;

import cubicchunks.converter.lib.util.EditTask;
import io.github.notstirred.mawm.MAWM;
import io.github.notstirred.mawm.asm.mixininterfaces.IFreezableWorld;
import io.github.notstirred.mawm.util.LimitedFifoQueue;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentTranslation;

import static io.github.notstirred.mawm.commands.editing.CommandUndo.getInverseForTask;

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

            LimitedFifoQueue<EditTask> tasksForPlayer = MAWM.INSTANCE.getPlayerTaskHistory().computeIfAbsent(player.getUniqueID(), (uuid) -> new LimitedFifoQueue<>(10));
            if(tasksForPlayer.hasNext()) {
                ((IFreezableWorld) sender.getEntityWorld()).addRedoTask(sender, getInverseForTask(tasksForPlayer.getNext()));
            } else {
                sender.sendMessage(new TextComponentTranslation("mawm.command.redo.none"));
                return;
            }
        }
        if(MAWM.isQueueMode) {
            sender.sendMessage(new TextComponentTranslation("mawm.command.queued"));
        } else {
            ((IFreezableWorld) sender.getEntityWorld()).requestRedoTasksExecute();
        }
    }
}