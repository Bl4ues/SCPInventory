package com.bl4ues.scpinventory.client;

import com.bl4ues.scpinventory.ScpInventoryMod;
import com.bl4ues.scpinventory.capability.IScpInventory;
import com.bl4ues.scpinventory.client.gui.ScpInventoryScreen;
import com.bl4ues.scpinventory.client.gui.components.EquipmentPanel;
import com.bl4ues.scpinventory.client.gui.components.ScrollableItemList;
import com.bl4ues.scpinventory.item.ScpEquipmentSlot;
import com.bl4ues.scpinventory.item.ScpItemClassifier;
import com.bl4ues.scpinventory.network.InventoryActionPacket;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;

@Mod.EventBusSubscriber(modid = ScpInventoryMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ShiftClickEquipHandler {

    private ShiftClickEquipHandler() {
    }

    @SubscribeEvent
    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getButton() != 0 || !Screen.hasShiftDown() || !(event.getScreen() instanceof ScpInventoryScreen screen)) {
            return;
        }

        if (!"INVENTORY".equals(String.valueOf(readField(screen, "mode")))) {
            return;
        }

        IScpInventory inventory = readField(screen, "inventory", IScpInventory.class);
        if (inventory == null) {
            return;
        }

        EquipmentPanel equipmentPanel = readField(screen, "equipmentPanel", EquipmentPanel.class);
        if (equipmentPanel != null) {
            ScpEquipmentSlot clickedSlot = equipmentPanel.getClickedSlot(event.getMouseX(), event.getMouseY());
            if (clickedSlot != null && !inventory.getEquipment(clickedSlot).isEmpty()) {
                ClientInventoryBridge.moveEquipmentToMain(clickedSlot, -1);
                event.setCanceled(true);
                return;
            }
        }

        Boolean showingKeys = readField(screen, "showingKeys", Boolean.class);
        if (Boolean.TRUE.equals(showingKeys)) {
            return;
        }

        ScrollableItemList itemList = readField(screen, "itemList", ScrollableItemList.class);
        if (itemList == null || itemList.clickedDrop(event.getMouseX())) {
            return;
        }

        int index = itemList.getClickedIndex(event.getMouseX(), event.getMouseY());
        if (!inventory.isValidMainSlot(index)) {
            return;
        }

        ItemStack stack = inventory.getInventoryItem(index);
        if (!stack.isEmpty() && ScpItemClassifier.getEquipmentSlot(stack).isPresent()) {
            ClientInventoryBridge.perform(index, InventoryActionPacket.ACTION_EQUIP);
            event.setCanceled(true);
        }
    }

    private static Object readField(Object target, String fieldName) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static <T> T readField(Object target, String fieldName, Class<T> type) {
        Object value = readField(target, fieldName);
        return type.isInstance(value) ? type.cast(value) : null;
    }
}
