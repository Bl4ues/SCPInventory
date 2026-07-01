package com.bl4ues.scpinventory.network;

import com.bl4ues.scpinventory.capability.IScpInventory;
import com.bl4ues.scpinventory.capability.ScpInventoryCapability;
import com.bl4ues.scpinventory.item.ScpItemClassifier;
import com.bl4ues.scpinventory.item.ScpItemType;
import com.bl4ues.scpinventory.item.ScpPickupRouter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class PickupItemPacket {

    private static final double MAX_PICKUP_DISTANCE_SQR = 6.25D;
    private static final long PICKUP_PACKET_COOLDOWN_MS = 180L;
    private static final long SAME_ENTITY_COOLDOWN_TICKS = 5L;
    private static final int VANILLA_MAIN_START = 9;
    private static final int VANILLA_MAIN_END_EXCLUSIVE = 36;
    private static final Map<UUID, Long> LAST_PICKUP_MS = new HashMap<>();
    private static final Map<UUID, Integer> LAST_PICKUP_ENTITY = new HashMap<>();
    private static final Map<UUID, Long> LAST_PICKUP_GAME_TICK = new HashMap<>();

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

            long gameTime = player.serverLevel().getGameTime();
            int lastEntityId = LAST_PICKUP_ENTITY.getOrDefault(playerId, Integer.MIN_VALUE);
            long lastGameTick = LAST_PICKUP_GAME_TICK.getOrDefault(playerId, Long.MIN_VALUE);
            if (lastEntityId == msg.entityId && gameTime - lastGameTick <= SAME_ENTITY_COOLDOWN_TICKS) {
                return;
            }

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
                ItemStack pickupStack = stack.copy();
                pickupStack.setCount(1);

                int acceptedCount = ScpPickupRouter.accept(inventory, player, pickupStack);
                if (acceptedCount <= 0) {
                    LAST_PICKUP_MS.put(playerId, now);
                    LAST_PICKUP_ENTITY.put(playerId, msg.entityId);
                    LAST_PICKUP_GAME_TICK.put(playerId, gameTime);
                    if (shouldShowInventoryFull(player, inventory, pickupStack)) {
                        ModNetwork.showInventoryFull(player);
                    }
                    ModNetwork.syncTo(player, inventory);
                    return;
                }

                LAST_PICKUP_MS.put(playerId, now);
                LAST_PICKUP_ENTITY.put(playerId, msg.entityId);
                LAST_PICKUP_GAME_TICK.put(playerId, gameTime);

                playPickupFeedback(player, itemEntity, acceptedCount);

                stack.shrink(acceptedCount);
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

    private static boolean shouldShowInventoryFull(ServerPlayer player, IScpInventory inventory, ItemStack stack) {
        if (player == null || inventory == null || stack == null || stack.isEmpty() || ScpPickupRouter.isCoinMirror(stack)) {
            return false;
        }

        if (ScpItemClassifier.isCoin(stack)) {
            return inventory.getFreeMainSlots() <= 0 || !hasFreeVanillaMirrorSlot(player);
        }

        ScpItemType type = ScpItemClassifier.getType(stack);
        if (type == ScpItemType.KEY) {
            return inventory.getFreeKeySlots() <= 0 || !hasFreeVanillaMirrorSlot(player);
        }

        if (type == ScpItemType.CODEX) {
            return false;
        }

        return inventory.getFreeMainSlots() <= 0;
    }

    private static boolean hasFreeVanillaMirrorSlot(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        for (int i = VANILLA_MAIN_START; i < VANILLA_MAIN_END_EXCLUSIVE && i < inventory.items.size(); i++) {
            if (inventory.items.get(i).isEmpty()) {
                return true;
            }
        }
        return false;
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
