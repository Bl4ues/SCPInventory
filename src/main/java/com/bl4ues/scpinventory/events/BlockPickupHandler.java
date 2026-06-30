package com.bl4ues.scpinventory.events;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "scpinventory")
public class BlockPickupHandler {

    @SubscribeEvent
    public static void onItemPickup(EntityItemPickupEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }

        // The SCP inventory uses deliberate right-click pickup instead of Minecraft's
        // proximity pickup. This prevents items from silently entering the hidden
        // vanilla inventory while the custom inventory is being used.
        event.setCanceled(true);
    }
}
