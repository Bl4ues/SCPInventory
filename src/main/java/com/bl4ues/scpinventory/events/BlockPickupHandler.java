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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "scpinventory")
public class BlockPickupHandler {

    private static final long FULL_MESSAGE_COOLDOWN_MS = 350L;
    private static final Map<UUID, Long> LAST_FULL_MESSAGE = new HashMap<>();

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
                showInventoryFullThrottled(serverPlayer);
                return;
            }

            if (acceptedCount >= originalCount) {
                itemEntity.discard();
            } else {
                stack.shrink(acceptedCount);
                itemEntity.setItem(stack);
                itemEntity.setPickUpDelay(10);
                showInventoryFullThrottled(serverPlayer);
            }

            ModNetwork.syncTo(serverPlayer, inventory);
        });
    }

    private static void showInventoryFullThrottled(ServerPlayer player) {
        long now = System.currentTimeMillis();
        UUID playerId = player.getUUID();
        long lastShown = LAST_FULL_MESSAGE.getOrDefault(playerId, 0L);

        if (now - lastShown >= FULL_MESSAGE_COOLDOWN_MS) {
            LAST_FULL_MESSAGE.put(playerId, now);
            ModNetwork.showInventoryFull(player);
        }
    }
}
