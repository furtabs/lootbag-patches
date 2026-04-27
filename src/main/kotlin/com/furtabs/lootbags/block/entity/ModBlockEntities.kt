package com.furtabs.lootbags.block.entity

import com.furtabs.lootbags.LootBags
import com.furtabs.lootbags.block.ModBlocks
import com.furtabs.lootbags.block.entity.custom.BagOpenerBlockEntity
import com.furtabs.lootbags.block.entity.custom.BagStorageBlockEntity
import com.furtabs.lootbags.block.entity.custom.LootRecyclerBlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier

object ModBlockEntities {
    @JvmField
    val BLOCK_ENTITIES: DeferredRegister<BlockEntityType<*>> =
        DeferredRegister.create(net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE, LootBags.MOD_ID)

    @JvmField
    val BAG_STORAGE: DeferredHolder<BlockEntityType<*>, BlockEntityType<BagStorageBlockEntity>> =
        BLOCK_ENTITIES.register("bag_storage", Supplier {
            BlockEntityType.Builder.of(::BagStorageBlockEntity, ModBlocks.BAG_STORAGE.get()).build(null)
        })

    @JvmField
    val BAG_OPENER: DeferredHolder<BlockEntityType<*>, BlockEntityType<BagOpenerBlockEntity>> =
        BLOCK_ENTITIES.register("bag_opener", Supplier {
            BlockEntityType.Builder.of(::BagOpenerBlockEntity, ModBlocks.BAG_OPENER.get()).build(null)
        })

    @JvmField
    val LOOT_RECYCLER: DeferredHolder<BlockEntityType<*>, BlockEntityType<LootRecyclerBlockEntity>> =
        BLOCK_ENTITIES.register("loot_recycler", Supplier {
            BlockEntityType.Builder.of(::LootRecyclerBlockEntity, ModBlocks.LOOT_RECYCLER.get()).build(null)
        })

    fun register(eventBus: IEventBus) {
        BLOCK_ENTITIES.register(eventBus)
    }
}
