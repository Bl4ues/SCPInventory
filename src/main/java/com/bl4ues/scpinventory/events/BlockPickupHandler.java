package com.bl4ues.scpinventory.events;

import com.bl4ues.scpinventory.capability.ScpInventoryCapability;
import com.bl4ues.scpinventory.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "scpinventory")
public class BlockPickupHandler {

    @SubscribeEvent
    public static void onItemPickup(EntityItemPickupEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        ItemEntity itemEntity = event.getItem();
        ItemStack stack = itemEntity.getItem();
        if (stack.isEmpty()) {
            return;
        }

        player.getCapability(ScpInventoryCapability.INSTANCE).ifPresent(inventory -> {
            boolean added = inventory.addInventoryItem(stack);
            event.setCanceled(true);

            if (added) {
                itemEntity.discard();
                ModNetwork.syncTo(serverPlayer, inventory);
            } else {
                ModNetwork.showInventoryFull(serverPlayer);
            }
        });
    }
}
