package io.github.notstirred.mawm.commands.editing;

import cubicchunks.converter.lib.util.BoundingBox;
import cubicchunks.converter.lib.util.Vector3i;
import cubicchunks.converter.lib.util.edittask.SetEditTask;
import io.github.notstirred.mawm.MAWM;
import io.github.notstirred.mawm.asm.mixininterfaces.IFreezableWorld;
import io.github.notstirred.mawm.converter.task.RelocateTaskRequest;
import io.github.notstirred.mawm.converter.task.source.WorldTaskSource;
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
import net.minecraft.world.WorldServer;

import java.util.Collections;

public class CommandSet extends CommandBase {
    @Override
    public String getName() {
        return "set";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "mawm.command.set.usage";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        EntityPlayer player = null;
        if (sender.getCommandSenderEntity() instanceof EntityPlayer) {
            player = (EntityPlayer) sender.getCommandSenderEntity();
        }
        if (player != null) { //command came from a player
            MutablePair<Vector3i, Vector3i> positions = CubeWandHandler.getWandLocationsForPlayer(player);
            if (positions.getKey() == null)
                throw new CommandException("mawm.cubewand.no_cubewandpos1");
            if (positions.getValue() == null)
                throw new CommandException("mawm.cubewand.no_cubewandpos2");

            BoundingBox box = new BoundingBox(
                    positions.getKey(),
                    positions.getValue()
            );

            if (args.length < 1) {
                throw new CommandException("mawm.command.set.no_args");
            }

            IBlockState state;
            if(args[0].contains(":")) {
                String[] split = args[0].split(":");
                Block inBlock = CommandBase.getBlockByText(sender, split[0]);
                state = CommandBase.convertArgToBlockState(inBlock, split[1]);
            } else {
                Block inBlock = CommandBase.getBlockByText(sender, args[0]);
                state = inBlock.getDefaultState();
            }
            @SuppressWarnings("deprecation")
            int id = Block.BLOCK_STATE_IDS.get(state);
            ((IFreezableWorld) sender.getEntityWorld()).addTask(new RelocateTaskRequest(sender, Collections.singletonList(new SetEditTask(box, (byte)(id >> 4 & 255), (byte)(id & 15))), true,
                new WorldTaskSource(((WorldServer) player.getEntityWorld())), MAWM.INSTANCE.workingDirectory));
        }
        if(MAWM.isQueueMode) {
            sender.sendMessage(new TextComponentTranslation("mawm.command.queued"));
        } else {
            ((IFreezableWorld) sender.getEntityWorld()).requestTasksExecute();
        }
    }
}
