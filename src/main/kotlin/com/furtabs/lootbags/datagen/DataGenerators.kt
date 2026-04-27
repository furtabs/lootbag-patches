package com.furtabs.lootbags.datagen

import net.minecraft.data.loot.LootTableProvider
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets
import net.neoforged.neoforge.data.event.GatherDataEvent
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import com.furtabs.lootbags.LootBags

@EventBusSubscriber(modid = LootBags.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
object DataGenerators {
    @SubscribeEvent
    fun gatherData(event: GatherDataEvent) {
        val generator = event.generator
        val packOutput = generator.packOutput
        val lookup = event.lookupProvider
        val existing = event.existingFileHelper
        if (event.includeServer()) {
            generator.addProvider(
                true,
                LootTableProvider(
                    packOutput,
                    setOf(),
                    listOf(
                        LootTableProvider.SubProviderEntry(
                            { registries -> ModBlockLootTableProvider(registries) },
                            LootContextParamSets.BLOCK
                        )
                    ),
                    lookup
                )
            )
            generator.addProvider(true, ModRecipeProvider(packOutput, lookup))
            val blockTags = ModBlockTagsProvider(packOutput, lookup, existing)
            generator.addProvider(true, blockTags)
            generator.addProvider(
                true,
                ModItemTagsProvider(packOutput, lookup, blockTags.contentsGetter(), existing)
            )
            generator.addProvider(true, ModGlobalLootModifierProvider(packOutput, lookup))
        }
    }
}
