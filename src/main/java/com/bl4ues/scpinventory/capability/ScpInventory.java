package com.bl4ues.scpinventory.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ScpInventory implements IScpInventory {

    private static final int MAX_SLOTS = 12;

    private final List<ItemStack> inventory = new ArrayList<>();
    private final List<ItemStack> keys = new ArrayList<>();
    private final List<String> unlockedDocuments = new ArrayList<>();

    private ItemStack head = ItemStack.EMPTY;
    private ItemStack chest = ItemStack.EMPTY;
    private ItemStack legs = ItemStack.EMPTY;
    private ItemStack feet = ItemStack.EMPTY;

    public ScpInventory() {
        for (int i = 0; i < MAX_SLOTS; i++) {
            inventory.add(ItemStack.EMPTY);
        }
    }

    // ================= INVENTÁRIO =================
    @Override
    public List<ItemStack> getInventory() { return inventory; }

    @Override
    public void setInventory(List<ItemStack> list) {
        inventory.clear();
        for (int i = 0; i < MAX_SLOTS; i++) {
            if (i < list.size()) inventory.add(list.get(i));
            else inventory.add(ItemStack.EMPTY);
        }
    }

    @Override
    public boolean addInventoryItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        for (int i = 0; i < inventory.size(); i++) {
            if (inventory.get(i).isEmpty()) {
                inventory.set(i, stack.copy());
                return true;
            }
        }
        return false;
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

    // ================= KEYS =================
    @Override
    public List<ItemStack> getKeys() { return keys; }

    @Override
    public void setKeys(List<ItemStack> list) {
        keys.clear();
        keys.addAll(list);
    }

    @Override
    public boolean addKeyItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        keys.add(stack.copy());
        return true;
    }

    @Override
    public ItemStack extractKeyItem(int index) {
        if (index < 0 || index >= keys.size()) return ItemStack.EMPTY;
        ItemStack stack = keys.get(index);
        keys.remove(index);
        return stack;
    }

    @Override
    public boolean removeKeyItem(int index) {
        if (index < 0 || index >= keys.size()) return false;
        keys.remove(index);
        return true;
    }

    // ================= EQUIPAMENTOS =================
    @Override public ItemStack getHead() { return head; }
    @Override public void setHead(ItemStack stack) { head = stack; }
    @Override public ItemStack getChest() { return chest; }
    @Override public void setChest(ItemStack stack) { chest = stack; }
    @Override public ItemStack getLegs() { return legs; }
    @Override public void setLegs(ItemStack stack) { legs = stack; }
    @Override public ItemStack getFeet() { return feet; }
    @Override public void setFeet(ItemStack stack) { feet = stack; }

    // ================= CODEX =================
    @Override public List<String> getUnlockedDocuments() { return unlockedDocuments; }
    @Override public void unlockDocument(String id) {
        if (!unlockedDocuments.contains(id)) unlockedDocuments.add(id);
    }
    @Override public boolean hasDocument(String id) { return unlockedDocuments.contains(id); }

    // ================= SERIALIZAÇÃO =================
    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();

        // Inventário
        ListTag invList = new ListTag();
        for (ItemStack stack : inventory) {
            CompoundTag stackTag = new CompoundTag();
            if (!stack.isEmpty()) stack.save(stackTag);
            invList.add(stackTag);
        }
        tag.put("Inventory", invList);

        // Keys
        ListTag keyList = new ListTag();
        for (ItemStack stack : keys) {
            CompoundTag stackTag = new CompoundTag();
            if (!stack.isEmpty()) stack.save(stackTag);
            keyList.add(stackTag);
        }
        tag.put("Keys", keyList);

        // Equipamentos
        CompoundTag equipTag = new CompoundTag();
        if (!head.isEmpty()) {
            CompoundTag headTag = new CompoundTag();
            head.save(headTag);
            equipTag.put("Head", headTag);
        }
        if (!chest.isEmpty()) {
            CompoundTag chestTag = new CompoundTag();
            chest.save(chestTag);
            equipTag.put("Chest", chestTag);
        }
        if (!legs.isEmpty()) {
            CompoundTag legsTag = new CompoundTag();
            legs.save(legsTag);
            equipTag.put("Legs", legsTag);
        }
        if (!feet.isEmpty()) {
            CompoundTag feetTag = new CompoundTag();
            feet.save(feetTag);
            equipTag.put("Feet", feetTag);
        }
        tag.put("Equipment", equipTag);

        // Codex
        ListTag docList = new ListTag();
        for (String doc : unlockedDocuments) {
            docList.add(StringTag.valueOf(doc));
        }
        tag.put("Documents", docList);

        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        // Inventário
        inventory.clear();
        ListTag invList = tag.getList("Inventory", 10);
        for (int i = 0; i < MAX_SLOTS; i++) {
            if (i < invList.size()) inventory.add(ItemStack.of(invList.getCompound(i)));
            else inventory.add(ItemStack.EMPTY);
        }

        // Keys
        keys.clear();
        ListTag keyList = tag.getList("Keys", 10);
        for (int i = 0; i < keyList.size(); i++) keys.add(ItemStack.of(keyList.getCompound(i)));

        // Equipamentos
        CompoundTag equipTag = tag.getCompound("Equipment");
        head = equipTag.contains("Head") ? ItemStack.of(equipTag.getCompound("Head")) : ItemStack.EMPTY;
        chest = equipTag.contains("Chest") ? ItemStack.of(equipTag.getCompound("Chest")) : ItemStack.EMPTY;
        legs = equipTag.contains("Legs") ? ItemStack.of(equipTag.getCompound("Legs")) : ItemStack.EMPTY;
        feet = equipTag.contains("Feet") ? ItemStack.of(equipTag.getCompound("Feet")) : ItemStack.EMPTY;

        // Codex
        unlockedDocuments.clear();
        ListTag docList = tag.getList("Documents", 8);
        for (int i = 0; i < docList.size(); i++) unlockedDocuments.add(docList.getString(i));
    }
}