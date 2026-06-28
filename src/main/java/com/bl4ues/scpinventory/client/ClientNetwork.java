package com.bl4ues.scpinventory.client;

import com.bl4ues.scpinventory.network.ModNetwork;
import com.bl4ues.scpinventory.network.RequestInventorySyncPacket;

public final class ClientNetwork {

    private ClientNetwork() {
    }

    public static void requestInventorySync() {
        ModNetwork.CHANNEL.sendToServer(new RequestInventorySyncPacket());
    }
}
