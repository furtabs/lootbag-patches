package com.furtabs.lootbags.util

import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.world.item.ItemStack
import net.minecraftforge.items.ItemStackHandler

private const val KEY_LOOT_SLOTS: String = "LootBagOpenItems"

/**
 * Returns up to [MAX_LOOT_BAG_ITEM_STACKS] stacks read from the bag's NBT, or null if nothing stored.
 */
@Suppress("DEPRECATION")
fun readStoredOpenLoot(bagStack: ItemStack): List<ItemStack>? {
    val root = bagStack.tag ?: return null
    if (!root.contains(KEY_LOOT_SLOTS, Tag.TAG_LIST.toInt())) {
        return null
    }
    val list = root.getList(KEY_LOOT_SLOTS, Tag.TAG_COMPOUND.toInt())
    if (list.isEmpty()) {
        return null
    }
    val out = ArrayList<ItemStack>(MAX_LOOT_BAG_ITEM_STACKS)
    for (i in 0..<MAX_LOOT_BAG_ITEM_STACKS) {
        val compound = if (i < list.size) list.getCompound(i) else CompoundTag()
        val parsed = if (compound.isEmpty) ItemStack.EMPTY else ItemStack.of(compound)
        out.add(parsed)
    }
    return if (out.any { !it.isEmpty }) out else null
}

@Suppress("DEPRECATION")
fun writeStoredOpenLoot(bagStack: ItemStack, handler: ItemStackHandler) {
    val root = bagStack.orCreateTag
    val list = ListTag()
    for (i in 0..<MAX_LOOT_BAG_ITEM_STACKS) {
        val slot = handler.getStackInSlot(i)
        val nbt = CompoundTag()
        if (!slot.isEmpty) {
            slot.save(nbt)
        }
        list.add(nbt)
    }
    root.put(KEY_LOOT_SLOTS, list)
}

@Suppress("DEPRECATION")
fun clearStoredOpenLoot(bagStack: ItemStack) {
    val root = bagStack.tag ?: return
    root.remove(KEY_LOOT_SLOTS)
    if (root.isEmpty) {
        bagStack.tag = null
    }
}
