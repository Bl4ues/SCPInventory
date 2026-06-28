package com.bl4ues.scpinventory.client;

import com.bl4ues.scpinventory.item.ScpEquipmentSlot;
import com.bl4ues.scpinventory.network.EquipmentActionPacket;
import com.bl4ues.scpinventory.network.InventoryActionPacket;
import com.bl4ues.scpinventory.network.ModNetwork;

public final class ClientInventoryBridge {

    private ClientInventoryBridge() {
    }

    public static void perform(int slot, String name) {
        ModNetwork.CHANNEL.sendToServer(new InventoryActionPacket(slot, name));
    }

    public static void performEquipment(ScpEquipmentSlot slot, String name) {
        if (slot == null) {
            return;
        }

        ModNetwork.CHANNEL.sendToServer(new EquipmentActionPacket(slot.name(), name));
    }
}
