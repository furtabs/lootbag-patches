package com.furtabs.lootbags.util

import net.minecraft.core.HolderLookup
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData

fun writeStoredOpenLoot(stack: ItemStack, loots: List<ItemStack>, registries: HolderLookup.Provider) {
    val listTag = ListTag()
    for (loot in loots) {
        // In 1.21.1, saveOptional safely serializes the item and its data components
        val itemTag = loot.saveOptional(registries) as? CompoundTag ?: continue
        listTag.add(itemTag)
    }

    // Fetch existing custom data (or create empty), then update the tag
    val customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
    val tag = customData.copyTag()
    tag.put("StoredLoot", listTag)
    
    // Set the updated CustomData back onto the stack
    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag))
}

fun readStoredOpenLoot(stack: ItemStack, registries: HolderLookup.Provider): List<ItemStack>? {
    val customData = stack.get(DataComponents.CUSTOM_DATA) ?: return null
    val tag = customData.copyTag()
    
    if (!tag.contains("StoredLoot", Tag.TAG_LIST.toInt())) return null

    val listTag = tag.getList("StoredLoot", Tag.TAG_COMPOUND.toInt())
    val loots = mutableListOf<ItemStack>()
    
    for (i in 0 until listTag.size) {
        val itemTag = listTag.getCompound(i)
        // parseOptional reads the NBT back into an ItemStack, returning ItemStack.EMPTY if invalid
        val parsedStack = ItemStack.parseOptional(registries, itemTag)
        if (!parsedStack.isEmpty) {
            loots.add(parsedStack)
        }
    }
    
    return loots
}

fun clearStoredOpenLoot(stack: ItemStack) {
    val customData = stack.get(DataComponents.CUSTOM_DATA) ?: return
    val tag = customData.copyTag()
    
    if (tag.contains("StoredLoot")) {
        tag.remove("StoredLoot")
        if (tag.isEmpty) {
            // Clean up: If the custom data is now completely empty, strip the component entirely
            stack.remove(DataComponents.CUSTOM_DATA)
        } else {
            // Otherwise, save the remaining custom data back
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag))
        }
    }
}