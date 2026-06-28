package com.bl4ues.scpinventory.events;

import com.bl4ues.scpinventory.capability.ScpInventoryCapability;
import com.bl4ues.scpinventory.item.ScpPickupRouter;
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
            int originalCount = stack.getCount();
            int acceptedCount = ScpPickupRouter.accept(inventory, stack);
            event.setCanceled(true);

            if (acceptedCount <= 0) {
                ModNetwork.showInventoryFull(serverPlayer);
                return;
            }

            if (acceptedCount >= originalCount) {
                itemEntity.discard();
            } else {
                stack.shrink(acceptedCount);
                itemEntity.setItem(stack);
                itemEntity.setPickUpDelay(10);
                ModNetwork.showInventoryFull(serverPlayer);
            }

            ModNetwork.syncTo(serverPlayer, inventory);
        });
    }
}
