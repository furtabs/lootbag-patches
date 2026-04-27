package com.furtabs.lootbags.util

import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.items.ItemStackHandler

fun writeStoredOpenLoot(stack: ItemStack, loots: List<ItemStack>) {
    // TODO 1.21.1: replace with data-component aware persistence.
}

fun readStoredOpenLoot(stack: ItemStack): List<ItemStack>? {
    return null
}

fun clearStoredOpenLoot(bagStack: ItemStack) {
    // TODO 1.21.1: replace with data-component aware persistence.
}
