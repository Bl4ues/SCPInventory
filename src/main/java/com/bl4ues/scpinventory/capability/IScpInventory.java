package com.bl4ues.scpinventory.capability;

import com.bl4ues.scpinventory.item.ScpItemClassifier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public interface IScpInventory {

    int MAIN_SLOT_COUNT = 12;

    List<ItemStack> getInventory();
    void setInventory(List<ItemStack> list);
    ItemStack getInventoryItem(int index);
    boolean setInventoryItem(int index, ItemStack stack);
    boolean isInventoryFull();
    boolean addInventoryItem(ItemStack stack);
    ItemStack extractInventoryItem(int index);
    boolean removeInventoryItem(int index);

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

    ItemStack getHead();
    void setHead(ItemStack stack);
    ItemStack getChest();
    void setChest(ItemStack stack);
    ItemStack getLegs();
    void setLegs(ItemStack stack);
    ItemStack getFeet();
    void setFeet(ItemStack stack);

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
        return MAIN_SLOT_COUNT - getInventoryCount();
    }

    default boolean hasFreeMainSlots(int amount) {
        return amount > 0 && getFreeMainSlots() >= amount;
    }

    default boolean isValidMainSlot(int slot) {
        return slot >= 0 && slot < MAIN_SLOT_COUNT;
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
