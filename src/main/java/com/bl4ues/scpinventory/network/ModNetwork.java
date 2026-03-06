package com.bl4ues.scpinventory.network;

import com.bl4ues.scpinventory.ScpInventoryMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetwork {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ScpInventoryMod.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        int id = 0;

        CHANNEL.registerMessage(
                id++,
                InventoryFullPacket.class,
                InventoryFullPacket::encode,
                InventoryFullPacket::decode,
                InventoryFullPacket::handle
        );
    }
}