package io.github.notstirred.mawm.commands.selection;

import cubicchunks.converter.lib.util.Vector3i;
import io.github.notstirred.mawm.input.CubeWandHandler;
import io.github.notstirred.mawm.util.MutablePair;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.server.command.TextComponentHelper;

import java.util.Locale;

public class CommandExpand extends CommandBase {
    @Override
    public String getName() {
        return "expand";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "mawm.command.expand.usage";
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

            Vector3i pos1 = positions.getKey();
            Vector3i pos2 = positions.getValue();

            String inDir = args[0].toLowerCase(Locale.ROOT);
            EnumFacing dir;
            switch(inDir) {
                case "up": case "u": {
                    dir = EnumFacing.UP;
                    break;
                }
                case "down": case "d": {
                    dir = EnumFacing.DOWN;
                    break;
                }
                case "left": case "l": {
                    dir = rotateHorizontal(player.getAdjustedHorizontalFacing(), -1);
                    break;
                }
                case "right": case "r": {
                    dir = rotateHorizontal(player.getAdjustedHorizontalFacing(), 1);
                    break;
                }
                case "forward": case "for": case "f": {
                    dir = player.getAdjustedHorizontalFacing();
                    break;
                }
                case "backward": case "back": case "b": {
                    dir = rotateHorizontal(player.getAdjustedHorizontalFacing(), 2);
                    break;
                }
                default: {
                    dir = EnumFacing.byName(inDir);
                    break;
                }
            }
            if(dir == null) {
                sender.sendMessage(TextComponentHelper.createComponentTranslation(sender, "mawm.command.expand.invalid_dir", args[0]));
                return;
            }
            int expandAmount;
            try {
                expandAmount = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage(TextComponentHelper.createComponentTranslation(sender, "mawm.command.expand.invalid_value", args[1]));
                return;
            }

            switch(dir) {
                case UP: {
                    int y1 = pos1.getY();
                    int y2 = pos2.getY();
                    if(y1 > y2)
                        pos1 = new Vector3i(pos1.getX(), y1 + expandAmount, pos1.getZ());
                    else
                        pos2 = new Vector3i(pos2.getX(), y2 + expandAmount, pos2.getZ());
                    break;
                }
                case DOWN: {
                    int y1 = pos1.getY();
                    int y2 = pos2.getY();
                    if(y1 < y2)
                        pos1 = new Vector3i(pos1.getX(), y1 - expandAmount, pos1.getZ());
                    else
                        pos2 = new Vector3i(pos2.getX(), y2 - expandAmount, pos2.getZ());
                    break;
                }
                case NORTH: {
                    int z1 = pos1.getZ();
                    int z2 = pos2.getZ();
                    if(z1 < z2)
                        pos1 = new Vector3i(pos1.getX(), pos1.getY(), z1 - expandAmount);
                    else
                        pos2 = new Vector3i(pos2.getX(), pos2.getY(), z2 - expandAmount);
                    break;
                }
                case SOUTH: {
                    int z1 = pos1.getZ();
                    int z2 = pos2.getZ();
                    if(z1 > z2)
                        pos1 = new Vector3i(pos1.getX(), pos1.getY(), z1 + expandAmount);
                    else
                        pos2 = new Vector3i(pos2.getX(), pos2.getY(), z2 + expandAmount);
                    break;
                }
                case EAST: {
                    int x1 = pos1.getX();
                    int x2 = pos2.getX();
                    if(x1 > x2)
                        pos1 = new Vector3i(x1 + expandAmount, pos1.getY(), pos1.getZ());
                    else
                        pos2 = new Vector3i(x2 + expandAmount, pos2.getY(), pos2.getZ());
                    break;
                }
                case WEST: {
                    int x1 = pos1.getX();
                    int x2 = pos2.getX();
                    if(x1 < x2)
                        pos1 = new Vector3i(x1 - expandAmount, pos1.getY(), pos1.getZ());
                    else
                        pos2 = new Vector3i(x2 - expandAmount, pos2.getY(), pos2.getZ());
                    break;
                }
            }
            positions.setKey(pos1);
            positions.setValue(pos2);
        }
    }

    private static EnumFacing rotateHorizontal(EnumFacing dir, int steps) {
        return EnumFacing.byHorizontalIndex((dir.getHorizontalIndex() + steps) % 4);
    }
}
