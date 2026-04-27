package com.furtabs.lootbags.jei

import com.furtabs.lootbags.util.LootBagType
import net.minecraft.world.item.ItemStack

class LootBagRecipe(
    val bagType: LootBagType,
    val bag: ItemStack,
    val outputs: List<ItemStack>,
    val pageIndex: Int,
    val totalPages: Int
)
