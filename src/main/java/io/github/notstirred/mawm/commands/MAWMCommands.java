package io.github.notstirred.mawm.commands;

import cubicchunks.converter.lib.util.BoundingBox;
import cubicchunks.converter.lib.util.Vector3i;
import io.github.notstirred.mawm.commands.editing.*;
import io.github.notstirred.mawm.commands.selection.CommandCubePos1;
import io.github.notstirred.mawm.commands.selection.CommandCubePos2;
import io.github.notstirred.mawm.commands.selection.CommandExpand;
import net.minecraft.command.ICommandSender;
import net.minecraftforge.server.command.CommandTreeBase;

import static java.lang.Math.floorDiv;

public class MAWMCommands extends CommandTreeBase {
    public MAWMCommands() {
        super.addSubcommand(new CommandExecute());
        super.addSubcommand(new CommandUndo());
        super.addSubcommand(new CommandRedo());
        super.addSubcommand(new CommandCut());
        super.addSubcommand(new CommandCopy());
        super.addSubcommand(new CommandRegen());
        super.addSubcommand(new CommandMoveRegen());
        super.addSubcommand(new CommandSet());
        super.addSubcommand(new CommandReplace());
        super.addSubcommand(new CommandCubePos1());
        super.addSubcommand(new CommandCubePos2());
        super.addSubcommand(new CommandExpand());
    }

    /**
     * Gets the name of the command
     */
    @Override
    public String getName() {
        return "mawm";
    }


    @Override
    public String getUsage(ICommandSender icommandsender) {
        return "mawm.command.commands.usage";
    }

    public static BoundingBox blockToCubeBox(Vector3i pos1, Vector3i pos2) {
        BoundingBox box = new BoundingBox(pos1, pos2);

        Vector3i minPos = box.getMinPos();
        Vector3i maxPos = box.getMaxPos();

        box = new BoundingBox(
                floorDiv(minPos.getX(), 16),
                floorDiv(minPos.getY(), 16),
                floorDiv(minPos.getZ(), 16),
                floorDiv(maxPos.getX(), 16),
                floorDiv(maxPos.getY(), 16),
                floorDiv(maxPos.getZ(), 16)
        );

        return box;
    }
}
