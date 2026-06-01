package com.furtabs.lootbags.util

import net.minecraft.core.HolderLookup
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData

private const val KEY_LOOT_SLOTS: String = "LootBagOpenItems"

/**
 * Returns up to [MAX_LOOT_BAG_ITEM_STACKS] stacks read from the bag's NBT, or null if nothing stored.
 */
@Suppress("DEPRECATION")
fun writeStoredOpenLoot(stack: ItemStack, loots: List<ItemStack>, registryAccess: HolderLookup.Provider? = null) {
    val registries = registryAccess ?: return
    val customData = stack.get(DataComponents.CUSTOM_DATA)
    val tag = customData?.copyTag() ?: CompoundTag()
    val listTag = ListTag()
    for (loot in loots) {
        if (!loot.isEmpty) {
            val lootTag = loot.save(registries)
            listTag.add(lootTag)
        }
    }
    tag.put("Items", listTag)
    tag.putBoolean("opened", true)
    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag))
}

@Suppress("DEPRECATION")
fun readStoredOpenLoot(stack: ItemStack, registryAccess: HolderLookup.Provider? = null): List<ItemStack>? {
    val registries = registryAccess ?: return null
    val customData = stack.get(DataComponents.CUSTOM_DATA) ?: return null
    val tag = customData.copyTag()
    if (!tag.contains("Items", Tag.TAG_LIST.toInt())) return null

    val listTag = tag.getList("Items", Tag.TAG_COMPOUND.toInt())
    val loots = mutableListOf<ItemStack>()
    for (i in 0 until listTag.size) {
        val parsed = ItemStack.parse(registries, listTag.getCompound(i)).orElse(ItemStack.EMPTY)
        if (!parsed.isEmpty) {
            loots.add(parsed)
        }
    }
    return if (loots.isEmpty()) null else loots
}

@Suppress("DEPRECATION")
fun clearStoredOpenLoot(bagStack: ItemStack) {
    val customData = bagStack.get(DataComponents.CUSTOM_DATA) ?: return
    val root = customData.copyTag()
    // Remove keys used by writeStoredOpenLoot
    root.remove("Items")
    root.remove("opened")
    // Also remove any legacy key
    root.remove(KEY_LOOT_SLOTS)
    if (root.isEmpty) {
        bagStack.remove(DataComponents.CUSTOM_DATA)
    } else {
        bagStack.set(DataComponents.CUSTOM_DATA, CustomData.of(root))
    }
}
