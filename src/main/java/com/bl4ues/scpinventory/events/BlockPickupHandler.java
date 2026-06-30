package com.bl4ues.scpinventory.events;

import com.bl4ues.scpinventory.item.ScpPickupRouter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
        if (!(entity instanceof ItemEntity itemEntity) || itemEntity.getItem().isEmpty()) {
            return;
        }

        ItemStack stack = itemEntity.getItem();
        if (stack.getCount() > 1) {
            splitStackEntity(event, itemEntity, stack);
            return;
        }

        ScpPickupRouter.addNoMergeMarker(stack, itemEntity.getStringUUID());
    }

    private static void splitStackEntity(EntityJoinLevelEvent event, ItemEntity original, ItemStack stack) {
        event.setCanceled(true);

        int count = stack.getCount();
        for (int i = 0; i < count; i++) {
            ItemStack single = stack.copy();
            single.setCount(1);
            ScpPickupRouter.addNoMergeMarker(single, original.getStringUUID() + "-" + i);

            double angle = (Math.PI * 2.0D * i) / Math.max(1, count);
            double offsetX = Math.cos(angle) * 0.035D;
            double offsetZ = Math.sin(angle) * 0.035D;

            ItemEntity split = new ItemEntity(
                    event.getLevel(),
                    original.getX() + offsetX,
                    original.getY(),
                    original.getZ() + offsetZ,
                    single
            );
            split.setPickUpDelay(20);
            split.setDeltaMovement(original.getDeltaMovement().add(offsetX, 0.02D, offsetZ));
            event.getLevel().addFreshEntity(split);
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
