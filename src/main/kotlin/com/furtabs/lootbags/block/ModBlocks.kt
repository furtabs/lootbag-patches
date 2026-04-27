package com.furtabs.lootbags.block

import com.furtabs.lootbags.LootBags
import com.furtabs.lootbags.block.custom.BagOpenerBlock
import com.furtabs.lootbags.block.custom.BagStorageBlock
import com.furtabs.lootbags.block.custom.LootRecyclerBlock
import com.furtabs.lootbags.item.ModItems
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier

object ModBlocks {
    @JvmField
    val BLOCKS: DeferredRegister.Blocks =
        DeferredRegister.createBlocks(LootBags.MOD_ID)

    @JvmField
    val LOOT_RECYCLER: DeferredHolder<Block, Block> =
        registerBlockWithItem("loot_recycler", Supplier { LootRecyclerBlock() })

    @JvmField
    val BAG_OPENER: DeferredHolder<Block, Block> =
        registerBlockWithItem("bag_opener", Supplier { BagOpenerBlock() })

    @JvmField
    val BAG_STORAGE: DeferredHolder<Block, Block> =
        registerBlockWithItem("bag_storage", Supplier { BagStorageBlock() })

    private fun registerBlockWithItem(name: String, block: Supplier<out Block>): DeferredHolder<Block, Block> {
        val defBlock = BLOCKS.register(name, block)
        ModItems.ITEMS.register(name, Supplier {
            BlockItem(defBlock.get(), Item.Properties())
        })
        return defBlock
    }

    fun register(eventBus: IEventBus) {
        BLOCKS.register(eventBus)
    }
}
