package com.bl4ues.scpinventory.events;

import com.bl4ues.scpinventory.capability.ScpInventoryCapability;
import com.bl4ues.scpinventory.client.InventoryFullOverlay;

import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

@Mod.EventBusSubscriber(modid = "scpinventory")
public class BlockPickupHandler {

    @SubscribeEvent
    public static void onItemPickup(EntityItemPickupEvent event) {

        Player player = event.getEntity();
        ItemEntity itemEntity = event.getItem();

        if (player.level().isClientSide()) return;

        ItemStack stack = itemEntity.getItem();

        if (stack.isEmpty()) return;

        player.getCapability(ScpInventoryCapability.INSTANCE).ifPresent(inv -> {

            // tenta adicionar no inventário SCP
            boolean added = inv.addInventoryItem(stack);

            if (added) {

                event.setCanceled(true);

                itemEntity.discard();

            } else {

                // inventário cheio → mostra overlay
                InventoryFullOverlay.show();

                event.setCanceled(true);
            }

        });

    }

}