package com.furtabs.lootbags.item

import com.furtabs.lootbags.LootBags
import com.furtabs.lootbags.item.custom.LootBagItem
import com.furtabs.lootbags.util.LootBagType
import net.minecraft.world.item.Item
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.registries.DeferredHolder
import java.util.function.Supplier

object ModItems {
    @JvmField
    val ITEMS: DeferredRegister.Items =
        DeferredRegister.createItems(LootBags.MOD_ID)

    @JvmField
    val COMMON_LOOT_BAG: DeferredHolder<Item, Item> = registerLootBagItem(LootBagType.COMMON)

    @JvmField
    val UNCOMMON_LOOT_BAG: DeferredHolder<Item, Item> = registerLootBagItem(LootBagType.UNCOMMON)

    @JvmField
    val RARE_LOOT_BAG: DeferredHolder<Item, Item> = registerLootBagItem(LootBagType.RARE)

    @JvmField
    val EPIC_LOOT_BAG: DeferredHolder<Item, Item> = registerLootBagItem(LootBagType.EPIC)

    @JvmField
    val LEGENDARY_LOOT_BAG: DeferredHolder<Item, Item> = registerLootBagItem(LootBagType.LEGENDARY)

    val LOOT_BAGS: List<DeferredHolder<Item, Item>> = listOf(
        COMMON_LOOT_BAG,
        UNCOMMON_LOOT_BAG,
        RARE_LOOT_BAG,
        EPIC_LOOT_BAG,
        LEGENDARY_LOOT_BAG
    )

    private fun registerLootBagItem(type: LootBagType): DeferredHolder<Item, Item> =
        ITEMS.register(type.itemId, Supplier { LootBagItem(type) })

    fun register(eventBus: IEventBus) {
        ITEMS.register(eventBus)
    }
}
