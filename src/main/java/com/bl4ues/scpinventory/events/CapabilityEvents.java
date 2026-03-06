package com.bl4ues.scpinventory.events;

import com.bl4ues.scpinventory.capability.ScpInventoryCapability;
import com.bl4ues.scpinventory.capability.ScpInventoryProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "scpinventory")
public class CapabilityEvents {

    private static final ResourceLocation ID =
            new ResourceLocation("scpinventory", "scp_inventory");

    @SubscribeEvent
    public static void attachCapability(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(ID, new ScpInventoryProvider());
        }
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {

        event.getOriginal().reviveCaps();

        event.getOriginal().getCapability(ScpInventoryCapability.INSTANCE).ifPresent(oldCap -> {

            event.getEntity().getCapability(ScpInventoryCapability.INSTANCE).ifPresent(newCap -> {

                newCap.deserializeNBT(oldCap.serializeNBT());

            });

        });

        event.getOriginal().invalidateCaps();
    }
}