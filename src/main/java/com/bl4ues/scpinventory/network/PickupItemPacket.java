package com.bl4ues.scpinventory.network;

import com.bl4ues.scpinventory.capability.ScpInventoryCapability;
import com.bl4ues.scpinventory.item.ScpPickupRouter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class PickupItemPacket {

    private static final double MAX_PICKUP_DISTANCE_SQR = 6.25D;
    private static final long PICKUP_PACKET_COOLDOWN_MS = 180L;
    private static final Map<UUID, Long> LAST_PICKUP_MS = new HashMap<>();

    private final int entityId;

    public PickupItemPacket(int entityId) {
        this.entityId = entityId;
    }

    public static void encode(PickupItemPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
    }

    public static PickupItemPacket decode(FriendlyByteBuf buf) {
        return new PickupItemPacket(buf.readVarInt());
    }

    public static void handle(PickupItemPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || player.isCreative() || player.isSpectator()) {
                return;
            }

            long now = System.currentTimeMillis();
            UUID playerId = player.getUUID();
            long lastPickup = LAST_PICKUP_MS.getOrDefault(playerId, 0L);
            if (now - lastPickup < PICKUP_PACKET_COOLDOWN_MS) {
                return;
            }
            LAST_PICKUP_MS.put(playerId, now);

            Entity entity = player.serverLevel().getEntity(msg.entityId);
            if (!(entity instanceof ItemEntity itemEntity) || !itemEntity.isAlive()) {
                return;
            }

            if (player.distanceToSqr(itemEntity) > MAX_PICKUP_DISTANCE_SQR) {
                return;
            }

            ItemStack stack = itemEntity.getItem();
            if (stack.isEmpty()) {
                return;
            }

            player.getCapability(ScpInventoryCapability.INSTANCE).ifPresent(inventory -> {
                ItemStack singlePickup = stack.copy();
                singlePickup.setCount(1);

                int acceptedCount = ScpPickupRouter.accept(inventory, player, singlePickup);
                if (acceptedCount <= 0) {
                    ModNetwork.showInventoryFull(player);
                    ModNetwork.syncTo(player, inventory);
                    return;
                }

                playPickupFeedback(player, itemEntity, acceptedCount);

                stack.shrink(1);
                if (stack.isEmpty()) {
                    itemEntity.discard();
                } else {
                    itemEntity.setItem(stack);
                    itemEntity.setPickUpDelay(20);
                }

                ModNetwork.syncTo(player, inventory);
            });
        });
        ctx.get().setPacketHandled(true);
    }

    private static void playPickupFeedback(ServerPlayer player, ItemEntity itemEntity, int acceptedCount) {
        player.take(itemEntity, acceptedCount);
        player.level().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ITEM_PICKUP,
                SoundSource.PLAYERS,
                0.2F,
                ((player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F
        );
    }
}
