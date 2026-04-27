package com.furtabs.lootbags.event

import net.minecraft.util.Mth
import net.minecraft.util.RandomSource
import net.minecraft.world.item.ItemStack
import net.minecraft.core.Direction
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import com.furtabs.lootbags.LootBags
import com.furtabs.lootbags.util.LootBagType
import com.furtabs.lootbags.util.newItemEntitiesForDropping
import com.furtabs.lootbags.block.entity.ModBlockEntities
import com.furtabs.lootbags.block.entity.custom.BagOpenerBlockEntity
import com.furtabs.lootbags.block.entity.custom.LootRecyclerBlockEntity
import com.furtabs.lootbags.block.entity.custom.BagStorageBlockEntity

/**
 * GAME BUS: Handles events occurring during active gameplay.
 */
@EventBusSubscriber(modid = LootBags.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
object ModEvents {

    @SubscribeEvent
    fun onLivingDrops(event: LivingDropsEvent) {
        for (bagType in LootBagType.entries) {
            if (!bagType.droppable) continue

            val random: RandomSource = event.entity.random
            val rand = Mth.nextDouble(random, 0.0, 1.0)
            if (rand <= bagType.dropChance) {
                val entityPos = event.entity.getPosition(0F)
                val stack = ItemStack(bagType.asItem(), 1)
                val entities = newItemEntitiesForDropping(event.entity.level(), entityPos, stack)
                event.drops.addAll(entities)

                LootBags.LOGGER.debug(
                    "Generated loot bags of type {} for {} at {}",
                    bagType, event.entity.type.description.string, entityPos
                )
                break
            }
        }
    }
}

/**
 * MOD BUS: Handles setup, registration, and capability attachment.
 * This is where automation (Piping/Hoppers) is enabled.
 */
@EventBusSubscriber(modid = LootBags.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
object ModBusEvents {

    @SubscribeEvent
    fun registerCapabilities(event: RegisterCapabilitiesEvent) {
        
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            ModBlockEntities.BAG_OPENER.get()
        ) { be, side ->
            if (be is BagOpenerBlockEntity) {
                if (side == Direction.UP) be.inputItemHandler else be.outputItemHandler
            } else null
        }

        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            ModBlockEntities.LOOT_RECYCLER.get()
        ) { be, side ->
            if (be is LootRecyclerBlockEntity) {
                be.itemHandler 
            } else null
        }

        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            ModBlockEntities.BAG_STORAGE.get()
        ) { be, _ ->
            if (be is BagStorageBlockEntity) {
                // Returns the single handler regardless of side
                be.itemHandler 
            } else null
        }
    }
}