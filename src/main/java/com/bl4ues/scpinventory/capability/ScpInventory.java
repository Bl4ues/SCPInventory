package com.bl4ues.scpinventory.capability;

import com.bl4ues.scpinventory.item.ScpEquipmentSlot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ScpInventory implements IScpInventory {

    private final List<ItemStack> inventory = new ArrayList<>();
    private final List<ItemStack> keys = new ArrayList<>();
    private final List<ItemStack> documents = new ArrayList<>();
    private final Map<ScpEquipmentSlot, ItemStack> equipment = new EnumMap<>(ScpEquipmentSlot.class);

    private int maxMainSlots = DEFAULT_MAIN_SLOT_COUNT;

    public ScpInventory() {
        resetEquipmentSlots();
        normalizeMainInventorySize();
    }

    @Override
    public List<ItemStack> getInventory() { return inventory; }

    @Override
    public void setInventory(List<ItemStack> list) {
        inventory.clear();
        for (int i = 0; i < maxMainSlots; i++) {
            if (list != null && i < list.size()) inventory.add(toSingleItemOrEmpty(list.get(i)));
            else inventory.add(ItemStack.EMPTY);
        }
        normalizeMainInventorySize();
    }

    @Override
    public ItemStack getInventoryItem(int index) {
        if (index < 0 || index >= maxMainSlots) return ItemStack.EMPTY;
        normalizeMainInventorySize();
        return inventory.get(index);
    }

    @Override
    public boolean setInventoryItem(int index, ItemStack stack) {
        if (index < 0 || index >= maxMainSlots) return false;
        normalizeMainInventorySize();
        inventory.set(index, toSingleItemOrEmpty(stack));
        return true;
    }

    @Override
    public boolean addInventoryItem(ItemStack stack) {
        return addInventoryItems(stack) == getStackCount(stack);
    }

    @Override
    public int addInventoryItems(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;

        normalizeMainInventorySize();

        int amountToInsert = Math.min(stack.getCount(), getFreeMainSlots());
        int inserted = 0;

        for (int i = 0; i < amountToInsert; i++) {
            int emptySlot = firstEmptyMainSlot();
            if (emptySlot == -1) break;

            ItemStack singleItem = stack.copy();
            singleItem.setCount(1);
            inventory.set(emptySlot, singleItem);
            inserted++;
        }

        return inserted;
    }

    @Override
    public boolean isInventoryFull() {
        normalizeMainInventorySize();
        for (ItemStack stack : inventory) if (stack.isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack extractInventoryItem(int index) {
        if (index < 0 || index >= maxMainSlots) return ItemStack.EMPTY;
        normalizeMainInventorySize();
        ItemStack stack = inventory.get(index);
        inventory.set(index, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public boolean removeInventoryItem(int index) {
        if (index < 0 || index >= maxMainSlots) return false;
        normalizeMainInventorySize();
        inventory.set(index, ItemStack.EMPTY);
        return true;
    }

    @Override
    public int getMaxMainSlots() {
        return maxMainSlots;
    }

    @Override
    public void setMaxMainSlots(int slots) {
        maxMainSlots = clampSlots(slots);
        normalizeMainInventorySize();
    }

    @Override
    public void resetMainInventory() {
        inventory.clear();
        normalizeMainInventorySize();
    }

    @Override
    public void resetAll() {
        maxMainSlots = DEFAULT_MAIN_SLOT_COUNT;
        resetMainInventory();
        keys.clear();
        documents.clear();
        resetEquipmentSlots();
    }

    @Override
    public List<ItemStack> getKeys() { return keys; }

    @Override
    public void setKeys(List<ItemStack> list) {
        keys.clear();
        if (list == null) return;

        for (ItemStack stack : list) {
            if (getKeyCount() >= MAX_KEY_COUNT) break;
            if (stack != null && !stack.isEmpty()) keys.add(toSingleItemOrEmpty(stack));
        }
    }

    @Override
    public boolean addKeyItem(ItemStack stack) {
        if (stack == null || stack.isEmpty() || getKeyCount() >= MAX_KEY_COUNT) return false;
        keys.add(toSingleItemOrEmpty(stack));
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

    @Override
    public ItemStack getEquipment(ScpEquipmentSlot slot) {
        if (slot == null) return ItemStack.EMPTY;
        return equipment.getOrDefault(slot, ItemStack.EMPTY);
    }

    @Override
    public void setEquipment(ScpEquipmentSlot slot, ItemStack stack) {
        if (slot == null) return;
        equipment.put(slot, copyOrEmpty(stack));
    }

    @Override
    public ItemStack extractEquipment(ScpEquipmentSlot slot) {
        if (slot == null) return ItemStack.EMPTY;
        ItemStack stack = equipment.getOrDefault(slot, ItemStack.EMPTY);
        equipment.put(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public boolean clearEquipment(ScpEquipmentSlot slot) {
        if (slot == null) return false;
        equipment.put(slot, ItemStack.EMPTY);
        return true;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();

        tag.putInt("MaxMainSlots", maxMainSlots);
        tag.put("Inventory", saveStackList(inventory, true));
        tag.put("Keys", saveStackList(keys, false));
        tag.put("Documents", saveStackList(documents, false));

        CompoundTag equipTag = new CompoundTag();
        for (ScpEquipmentSlot slot : ScpEquipmentSlot.values()) {
            saveEquipment(equipTag, slot.getTagName(), getEquipment(slot));
        }
        tag.put("Equipment", equipTag);

        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        maxMainSlots = tag.contains("MaxMainSlots")
                ? clampSlots(tag.getInt("MaxMainSlots"))
                : DEFAULT_MAIN_SLOT_COUNT;

        inventory.clear();
        ListTag invList = tag.getList("Inventory", 10);
        for (int i = 0; i < maxMainSlots; i++) {
            if (i < invList.size()) inventory.add(toSingleItemOrEmpty(ItemStack.of(invList.getCompound(i))));
            else inventory.add(ItemStack.EMPTY);
        }
        normalizeMainInventorySize();

        keys.clear();
        loadKeyList(keys, tag.getList("Keys", 10));

        documents.clear();
        loadStackList(documents, tag.getList("Documents", 10));

        resetEquipmentSlots();
        CompoundTag equipTag = tag.getCompound("Equipment");
        for (ScpEquipmentSlot slot : ScpEquipmentSlot.values()) {
            equipment.put(slot, loadEquipment(equipTag, slot.getTagName()));
        }

        migrateLegacyEquipment(equipTag);
    }

    private int firstEmptyMainSlot() {
        normalizeMainInventorySize();
        for (int i = 0; i < inventory.size(); i++) {
            if (inventory.get(i).isEmpty()) return i;
        }
        return -1;
    }

    private void normalizeMainInventorySize() {
        while (inventory.size() < maxMainSlots) {
            inventory.add(ItemStack.EMPTY);
        }

        while (inventory.size() > maxMainSlots) {
            inventory.remove(inventory.size() - 1);
        }
    }

    private void resetEquipmentSlots() {
        equipment.clear();
        for (ScpEquipmentSlot slot : ScpEquipmentSlot.values()) {
            equipment.put(slot, ItemStack.EMPTY);
        }
    }

    private void migrateLegacyEquipment(CompoundTag equipTag) {
        if (getEquipment(ScpEquipmentSlot.HEAD).isEmpty()) {
            equipment.put(ScpEquipmentSlot.HEAD, loadEquipment(equipTag, "Head"));
        }

        if (getEquipment(ScpEquipmentSlot.BODY).isEmpty()) {
            equipment.put(ScpEquipmentSlot.BODY, loadEquipment(equipTag, "Chest"));
        }

        if (getEquipment(ScpEquipmentSlot.ACCESSORY).isEmpty()) {
            ItemStack legacyAccessory = loadEquipment(equipTag, "Accessory");
            if (legacyAccessory.isEmpty()) {
                legacyAccessory = loadEquipment(equipTag, "Trinket");
            }
            equipment.put(ScpEquipmentSlot.ACCESSORY, legacyAccessory);
        }

        if (getEquipment(ScpEquipmentSlot.WEAPON).isEmpty()) {
            equipment.put(ScpEquipmentSlot.WEAPON, loadEquipment(equipTag, "Weapon"));
        }
    }

    private static int clampSlots(int slots) {
        return Math.max(MIN_MAIN_SLOT_COUNT, Math.min(MAX_MAIN_SLOT_COUNT, slots));
    }

    private static int getStackCount(ItemStack stack) {
        return stack == null || stack.isEmpty() ? 0 : stack.getCount();
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

    private static void loadKeyList(List<ItemStack> target, ListTag list) {
        for (int i = 0; i < list.size() && target.size() < MAX_KEY_COUNT; i++) {
            ItemStack stack = toSingleItemOrEmpty(ItemStack.of(list.getCompound(i)));
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

    private static ItemStack loadEquipment(CompoundTag parent, String key) {
        return parent.contains(key) ? ItemStack.of(parent.getCompound(key)) : ItemStack.EMPTY;
    }
}
