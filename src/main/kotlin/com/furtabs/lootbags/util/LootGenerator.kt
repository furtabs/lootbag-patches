package com.furtabs.lootbags.util

import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.RandomSource
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.storage.loot.LootParams
import net.minecraft.world.level.storage.loot.LootTable
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets
import java.util.concurrent.ConcurrentHashMap

/**
 * Constants and Weights for Loot Generation
 */
const val MAX_LOOT_BAG_ITEM_STACKS: Int = 5
private val STACK_COUNT_WEIGHTS: IntArray = intArrayOf(5, 4, 3, 2, 1)

/** Cache to prevent repeated string parsing and tier calculation on every loot roll. */
private val TIER_CACHE = ConcurrentHashMap<ResourceLocation, Int>()

/**
 * Weighted random roll to determine how many item stacks from the loot table
 * should actually be given to the player.
 */
fun rollLootBagDisplayedItemCount(random: RandomSource): Int {
    val weights = STACK_COUNT_WEIGHTS
    val totalWeight = weights.sum()
    var roll = random.nextInt(totalWeight)
    for (i in weights.indices) {
        roll -= weights[i]
        if (roll < 0) return i + 1
    }
    return 1
}

class LootGenerator(private val bagType: LootBagType, private val bagTier: Int) {

fun generateLoot(
        level: ServerLevel,
        lootParamsBuilder: LootParams.Builder = LootParams.Builder(level),
        maxStacks: Int = MAX_LOOT_BAG_ITEM_STACKS
    ): List<ItemStack> {
        // Do not use level.random here; JEI preview can be triggered from render thread.
        // Using a local RNG avoids ThreadingDetector violations.
        val random = RandomSource.create()

        // 1. Pick one loot table ID from known vanilla chest tables.
        val selectedId = pickLootTableId(VANILLA_CHEST_TABLES, random, bagTier)

        // 2. Create the ResourceKey and fetch the actual table using reloadable registries.
        val tableKey = ResourceKey.create(Registries.LOOT_TABLE, selectedId)
        val registries = level.server.reloadableRegistries()
        val lootTable = registries.getLootTable(tableKey)
        
        if (lootTable == LootTable.EMPTY) return emptyList()

        // 3. Generate items
        val params = lootParamsBuilder.create(LootContextParamSets.CHEST)
        val allGeneratedItems = lootTable.getRandomItems(params)

        if (allGeneratedItems.isEmpty()) return emptyList()

        // 7. Stack count and shuffle
        val rolledCount = rollLootBagDisplayedItemCount(random)
        val cap = rolledCount.coerceAtMost(maxStacks)
        
        val shuffled = allGeneratedItems.toMutableList()
        for (i in shuffled.size - 1 downTo 1) {
            val j = random.nextInt(i + 1)
            val temp = shuffled[i]
            shuffled[i] = shuffled[j]
            shuffled[j] = temp
        }
        
        return shuffled.take(cap)
    }
}

private val VANILLA_CHEST_TABLES: List<ResourceLocation> = listOf(
    ResourceLocation.withDefaultNamespace("chests/spawn_bonus_chest"),
    ResourceLocation.withDefaultNamespace("chests/simple_dungeon"),
    ResourceLocation.withDefaultNamespace("chests/abandoned_mineshaft"),
    ResourceLocation.withDefaultNamespace("chests/desert_pyramid"),
    ResourceLocation.withDefaultNamespace("chests/jungle_temple"),
    ResourceLocation.withDefaultNamespace("chests/shipwreck_supply"),
    ResourceLocation.withDefaultNamespace("chests/shipwreck_treasure"),
    ResourceLocation.withDefaultNamespace("chests/ruined_portal"),
    ResourceLocation.withDefaultNamespace("chests/nether_bridge"),
    ResourceLocation.withDefaultNamespace("chests/stronghold_corridor"),
    ResourceLocation.withDefaultNamespace("chests/stronghold_library"),
    ResourceLocation.withDefaultNamespace("chests/end_city_treasure"),
    ResourceLocation.withDefaultNamespace("chests/ancient_city"),
    ResourceLocation.withDefaultNamespace("chests/bastion_treasure")
)

