package com.bl4ues.scpinventory.capability;

import com.bl4ues.scpinventory.item.ScpEquipmentSlot;
import com.bl4ues.scpinventory.item.ScpItemClassifier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public interface IScpInventory {

    int DEFAULT_MAIN_SLOT_COUNT = 12;
    int MIN_MAIN_SLOT_COUNT = 1;
    int MAX_MAIN_SLOT_COUNT = 128;
    int MAX_KEY_COUNT = 12;

    List<ItemStack> getInventory();
    void setInventory(List<ItemStack> list);
    ItemStack getInventoryItem(int index);
    boolean setInventoryItem(int index, ItemStack stack);
    boolean isInventoryFull();
    boolean addInventoryItem(ItemStack stack);
    int addInventoryItems(ItemStack stack);
    ItemStack extractInventoryItem(int index);
    boolean removeInventoryItem(int index);

    int getMaxMainSlots();
    void setMaxMainSlots(int slots);
    void resetMainInventory();
    void resetAll();

    List<ItemStack> getKeys();
    void setKeys(List<ItemStack> list);
    boolean addKeyItem(ItemStack stack);
    ItemStack extractKeyItem(int index);
    boolean removeKeyItem(int index);

    List<ItemStack> getDocuments();
    void setDocuments(List<ItemStack> list);
    boolean addDocumentItem(ItemStack stack);
    ItemStack getDocumentItem(int index);
    ItemStack extractDocumentItem(int index);
    boolean removeDocumentItem(int index);

    ItemStack getEquipment(ScpEquipmentSlot slot);
    void setEquipment(ScpEquipmentSlot slot, ItemStack stack);
    ItemStack extractEquipment(ScpEquipmentSlot slot);
    boolean clearEquipment(ScpEquipmentSlot slot);

    CompoundTag serializeNBT();
    void deserializeNBT(CompoundTag tag);

    default int getInventoryCount() {
        int count = 0;
        for (ItemStack stack : getInventory()) {
            if (!stack.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    default int getFreeMainSlots() {
        return getMaxMainSlots() - getInventoryCount();
    }

    default boolean hasFreeMainSlots(int amount) {
        return amount > 0 && getFreeMainSlots() >= amount;
    }

    default int getKeyCount() {
        int count = 0;
        for (ItemStack stack : getKeys()) {
            if (!stack.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    default int getFreeKeySlots() {
        return MAX_KEY_COUNT - getKeyCount();
    }

    default boolean hasFreeKeySlots(int amount) {
        return amount > 0 && getFreeKeySlots() >= amount;
    }

    default boolean isValidMainSlot(int slot) {
        return slot >= 0 && slot < getMaxMainSlots();
    }

    default String getItemType(int slot) {
        if (!isValidMainSlot(slot)) {
            return "Empty";
        }

        ItemStack stack = getInventoryItem(slot);
        if (stack.isEmpty()) {
            return "Empty";
        }

        return ScpItemClassifier.getDisplayType(stack);
    }
}
