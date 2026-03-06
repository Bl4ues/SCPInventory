package com.bl4ues.scpinventory.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.List;

public interface IScpInventory {

    // ================= INVENTÁRIO PRINCIPAL =================
    List<ItemStack> getInventory();
    void setInventory(List<ItemStack> list);
    boolean isInventoryFull();
    boolean addInventoryItem(ItemStack stack);
    ItemStack extractInventoryItem(int index);
    boolean removeInventoryItem(int index);

    // ================= KEYS =================
    List<ItemStack> getKeys();
    void setKeys(List<ItemStack> list);
    boolean addKeyItem(ItemStack stack);
    ItemStack extractKeyItem(int index);
    boolean removeKeyItem(int index);

    // ================= EQUIPAMENTOS =================
    ItemStack getHead();
    void setHead(ItemStack stack);
    ItemStack getChest();
    void setChest(ItemStack stack);
    ItemStack getLegs();
    void setLegs(ItemStack stack);
    ItemStack getFeet();
    void setFeet(ItemStack stack);

    // ================= CODEX =================
    List<String> getUnlockedDocuments();
    void unlockDocument(String id);
    boolean hasDocument(String id);

    // ================= SERIALIZAÇÃO =================
    CompoundTag serializeNBT();
    void deserializeNBT(CompoundTag tag);

    // ================= CONTAGEM DE ITENS =================
    default int getInventoryCount() {
        int count = 0;
        for (ItemStack stack : getInventory()) {
            if (!stack.isEmpty()) count++;
        }
        return count;
    }

    // ================= MÉTODOS PARA UI =================

    /** Retorna o tipo de item em um slot: Consumable, Equip ou Misc */
    default String getItemType(int slot) {
        ItemStack stack = getInventory().get(slot);
        if (stack.isEmpty()) return "Empty";

        // Consumable
        if (stack.isEdible()) return "Consumable";

        // Equipamentos por nome do item
        String name = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        if (name.contains("helmet")) return "Head";
        if (name.contains("chestplate")) return "Body";
        if (name.contains("leggings")) return "Legs";
        if (name.contains("boots")) return "Feet";

        // Outros
        return "Miscellaneous";
    }

    /** Dropa o item do slot para o mundo */
    default void dropItem(int slot, Level world, double x, double y, double z) {
        ItemStack stack = extractInventoryItem(slot);
        if (!stack.isEmpty()) {
            ItemEntity drop = new ItemEntity(world, x, y, z, stack);
            world.addFreshEntity(drop);
        }
    }
}