package com.furtabs.lootbags.util

import net.minecraft.world.item.Item
import net.minecraft.world.level.ItemLike
import com.furtabs.lootbags.item.ModItems
import com.furtabs.lootbags.item.custom.LootBagItem
import kotlin.math.pow

enum class LootBagType(
    val itemId: String,
    val rarity: UInt, // This acts as our "Tier"
    val droppable: Boolean,
    val creativeOnly: Boolean,
    val dropChance: Double
) : ItemLike {
    COMMON("common_loot_bag", 0U, true, false, 0.4),
    UNCOMMON("uncommon_loot_bag", 1U, true, false, 0.1),
    RARE("rare_loot_bag", 2U, true, false, 0.025),
    EPIC("epic_loot_bag", 3U, true, false, 0.00625),
    LEGENDARY("legendary_loot_bag", 4U, true, false, 0.0015625);

    companion object {
        const val AMOUNT_TO_NEXT_RARITY = 4
        
        // Helper for the extension function to avoid repetitive 'when' blocks
        private val ALL_BAGS by lazy { entries }
    }

    // Pass 'rarity.toInt()' as the second argument required by your LootGenerator
    // Use 'lazy' so it doesn't run until the first time a bag is actually opened
    val lootGenerator: LootGenerator by lazy { LootGenerator(this, this.rarity.toInt()) }

    override fun asItem(): Item = when (this) {
        COMMON -> ModItems.COMMON_LOOT_BAG.get()
        UNCOMMON -> ModItems.UNCOMMON_LOOT_BAG.get()
        RARE -> ModItems.RARE_LOOT_BAG.get()
        EPIC -> ModItems.EPIC_LOOT_BAG.get()
        LEGENDARY -> ModItems.LEGENDARY_LOOT_BAG.get()
    }

    fun amountFactorEquivalentTo(other: LootBagType): Float =
        AMOUNT_TO_NEXT_RARITY.toFloat().pow(this.rarity.toInt() - other.rarity.toInt())
}

/**
 * Safely converts a LootBagItem instance to its corresponding Enum type.
 */
fun LootBagItem.asLootBagType(): LootBagType {
    return LootBagType.entries.find { it.asItem() == this }
        ?: throw IllegalArgumentException("Item $this is not a valid LootBagType!")
}