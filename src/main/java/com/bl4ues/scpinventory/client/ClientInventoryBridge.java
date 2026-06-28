package com.bl4ues.scpinventory.client;

import com.bl4ues.scpinventory.network.InventoryActionPacket;
import com.bl4ues.scpinventory.network.ModNetwork;

public final class ClientInventoryBridge {

    private ClientInventoryBridge() {
    }

    public static void perform(int slot, String name) {
        ModNetwork.CHANNEL.sendToServer(new InventoryActionPacket(slot, name));
    }
}
