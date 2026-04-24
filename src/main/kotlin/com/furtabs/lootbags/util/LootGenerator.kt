package com.furtabs.lootbags.util

import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.Mth
import net.minecraft.util.RandomSource
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.storage.loot.LootDataId
import net.minecraft.world.level.storage.loot.LootDataType
import net.minecraft.world.level.storage.loot.LootParams
import net.minecraft.world.level.storage.loot.LootTable
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets

/** Matches GUI / item handler capacity for opened bags. */
const val MAX_LOOT_BAG_ITEM_STACKS: Int = 5

/**
 * How many stacks (1..[MAX_LOOT_BAG_ITEM_STACKS]) the bag tries to fill. Weights are **higher for fewer
 * stacks** so 1 item is most common and 5 is the rarest.
 */
private val LOOT_STACK_COUNT_WEIGHTS: IntArray = intArrayOf(10, 7, 5, 3, 2)

fun rollLootBagDisplayedItemCount(random: RandomSource): Int {
    var totalWeight = 0
    for (w in LOOT_STACK_COUNT_WEIGHTS) {
        totalWeight += w
    }
    var roll = random.nextInt(totalWeight)
    for (i in LOOT_STACK_COUNT_WEIGHTS.indices) {
        roll -= LOOT_STACK_COUNT_WEIGHTS[i]
        if (roll < 0) {
            return i + 1
        }
    }
    return MAX_LOOT_BAG_ITEM_STACKS
}

class LootGenerator(private val bagType: LootBagType) {
    /**
     * Fills up to [maxStacks] item stacks using only vanilla/mod **loot tables**: each contribution is
     * `LootTable.getRandomItems` from a **randomly chosen** registered table ID (new pick each roll).
     *
     * Each bag rarity maps to a **disjoint loot-table tier band** (0..4) so higher bags never draw from
     * the same pool as lower ones; luck rises with tier for better rolls inside those tables.
     *
     * @param maxStacks Maximum number of non-empty stacks returned.
     */
    fun generateLoot(
        level: ServerLevel,
        lootParamsBuilder: LootParams.Builder = LootParams.Builder(level),
        maxStacks: Int = MAX_LOOT_BAG_ITEM_STACKS
    ): List<ItemStack> {
        val cap = maxStacks.coerceIn(1, MAX_LOOT_BAG_ITEM_STACKS)
        val tier = bagType.rarity.toInt().coerceIn(0, 8)
        val tierLuckBonus = tier * 22f
        return when (bagType) {
            LootBagType.COMMON -> sampleFromRandomLootTables(
                level, lootParamsBuilder, cap,
                luckMin = 0f, luckMax = 35f + tierLuckBonus, maxTablePicks = 32, bagTier = tier,
                contentTierMin = 0, contentTierMax = 0
            )
            LootBagType.UNCOMMON -> sampleFromRandomLootTables(
                level, lootParamsBuilder, cap,
                luckMin = 18f, luckMax = 95f + tierLuckBonus, maxTablePicks = 40, bagTier = tier,
                contentTierMin = 1, contentTierMax = 1
            )
            LootBagType.RARE -> sampleFromRandomLootTables(
                level, lootParamsBuilder, cap,
                luckMin = 55f, luckMax = 200f + tierLuckBonus, maxTablePicks = 56, bagTier = tier,
                contentTierMin = 2, contentTierMax = 2
            )
            LootBagType.EPIC -> sampleFromRandomLootTables(
                level, lootParamsBuilder, cap,
                luckMin = 160f, luckMax = 420f + tierLuckBonus, maxTablePicks = 96, bagTier = tier,
                contentTierMin = 3, contentTierMax = 3
            )
            LootBagType.LEGENDARY -> sampleFromRandomLootTables(
                level, lootParamsBuilder, cap,
                luckMin = 520f, luckMax = 1200f + tierLuckBonus, maxTablePicks = 200, bagTier = tier,
                contentTierMin = 4, contentTierMax = 4
            )
        }
    }
}

/**
 * Repeatedly picks a random loot table from the server's registry and runs one full
 * [LootTable.getRandomItems] roll until [maxStacks] stacks are collected or [maxTablePicks] picks are used.
 * EMPTY tables and failed rolls are skipped; another random table is tried on the next pick.
 *
 * @param bagTier [LootBagType.rarity] — higher tiers skew picks inside the filtered pool and use more luck.
 * @param contentTierMin [contentTierMax] Inclusive band from [lootTableContentTier]; each bag rarity uses a
 * single tier so pools do not overlap. If nothing matches (e.g. modded-only packs), the band widens slightly.
 */
