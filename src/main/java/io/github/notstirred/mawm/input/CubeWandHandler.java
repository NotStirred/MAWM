package io.github.notstirred.mawm.input;

import cubicchunks.converter.lib.util.Vector3i;
import io.github.notstirred.mawm.util.MutablePair;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber
public class CubeWandHandler {
    private static Map<EntityPlayer, MutablePair<Vector3i, Vector3i>> playerWandPositions = new HashMap<>();

    public static MutablePair<Vector3i, Vector3i> getWandLocationsForPlayer(EntityPlayer player) {
        MutablePair<Vector3i, Vector3i> entry = playerWandPositions.get(player);
        if(entry == null) {
            playerWandPositions.put(player, new MutablePair<>(null, null));
            return playerWandPositions.get(player);
        }
        return entry;
    }

    public static Map<EntityPlayer, MutablePair<Vector3i, Vector3i>> getWandLocations() {
        return playerWandPositions;
    }

    @SubscribeEvent
    public static void leftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.isCanceled() || event.getWorld().isRemote)
            return;

        if (event.getEntityPlayer().getHeldItem(event.getHand()).getItem() == Items.GOLDEN_AXE) {
            MutablePair<Vector3i, Vector3i> positions = playerWandPositions.get(event.getEntityPlayer());
            int posX = event.getPos().getX() >> 4;
            int posY = event.getPos().getY() >> 4;
            int posZ = event.getPos().getZ() >> 4;
            if(positions == null)
                positions = new MutablePair<>(null, new Vector3i(posX, posY, posZ));
            else
                positions.setValue(new Vector3i(posX, posY, posZ));

            playerWandPositions.put(event.getEntityPlayer(), positions);
            event.getEntityPlayer().sendMessage(new TextComponentTranslation("mawm.cubewand.setpos1", posX, posY, posZ));
        }
    }

    @SubscribeEvent
    public static void rightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.isCanceled() || event.getWorld().isRemote)
            return;

        if (event.getEntityPlayer().getHeldItem(event.getHand()).getItem() == Items.GOLDEN_AXE) {
            MutablePair<Vector3i, Vector3i> positions = playerWandPositions.get(event.getEntityPlayer());
            int posX = event.getPos().getX() >> 4;
            int posY = event.getPos().getY() >> 4;
            int posZ = event.getPos().getZ() >> 4;
            if(positions == null)
                positions = new MutablePair<>(new Vector3i(posX, posY, posZ), null);
            else
                positions = new MutablePair<>(new Vector3i(posX, posY, posZ), positions.getValue());

            playerWandPositions.put(event.getEntityPlayer(), positions);
            event.getEntityPlayer().sendMessage(new TextComponentTranslation("mawm.cubewand.setpos2", posX, posY, posZ));
        }
    }
}
