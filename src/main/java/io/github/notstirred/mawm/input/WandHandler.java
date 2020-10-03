package io.github.notstirred.mawm.input;

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
public class WandHandler {
    private static Map<EntityPlayer, AbstractMap.SimpleEntry<BlockPos, BlockPos>> playerWandPositions = new HashMap<>();

    public static AbstractMap.SimpleEntry<BlockPos, BlockPos> getWandLocationsForPlayer(EntityPlayer player) {
        return playerWandPositions.get(player);
    }

    @SubscribeEvent
    public static void leftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.isCanceled() || !event.getWorld().isRemote)
            return;

        if (event.getEntityPlayer().getHeldItem(event.getHand()).getItem() == Items.GOLDEN_AXE) {
            AbstractMap.SimpleEntry<BlockPos, BlockPos> positions = playerWandPositions.get(event.getEntityPlayer());
            if(positions == null)
                positions = new AbstractMap.SimpleEntry<>(null, event.getPos());
            else {
                positions.setValue(event.getPos());
            }
            playerWandPositions.put(event.getEntityPlayer(), positions);
            event.getEntityPlayer().sendMessage(new TextComponentTranslation("mawm.wand.setpos1"));
        }
    }

    @SubscribeEvent
    public static void rightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.isCanceled() || !event.getWorld().isRemote)
            return;

        if (event.getEntityPlayer().getHeldItem(event.getHand()).getItem() == Items.GOLDEN_AXE) {
            AbstractMap.SimpleEntry<BlockPos, BlockPos> positions = playerWandPositions.get(event.getEntityPlayer());
            if(positions == null)
                positions = new AbstractMap.SimpleEntry<>(event.getPos(), null);
            else {
                positions = new AbstractMap.SimpleEntry<>(event.getPos(), positions.getValue());
            }
            playerWandPositions.put(event.getEntityPlayer(), positions);
            event.getEntityPlayer().sendMessage(new TextComponentTranslation("mawm.wand.setpos2"));
        }
    }
}
