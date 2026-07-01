package com.bl4ues.scpinventory.event;

import com.bl4ues.scpinventory.ScpInventoryMod;
import com.bl4ues.scpinventory.capability.ScpInventoryCapability;
import com.bl4ues.scpinventory.item.ScpItemClassifier;
import com.bl4ues.scpinventory.item.ScpPickupRouter;
import com.bl4ues.scpinventory.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ScpInventoryMod.MODID)
public final class ScpCoinPickupEvents {

    private ScpCoinPickupEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onCoinPickup(EntityItemPickupEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isCreative() || player.isSpectator()) {
            return;
        }

        ItemEntity itemEntity = event.getItem();
        if (itemEntity == null || !itemEntity.isAlive()) {
            return;
        }

        ItemStack stack = itemEntity.getItem();
        if (stack.isEmpty() || !ScpItemClassifier.isCoin(stack)) {
            return;
        }

        event.setCanceled(true);
        player.getCapability(ScpInventoryCapability.INSTANCE).ifPresent(inventory -> {
            int accepted = ScpPickupRouter.accept(inventory, player, stack.copy());
            if (accepted <= 0) {
                ModNetwork.showInventoryFull(player);
                ModNetwork.syncTo(player, inventory);
                return;
            }

            player.take(itemEntity, accepted);
            stack.shrink(accepted);
            if (stack.isEmpty()) {
                itemEntity.discard();
            } else {
                itemEntity.setItem(stack);
                itemEntity.setPickUpDelay(20);
            }
            ModNetwork.syncTo(player, inventory);
        });
    }
}
