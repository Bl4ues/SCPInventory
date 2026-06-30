package com.bl4ues.scpinventory.events;

import com.bl4ues.scpinventory.capability.IScpInventory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class PendingConsumableUseManager {

    private PendingConsumableUseManager() {
    }

    public static boolean start(ServerPlayer player, IScpInventory scpInventory, int sourceSlot, ItemStack sourceStack) {
        return false;
    }
}
