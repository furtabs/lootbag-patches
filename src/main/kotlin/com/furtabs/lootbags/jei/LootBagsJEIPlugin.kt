package com.furtabs.lootbags.jei

import com.furtabs.lootbags.util.LootBagType
import mezz.jei.api.IModPlugin
import mezz.jei.api.JeiPlugin
import mezz.jei.api.recipe.RecipeType
import mezz.jei.api.registration.IRecipeCategoryRegistration
import mezz.jei.api.registration.IRecipeRegistration
import net.minecraft.client.Minecraft
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.storage.loot.LootParams
import net.minecraft.world.level.storage.loot.parameters.LootContextParams
import net.minecraft.world.phys.Vec3
import java.util.concurrent.ConcurrentHashMap

@JeiPlugin
class LootBagsJEIPlugin : IModPlugin {
    
    companion object {
        val PLUGIN_UID: ResourceLocation = ResourceLocation.fromNamespaceAndPath("lootbags", "jei_plugin")
        
        // Map to store a unique RecipeType for every LootBagType
        private val RECIPE_TYPES = mutableMapOf<LootBagType, RecipeType<LootBagRecipe>>()
        
        private val runtimeOutputs = ConcurrentHashMap<LootBagType, List<ItemStack>>()
        private val pendingBags = ConcurrentHashMap.newKeySet<LootBagType>()
        
        @Volatile
        private var preloadStarted = false
        @Volatile
        private var loadedWorldKey: String? = null

        fun onClientTickPreload() {
            val serverLevel = resolveServerLevel() ?: return
            val worldKey = serverLevel.dimension().location().toString()
            if (loadedWorldKey != worldKey) {
                loadedWorldKey = worldKey
                preloadStarted = false
                runtimeOutputs.clear()
                pendingBags.clear()
            }
            if (!preloadStarted) {
                preloadStarted = true
                LootBagType.entries.forEach { queueGenerationIfNeeded(it) }
            }
        }

        fun getOutputsForBagType(bagType: LootBagType): List<ItemStack> {
            val cached = runtimeOutputs[bagType]
            if (cached != null && cached.isNotEmpty()) return cached
            queueGenerationIfNeeded(bagType)
            return emptyList()
        }

        private fun queueGenerationIfNeeded(bagType: LootBagType) {
            if (!pendingBags.add(bagType)) return
            val serverLevel = resolveServerLevel()
            if (serverLevel == null) {
                pendingBags.remove(bagType)
                return
            }

            serverLevel.server.execute {
                val generated = generatePotentialLootNow(bagType, serverLevel)
                if (generated.isNotEmpty()) {
                    runtimeOutputs[bagType] = generated
                }
                pendingBags.remove(bagType)
            }
        }

        private fun generatePotentialLootNow(bagType: LootBagType, serverLevel: ServerLevel): List<ItemStack> {
            return try {
                val seen = linkedMapOf<String, ItemStack>()
                repeat(4000) {
                    bagType.lootGenerator.generateLoot(
                        serverLevel,
                        LootParams.Builder(serverLevel).withParameter(LootContextParams.ORIGIN, Vec3.ZERO)
                    ).forEach { stack ->
                        if (stack.isEmpty) return@forEach
                        val key = stack.item.toString()
                        seen.putIfAbsent(key, stack.copyWithCount(1))
                    }
                }
                seen.values.toList()
            } catch (_: Throwable) {
                emptyList()
            }
        }

        private fun resolveServerLevel(): ServerLevel? {
            val mc = Minecraft.getInstance()
            return mc.singleplayerServer?.overworld()
        }
    }

    override fun getPluginUid(): ResourceLocation = PLUGIN_UID

    override fun registerCategories(registration: IRecipeCategoryRegistration) {
        val guiHelper = registration.jeiHelpers.guiHelper

        // Create a unique category and RecipeType for every entry in the enum
        for (bagType in LootBagType.entries) {
            val typeId = bagType.name.lowercase()
            val recipeType = RecipeType.create("lootbags", "${typeId}_drops", LootBagRecipe::class.java)
            
            RECIPE_TYPES[bagType] = recipeType
            
            // Register category instance with its specific type and icon
            registration.addRecipeCategories(
                LootBagRecipeCategory(guiHelper, bagType, recipeType)
            )
        }
    }

    override fun registerRecipes(registration: IRecipeRegistration) {
        val pageSize = 24 // Adjusted to match 6x4 or similar grid
        
        for (bagType in LootBagType.entries) {
            val recipeType = RECIPE_TYPES[bagType] ?: continue
            val bagRecipes = mutableListOf<LootBagRecipe>()
            
            val outputs = getOrGenerateOutputs(bagType)
            val totalPages = maxOf(1, (maxOf(1, outputs.size) + pageSize - 1) / pageSize)

            for (page in 0 until totalPages) {
                val from = page * pageSize
                val to = minOf(from + pageSize, outputs.size)
                val pageOutputs = if (outputs.isEmpty()) emptyList() else outputs.subList(from, to)

                bagRecipes.add(
                    LootBagRecipe(
                        bagType = bagType,
                        bag = ItemStack(bagType.asItem()),
                        outputs = pageOutputs,
                        pageIndex = page + 1,
                        totalPages = totalPages
                    )
                )
            }
            
            // Add the recipes specifically to this bag's unique RecipeType
            registration.addRecipes(recipeType, bagRecipes)
        }
    }

    private fun getOrGenerateOutputs(bagType: LootBagType): List<ItemStack> {
        runtimeOutputs[bagType]?.let { if (it.isNotEmpty()) return it }
        val serverLevel = resolveServerLevel()
        val generated = if (serverLevel != null) generatePotentialLootNow(bagType, serverLevel) else emptyList()
        if (generated.isNotEmpty()) runtimeOutputs[bagType] = generated
        return generated
    }
}