// --- Logic for Tiering and Biasing ---

private fun getOrCacheTier(id: ResourceLocation): Int {
    return TIER_CACHE.getOrPut(id) { calculateLootTableTier(id) }
}

private fun calculateLootTableTier(id: ResourceLocation): Int {
    val path = id.path.lowercase()
    val ns = id.namespace.lowercase()
    
    if (ns != "minecraft") return modLootTableContentTier(path)
    
    if (path.startsWith("archaeology/")) {
        return when {
            path.contains("trail_ruins_rare") -> 3
            path.contains("ocean_ruin_warm") -> 2
            path.contains("desert_pyramid") || path.contains("trail_ruins_common") -> 1
            else -> 0
        }
    }

    if (path.startsWith("chests/")) {
        return when {
            path.contains("village") || path.contains("spawn_bonus") || 
            path.contains("igloo") || path.contains("shipwreck_supply") ||
            path.contains("underwater_ruin_small") -> 0

            path.contains("desert_pyramid") || path.contains("jungle_temple") || 
            path.contains("mineshaft") || path.contains("pillager_outpost") || 
            path.contains("shipwreck_map") || path.contains("simple_dungeon") -> 1

            path.contains("nether_bridge") || path.contains("bastion_bridge") || 
            path.contains("stronghold_corridor") || path.contains("stronghold_crossing") ||
            path.contains("shipwreck_treasure") || path.contains("underwater_ruin_big") ||
            path.contains("ruined_portal") -> 2

            path.contains("ancient_city") || path.contains("stronghold_library") || 
            path.contains("bastion_hoglin_stable") || path.contains("bastion_other") -> 3

            path.contains("bastion_treasure") || path.contains("end_city") || 
            path.contains("buried_treasure") || path.contains("woodland_mansion") -> 4

            else -> 1
        }
    }

    if (path.startsWith("entities/")) {
        return if (entityLootPathLikelyRewarding(path)) 2 else 0
    }

    return 1
}

private fun modLootTableContentTier(path: String): Int = when {
    path.contains("legendary") || path.contains("treasure") || path.contains("mythic") -> 4
    path.contains("ancient") || path.contains("stronghold") || path.contains("epic") -> 3
    path.contains("nether") || path.contains("bastion") || path.contains("rare") -> 2
    path.contains("dungeon") || path.contains("temple") || path.contains("uncommon") -> 1
    else -> 0
}

private fun pickLootTableId(ids: List<ResourceLocation>, random: RandomSource, bagTier: Int): ResourceLocation {
    if (ids.isEmpty()) return ResourceLocation.withDefaultNamespace("empty")
    
    var total = 0f
    val weights = FloatArray(ids.size)
    for (i in ids.indices) {
        val w = lootTableTierBias(ids[i], bagTier)
        weights[i] = w
        total += w
    }
    
    var roll = random.nextFloat() * total
    for (i in weights.indices) {
        roll -= weights[i]
        if (roll <= 0f) return ids[i]
    }
    return ids.last()
}

private fun lootTableTierBias(id: ResourceLocation, bagTier: Int): Float {
    val tier = getOrCacheTier(id)
    val path = id.path.lowercase()
    
    var score = when {
        tier == bagTier -> 25.0f
        tier == bagTier - 1 || tier == bagTier + 1 -> 5.0f
        else -> 0.5f
    }
    
    if (path.contains("chests/")) score *= 2.0f
    
    return score.coerceIn(0.01f, 200f)
}

private fun entityLootPathLikelyRewarding(path: String): Boolean =
    path.contains("equipment") || path.contains("raid") || path.contains("boss") || 
    path.contains("wither") || path.contains("warden") || path.contains("evoker")