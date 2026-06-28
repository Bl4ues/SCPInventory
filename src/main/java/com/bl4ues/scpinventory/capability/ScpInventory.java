package com.bl4ues.scpinventory.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ScpInventory implements IScpInventory {

    private static final int MAX_SLOTS = MAIN_SLOT_COUNT;

    private final List<ItemStack> inventory = new ArrayList<>();
    private final List<ItemStack> keys = new ArrayList<>();
    private final List<ItemStack> documents = new ArrayList<>();

    private ItemStack head = ItemStack.EMPTY;
    private ItemStack chest = ItemStack.EMPTY;
    private ItemStack legs = ItemStack.EMPTY;
    private ItemStack feet = ItemStack.EMPTY;

    public ScpInventory() {
        for (int i = 0; i < MAX_SLOTS; i++) {
            inventory.add(ItemStack.EMPTY);
        }
    }

    @Override
    public List<ItemStack> getInventory() { return inventory; }

    @Override
    public void setInventory(List<ItemStack> list) {
        inventory.clear();
        for (int i = 0; i < MAX_SLOTS; i++) {
            if (list != null && i < list.size()) inventory.add(toSingleItemOrEmpty(list.get(i)));
            else inventory.add(ItemStack.EMPTY);
        }
    }

    @Override
    public ItemStack getInventoryItem(int index) {
        if (index < 0 || index >= MAX_SLOTS) return ItemStack.EMPTY;
        return inventory.get(index);
    }

    @Override
    public boolean setInventoryItem(int index, ItemStack stack) {
        if (index < 0 || index >= MAX_SLOTS) return false;
        inventory.set(index, toSingleItemOrEmpty(stack));
        return true;
    }

    @Override
    public boolean addInventoryItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;

        int amount = stack.getCount();
        if (!hasFreeMainSlots(amount)) return false;

        for (int amountAdded = 0; amountAdded < amount; amountAdded++) {
            int emptySlot = firstEmptyMainSlot();
            if (emptySlot == -1) return false;

            ItemStack singleItem = stack.copy();
            singleItem.setCount(1);
            inventory.set(emptySlot, singleItem);
        }

        return true;
    }

    @Override
    public boolean isInventoryFull() {
        for (ItemStack stack : inventory) if (stack.isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack extractInventoryItem(int index) {
        if (index < 0 || index >= MAX_SLOTS) return ItemStack.EMPTY;
        ItemStack stack = inventory.get(index);
        inventory.set(index, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public boolean removeInventoryItem(int index) {
        if (index < 0 || index >= MAX_SLOTS) return false;
        inventory.set(index, ItemStack.EMPTY);
        return true;
    }

    @Override
    public List<ItemStack> getKeys() { return keys; }

    @Override
    public void setKeys(List<ItemStack> list) {
        keys.clear();
        addAllStacks(keys, list);
    }

    @Override
    public boolean addKeyItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        keys.add(stack.copy());
        return true;
    }

    @Override
    public ItemStack extractKeyItem(int index) {
        if (index < 0 || index >= keys.size()) return ItemStack.EMPTY;
        return keys.remove(index);
    }

    @Override
    public boolean removeKeyItem(int index) {
        if (index < 0 || index >= keys.size()) return false;
        keys.remove(index);
        return true;
    }

    @Override
    public List<ItemStack> getDocuments() { return documents; }

    @Override
    public void setDocuments(List<ItemStack> list) {
        documents.clear();
        addAllStacks(documents, list);
    }

    @Override
    public boolean addDocumentItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        documents.add(stack.copy());
        return true;
    }

    @Override
    public ItemStack getDocumentItem(int index) {
        if (index < 0 || index >= documents.size()) return ItemStack.EMPTY;
        return documents.get(index);
    }

    @Override
    public ItemStack extractDocumentItem(int index) {
        if (index < 0 || index >= documents.size()) return ItemStack.EMPTY;
        return documents.remove(index);
    }

    @Override
    public boolean removeDocumentItem(int index) {
        if (index < 0 || index >= documents.size()) return false;
        documents.remove(index);
        return true;
    }

    @Override public ItemStack getHead() { return head; }
    @Override public void setHead(ItemStack stack) { head = copyOrEmpty(stack); }
    @Override public ItemStack getChest() { return chest; }
    @Override public void setChest(ItemStack stack) { chest = copyOrEmpty(stack); }
    @Override public ItemStack getLegs() { return legs; }
    @Override public void setLegs(ItemStack stack) { legs = copyOrEmpty(stack); }
    @Override public ItemStack getFeet() { return feet; }
    @Override public void setFeet(ItemStack stack) { feet = copyOrEmpty(stack); }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();

        tag.put("Inventory", saveStackList(inventory, true));
        tag.put("Keys", saveStackList(keys, false));
        tag.put("Documents", saveStackList(documents, false));

        CompoundTag equipTag = new CompoundTag();
        saveEquipment(equipTag, "Head", head);
        saveEquipment(equipTag, "Chest", chest);
        saveEquipment(equipTag, "Legs", legs);
        saveEquipment(equipTag, "Feet", feet);
        tag.put("Equipment", equipTag);

        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        inventory.clear();
        ListTag invList = tag.getList("Inventory", 10);
        for (int i = 0; i < MAX_SLOTS; i++) {
            if (i < invList.size()) inventory.add(toSingleItemOrEmpty(ItemStack.of(invList.getCompound(i))));
            else inventory.add(ItemStack.EMPTY);
        }

        keys.clear();
        loadStackList(keys, tag.getList("Keys", 10));

        documents.clear();
        loadStackList(documents, tag.getList("Documents", 10));

        CompoundTag equipTag = tag.getCompound("Equipment");
        head = equipTag.contains("Head") ? ItemStack.of(equipTag.getCompound("Head")) : ItemStack.EMPTY;
        chest = equipTag.contains("Chest") ? ItemStack.of(equipTag.getCompound("Chest")) : ItemStack.EMPTY;
        legs = equipTag.contains("Legs") ? ItemStack.of(equipTag.getCompound("Legs")) : ItemStack.EMPTY;
        feet = equipTag.contains("Feet") ? ItemStack.of(equipTag.getCompound("Feet")) : ItemStack.EMPTY;
    }

    private int firstEmptyMainSlot() {
        for (int i = 0; i < inventory.size(); i++) {
            if (inventory.get(i).isEmpty()) return i;
        }
        return -1;
    }

    private static ItemStack copyOrEmpty(ItemStack stack) {
        return stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
    }

    private static ItemStack toSingleItemOrEmpty(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy;
    }

    private static void addAllStacks(List<ItemStack> target, List<ItemStack> source) {
        if (source == null) return;
        for (ItemStack stack : source) {
            if (stack != null && !stack.isEmpty()) target.add(stack.copy());
        }
    }

    private static ListTag saveStackList(List<ItemStack> stacks, boolean keepEmptySlots) {
        ListTag list = new ListTag();
        for (ItemStack stack : stacks) {
            if (stack.isEmpty() && !keepEmptySlots) continue;
            CompoundTag stackTag = new CompoundTag();
            if (!stack.isEmpty()) stack.save(stackTag);
            list.add(stackTag);
        }
        return list;
    }

    private static void loadStackList(List<ItemStack> target, ListTag list) {
        for (int i = 0; i < list.size(); i++) {
            ItemStack stack = ItemStack.of(list.getCompound(i));
            if (!stack.isEmpty()) target.add(stack);
        }
    }

    private static void saveEquipment(CompoundTag parent, String key, ItemStack stack) {
        if (!stack.isEmpty()) {
            CompoundTag stackTag = new CompoundTag();
            stack.save(stackTag);
            parent.put(key, stackTag);
        }
    }
}
