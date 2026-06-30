package com.bl4ues.scpinventory.events;

import com.bl4ues.scpinventory.item.ScpPickupRouter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "scpinventory")
public class BlockPickupHandler {

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        Entity entity = event.getEntity();
        if (entity instanceof ItemEntity itemEntity && !itemEntity.getItem().isEmpty()) {
            ScpPickupRouter.addNoMergeMarker(itemEntity.getItem(), itemEntity.getStringUUID());
        }
    }

    @SubscribeEvent
    public static void onItemPickup(EntityItemPickupEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || player.isCreative() || player.isSpectator()) {
            return;
        }

        event.setCanceled(true);
    }
}
