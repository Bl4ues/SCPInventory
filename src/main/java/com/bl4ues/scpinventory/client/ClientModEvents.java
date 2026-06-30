package com.bl4ues.scpinventory.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "scpinventory", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("player_vitals_overlay",
                (gui, guiGraphics, partialTick, width, height) -> {
                    PlayerVitalsOverlay.render(guiGraphics, width, height, partialTick);
                });

        event.registerAboveAll("pickup_prompt_overlay",
                (gui, guiGraphics, partialTick, width, height) -> {
                    PickupPromptClient.render(guiGraphics, width, height, partialTick);
                });

        event.registerAboveAll("inventory_full_overlay",
                (gui, guiGraphics, partialTick, width, height) -> {
                    InventoryFullOverlay.render(guiGraphics);
                });
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(Keybinds.OPEN_SCP_INVENTORY);
    }
}