private fun sampleFromRandomLootTables(
    level: ServerLevel,
    lootParamsBuilder: LootParams.Builder,
    maxStacks: Int,
    luckMin: Float,
    luckMax: Float,
    maxTablePicks: Int,
    bagTier: Int,
    contentTierMin: Int,
    contentTierMax: Int
): List<ItemStack> {
    val allIds = level.server.lootData.getKeys(LootDataType.TABLE).toList()
    if (allIds.isEmpty()) {
        return emptyList()
    }
    var bandLo = contentTierMin.coerceIn(0, 4)
    var bandHi = contentTierMax.coerceIn(0, 4)
    var ids = filterLootTablesByContentTier(allIds, bandLo, bandHi)
    var relax = 0
    while (ids.isEmpty() && relax < 5) {
        relax++
        bandLo = (bandLo - 1).coerceAtLeast(0)
        bandHi = (bandHi + 1).coerceAtMost(4)
        ids = filterLootTablesByContentTier(allIds, bandLo, bandHi)
    }
    if (ids.isEmpty()) {
        ids = allIds
    }
    val random = level.random
    var bagAttempts = 0
    val result = mutableListOf<ItemStack>()
    do {
        result.clear()
        var picks = 0
        while (result.size < maxStacks && picks < maxTablePicks) {
            picks++
            val id = pickLootTableId(ids, random, bagTier)
            val lootTable = level.server.lootData.getElement(LootDataId(LootDataType.TABLE, id))
            if (lootTable == null || lootTable === LootTable.EMPTY) {
                continue
            }
            val luck = if (luckMin >= luckMax) luckMax else Mth.randomBetween(random, luckMin, luckMax)
            val paramSet = lootParamSetForTier(bagTier)
            val params = lootParamsBuilder.withLuck(luck).create(paramSet)
            for (stack in lootTable.getRandomItems(params)) {
                if (result.size >= maxStacks) {
                    break
                }
                if (!stack.isEmpty) {
                    result.add(stack.copy())
                }
            }
        }
        bagAttempts++
    } while (result.isEmpty() && bagAttempts < 100)

    return result.filter { !it.isEmpty }.take(maxStacks)
}

/** Chest-style tables only need [LootContextParams.ORIGIN], which the bag already supplies — improves rolls vs EMPTY. */
private fun lootParamSetForTier(bagTier: Int): LootContextParamSet =
    if (bagTier >= 1) LootContextParamSets.CHEST else LootContextParamSets.EMPTY

private fun filterLootTablesByContentTier(
    ids: List<ResourceLocation>,
    minTier: Int,
    maxTier: Int
): List<ResourceLocation> {
    val lo = minTier.coerceIn(0, 4)
    val hi = maxTier.coerceIn(0, 4)
    return ids.filter { lootTableContentTier(it) in lo..hi }
}

/**
 * Vanilla 1.20.1-style grouping: **0** = junk / starter chests, **1** = early structures, **2** = nether /
 * mid-stronghold / outer bastion, **3** = high-value structure loot, **4** = top-tier (legendary-only band).
 * Non-minecraft IDs use path heuristics so modded tables still land in a sensible band.
 */
private fun lootTableContentTier(id: ResourceLocation): Int {
    val path = id.path.lowercase()
    val ns = id.namespace.lowercase()
    if (ns != "minecraft") {
        return modLootTableContentTier(path)
    }
    if (path.startsWith("blocks/")) {
        return 0
    }
    if (path.startsWith("gameplay/fishing/")) {
        return if (path.contains("treasure")) 4 else 0
    }
    if (path.startsWith("chests/")) {
        return when {
            path.startsWith("chests/village/") || path.startsWith("chests/spawn_bonus") -> 0
            path.startsWith("chests/igloo_chest") -> 0
            path == "chests/desert_pyramid" || path == "chests/jungle_temple" ||
                path == "chests/jungle_temple_dispenser" || path == "chests/pillager_outpost" ||
                path == "chests/abandoned_mineshaft" || path.startsWith("chests/shipwreck_map") ||
                path.startsWith("chests/shipwreck_supply") || path.startsWith("chests/underwater_ruin_") -> 1
            path.startsWith("chests/nether_bridge") || path.startsWith("chests/bastion_bridge") ||
                path.startsWith("chests/bastion_hoglin_stable") || path.startsWith("chests/bastion_other") ||
                path.startsWith("chests/stronghold_corridor") || path.startsWith("chests/stronghold_crossing") -> 2
            path.startsWith("chests/ancient_city") || path.startsWith("chests/stronghold_library") ||
                path.startsWith("chests/ruined_portal") -> 3
            path.startsWith("chests/bastion_treasure") || path.startsWith("chests/end_city_treasure") ||
                path.startsWith("chests/woodland_mansion") || path.startsWith("chests/buried_treasure") ||
                path.startsWith("chests/shipwreck_treasure") -> 4
            else -> 1
        }
    }
    if (path.startsWith("entities/")) {
        return if (entityLootPathLikelyRewarding(path)) 2 else 0
    }
    if (path.contains("shearing")) {
        return 0
    }
    return 1
}

private fun modLootTableContentTier(path: String): Int =
    when {
        path.contains("end_city") || path.contains("woodland_mansion") ||
            (path.contains("legendary") && path.contains("chest")) -> 4
        path.contains("bastion_treasure") ||
            (path.contains("treasure") && path.contains("bastion")) -> 4
        path.contains("buried") || path.contains("shipwreck_treasure") ||
            (path.contains("fishing") && path.contains("treasure")) -> 4
        path.contains("ancient_city") || path.contains("warden") ||
            path.contains("stronghold_library") || path.contains("ruined_portal") -> 3
        path.contains("bastion") || path.contains("nether_bridge") ||
            path.contains("stronghold_corridor") || path.contains("stronghold_crossing") -> 2
        path.contains("chest") || path.contains("dungeon") || path.contains("temple") -> 1
        else -> 1
    }

