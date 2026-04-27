package com.furtabs.lootbags.datagen

import net.minecraft.data.PackOutput
import net.minecraft.core.HolderLookup
import net.neoforged.neoforge.common.data.GlobalLootModifierProvider
import com.furtabs.lootbags.LootBags
import java.util.concurrent.CompletableFuture

class ModGlobalLootModifierProvider(
    output: PackOutput,
    registries: CompletableFuture<HolderLookup.Provider>
) : GlobalLootModifierProvider(output, registries, LootBags.MOD_ID) {
    override fun start() {}
}
