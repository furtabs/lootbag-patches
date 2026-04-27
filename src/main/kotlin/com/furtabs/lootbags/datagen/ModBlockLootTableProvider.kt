package com.furtabs.lootbags.datagen

import net.minecraft.data.loot.BlockLootSubProvider
import net.minecraft.world.flag.FeatureFlags
import net.minecraft.world.level.block.Block
import com.furtabs.lootbags.LootBags
import com.furtabs.lootbags.block.ModBlocks
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.HolderLookup
import net.minecraft.world.item.Item

class ModBlockLootTableProvider(
    registries: HolderLookup.Provider
) : BlockLootSubProvider(setOf<Item>(), FeatureFlags.REGISTRY.allFlags(), registries) {
    override fun generate() {
        dropSelf(ModBlocks.LOOT_RECYCLER.get())
        dropSelf(ModBlocks.BAG_OPENER.get())
        dropSelf(ModBlocks.BAG_STORAGE.get())
    }

    override fun getKnownBlocks(): Iterable<Block> =
        BuiltInRegistries.BLOCK
            .filter { block -> BuiltInRegistries.BLOCK.getKey(block).namespace == LootBags.MOD_ID }
}
