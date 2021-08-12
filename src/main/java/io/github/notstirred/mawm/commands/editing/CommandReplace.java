package io.github.notstirred.mawm.commands.editing;

import cubicchunks.converter.lib.util.BoundingBox;
import cubicchunks.converter.lib.util.ImmutablePair;
import cubicchunks.converter.lib.util.Vector3i;
import io.github.notstirred.mawm.MAWM;
import io.github.notstirred.mawm.asm.mixininterfaces.IFreezableWorld;
import io.github.notstirred.mawm.converter.task.RelocateTaskRequest;
import io.github.notstirred.mawm.converter.task.extra.ManyReplaceEditTask;
import io.github.notstirred.mawm.converter.task.source.WorldTaskSource;
import io.github.notstirred.mawm.input.CubeWandHandler;
import io.github.notstirred.mawm.util.MutablePair;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.WorldServer;

import java.util.Collections;
import java.util.List;

public class CommandReplace extends CommandBase {
    @Override
    public String getName() {
        return "replace";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "mawm.command.replace.usage";
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

            if (args.length < 2) {
                throw new CommandException("mawm.command.replace.no_args");
            }

            byte inBlockId;
            List<Byte> inBlockMetas;
            {
                String[] splitInBlock = args[0].split(":");
                ImmutablePair<Byte, List<Byte>> idMetaPair = MAWM.INSTANCE.nameToIDMetas.get(splitInBlock[0]);
                if(idMetaPair == null) {
                    sender.sendMessage(new TextComponentTranslation("mawm.command.replace.invalid_block", args[0]));
                    return;
                }

                inBlockId = idMetaPair.getFirst();

                List<Byte> validInMetadataValues = idMetaPair.getSecond();
                if (splitInBlock.length == 1) {
                    inBlockMetas = validInMetadataValues; //default to accept all input metadata values
                } else {
                    try {
                        int metaIndex = Integer.parseInt(splitInBlock[1]);
                        if (metaIndex < 0 || metaIndex >= validInMetadataValues.size()) {
                            sender.sendMessage(new TextComponentTranslation("mawm.command.replace.invalid_meta", splitInBlock[0], splitInBlock[1]));
                            return;
                        }
                        inBlockMetas = Collections.singletonList(validInMetadataValues.get(metaIndex));
                    } catch (NumberFormatException e) {
                        sender.sendMessage(new TextComponentTranslation("mawm.command.replace.invalid_meta.nan", splitInBlock[1]));
                        return;
                    }
                }
            }

            byte outBlockId;
            byte outBlockMeta;
            {
                String[] splitOutBlock = args[1].split(":");
                ImmutablePair<Byte, List<Byte>> outMetaPair = MAWM.INSTANCE.nameToIDMetas.get(splitOutBlock[0]);
                if(outMetaPair == null) {
                    sender.sendMessage(new TextComponentTranslation("mawm.command.replace.invalid_block", splitOutBlock[0]));
                    return;
                }

                outBlockId = outMetaPair.getFirst();

                List<Byte> validOutMetadataValues = outMetaPair.getSecond();
                if (splitOutBlock.length == 1) {
                    outBlockMeta = validOutMetadataValues.get(0); //default to the first valid metadata value
                } else {
                    try {
                        int metaIndex = Integer.parseInt(splitOutBlock[1]);
                        if (metaIndex < 0 || metaIndex >= validOutMetadataValues.size()) {
                            sender.sendMessage(new TextComponentTranslation("mawm.command.replace.invalid_meta", splitOutBlock[0], splitOutBlock[1]));
                            return;
                        }
                        outBlockMeta = validOutMetadataValues.get(metaIndex);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(new TextComponentTranslation("mawm.command.replace.invalid_meta.nan", splitOutBlock[1]));
                        return;
                    }
                }
            }

            byte[] inBlockIDPrimitiveArray = new byte[inBlockMetas.size()];
            byte[] inBlockMetaPrimitiveArray = new byte[inBlockMetas.size()];
            for (int i = 0, inBlockMetasSize = inBlockMetas.size(); i < inBlockMetasSize; i++) {
                inBlockIDPrimitiveArray[i] = inBlockId;
                inBlockMetaPrimitiveArray[i] = inBlockMetas.get(i);
            }
            ((IFreezableWorld) sender.getEntityWorld()).addTask(new RelocateTaskRequest(sender, Collections.singletonList(new ManyReplaceEditTask(box,
                inBlockIDPrimitiveArray, inBlockMetaPrimitiveArray,
                outBlockId, outBlockMeta
            )), true, new WorldTaskSource(((WorldServer) player.getEntityWorld())), MAWM.INSTANCE.workingDirectory));
        }
        if(MAWM.isQueueMode) {
            sender.sendMessage(new TextComponentTranslation("mawm.command.queued"));
        } else {
            ((IFreezableWorld) sender.getEntityWorld()).requestTasksExecute();
        }
    }
}