/**
 * Weighted pick: higher [bagTier] favors treasure / structure chest tables more strongly.
 * Tier 0 (common) uses a **small** chest/shipwreck-style nudge only — still mostly flat.
 */
private fun pickLootTableId(ids: List<ResourceLocation>, random: RandomSource, bagTier: Int): ResourceLocation {
    if (ids.size == 1) {
        return ids[0]
    }
    var total = 0f
    val weights = FloatArray(ids.size)
    for (i in ids.indices) {
        val w = lootTableTierBias(ids[i], bagTier)
        weights[i] = w
        total += w
    }
    if (total <= 0f) {
        return ids[random.nextInt(ids.size)]
    }
    var roll = random.nextFloat() * total
    for (i in weights.indices) {
        roll -= weights[i]
        if (roll <= 0f) {
            return ids[i]
        }
    }
    return ids[ids.lastIndex]
}

/**
 * Bias toward rewarding table paths and **downweight** junk (blocks/, generic entities/, etc.) at higher tiers.
 * Legendary (tier 4) uses the strongest curve so “common” worldgen tables rarely win the weighted pick.
 */
private fun lootTableTierBias(id: ResourceLocation, bagTier: Int): Float {
    val path = id.path.lowercase()
    val ns = id.namespace.lowercase()

    if (bagTier == 0) {
        var wCommon = 1f
        if (ns == "minecraft" && path.startsWith("chests/")) {
            wCommon += 0.2f
        }
        if (path.contains("shipwreck") || path.contains("buried_treasure") || path.contains("ruined_portal")) {
            wCommon += 0.08f
        }
        return wCommon.coerceIn(0.25f, 24f)
    }

    val t = bagTier.coerceIn(1, 8).toFloat()
    val leg = bagTier >= 4
    val epic = bagTier >= 3

    var score = 1f

    if (ns == "minecraft" && path.startsWith("chests/")) {
        score += 0.18f * t
        if (path.contains("end_city") || path.contains("bastion") || path.contains("ancient_city") ||
            path.contains("stronghold") || path.contains("woodland") || path.contains("trial_chambers") ||
            path.contains("vault") || path.contains("treasure")
        ) {
            score += (if (leg) 1.4f else if (epic) 0.85f else 0.45f) * t
        }
    }
    if (path.contains("treasure")) {
        score += (if (leg) 0.95f else 0.35f) * t
    }
    if (path.contains("end_city") || path.contains("ancient_city") || path.contains("trial_chambers") ||
        path.contains("vault") || path.contains("ominous")
    ) {
        score += (if (leg) 1.5f else 0.55f) * t
    }
    if (path.contains("bastion") || path.contains("stronghold") || path.contains("woodland")) {
        score += (if (leg) 0.9f else 0.4f) * t
    }
    if (path.contains("shipwreck") || path.contains("buried") || path.contains("ruined_portal")) {
        score += (if (leg) 0.35f else 0.22f) * t
    }
    if (path.contains("nether_bridge") || path.contains("hoglin_stable")) {
        score += 0.12f * t
    }

    val trash = lootTableTrashMultiplier(path, ns, bagTier)
    return (score * trash).coerceIn(0.008f, 120f)
}

/** Pushes weight down for tables that usually roll dirt, grass, or generic mob drops — harsher for Legendary. */
private fun lootTableTrashMultiplier(path: String, ns: String, bagTier: Int): Float {
    if (bagTier < 2) {
        return 1f
    }
    var m = 1f
    val harsh = when (bagTier) {
        2 -> 0.45f
        3 -> 0.22f
        else -> 0.09f
    }
    if (path.startsWith("blocks/")) {
        m *= harsh
    }
    if (path.startsWith("entities/") && !entityLootPathLikelyRewarding(path)) {
        m *= harsh * 0.75f
    }
    if (path.contains("shearing")) {
        m *= harsh
    }
    if (path.startsWith("gameplay/fishing") && !path.contains("treasure")) {
        m *= harsh * 0.85f
    }
    if (path.startsWith("entities/") && (path.contains("grass") || path.contains("fern"))) {
        m *= harsh
    }
    if (bagTier >= 4) {
        if (path.startsWith("chests/village/")) {
            m *= 0.28f
        }
        if (path.contains("igloo") || path.contains("underwater_ruin")) {
            m *= 0.35f
        }
        if (path.startsWith("chests/spawn_bonus")) {
            m *= 0.2f
        }
    }
    return m.coerceIn(0.006f, 1f)
}

private fun entityLootPathLikelyRewarding(path: String): Boolean =
    path.contains("equipment") || path.contains("raid") || path.contains("wither") ||
        path.contains("warden") || path.contains("treasure") || path.contains("head") ||
        path.contains("nautilus") || path.contains("trident")
