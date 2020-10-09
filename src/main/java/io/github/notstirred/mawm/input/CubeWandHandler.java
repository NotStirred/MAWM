package io.github.notstirred.mawm.input;

import cubicchunks.converter.lib.util.Vector3i;
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
    //Map of player to Pair<CubePos, CubePos>
    private static Map<EntityPlayer, AbstractMap.SimpleEntry<Vector3i, Vector3i>> playerWandPositions = new HashMap<>();

    public static AbstractMap.SimpleEntry<Vector3i, Vector3i> getWandLocationsForPlayer(EntityPlayer player) {
        AbstractMap.SimpleEntry<Vector3i, Vector3i> entry = playerWandPositions.get(player);
        if(entry == null) {
            playerWandPositions.put(player, new AbstractMap.SimpleEntry<>(null, null));
            return playerWandPositions.get(player);
        }
        return entry;
    }

    public static Map<EntityPlayer, AbstractMap.SimpleEntry<Vector3i, Vector3i>> getWandLocations() {
        return playerWandPositions;
    }

    @SubscribeEvent
    public static void leftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.isCanceled() || !event.getWorld().isRemote)
            return;

        if (event.getEntityPlayer().getHeldItem(event.getHand()).getItem() == Items.GOLDEN_AXE) {
            AbstractMap.SimpleEntry<Vector3i, Vector3i> positions = playerWandPositions.get(event.getEntityPlayer());
            if(positions == null)
                positions = new AbstractMap.SimpleEntry<>(null, new Vector3i(event.getPos().getX(), event.getPos().getY(), event.getPos().getZ()));
            else {
                positions.setValue(new Vector3i(event.getPos().getX(), event.getPos().getY(), event.getPos().getZ()));
            }
            playerWandPositions.put(event.getEntityPlayer(), positions);
            event.getEntityPlayer().sendMessage(new TextComponentTranslation("mawm.cubewand.setpos1"));
        }
    }

    @SubscribeEvent
    public static void rightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.isCanceled() || !event.getWorld().isRemote)
            return;

        if (event.getEntityPlayer().getHeldItem(event.getHand()).getItem() == Items.GOLDEN_AXE) {
            AbstractMap.SimpleEntry<Vector3i, Vector3i> positions = playerWandPositions.get(event.getEntityPlayer());
            if(positions == null)
                positions = new AbstractMap.SimpleEntry<>(new Vector3i(event.getPos().getX(), event.getPos().getY(), event.getPos().getZ()), null);
            else {
                positions = new AbstractMap.SimpleEntry<>(new Vector3i(event.getPos().getX(), event.getPos().getY(), event.getPos().getZ()), positions.getValue());
            }
            playerWandPositions.put(event.getEntityPlayer(), positions);
            event.getEntityPlayer().sendMessage(new TextComponentTranslation("mawm.cubewand.setpos2"));
        }
    }
}
