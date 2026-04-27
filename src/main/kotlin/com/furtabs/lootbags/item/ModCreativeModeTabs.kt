package com.furtabs.lootbags.item

import com.furtabs.lootbags.LootBags
import com.furtabs.lootbags.block.ModBlocks
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.ItemStack
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier

object ModCreativeModeTabs {
    @JvmField
    val CREATIVE_MODE_TAB: DeferredRegister<CreativeModeTab> =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, LootBags.MOD_ID)

    @JvmField
    val LOOT_BAGS_TAB: DeferredHolder<CreativeModeTab, CreativeModeTab> =
        CREATIVE_MODE_TAB.register("loot_bags_tab", Supplier {
            CreativeModeTab.builder()
                .icon { ItemStack(ModItems.COMMON_LOOT_BAG.get()) }
                .title(Component.translatable("creativetab.lootbags.loot_bags_tab"))
                .displayItems { _, output ->
                    output.accept(ModItems.COMMON_LOOT_BAG.get())
                    output.accept(ModItems.UNCOMMON_LOOT_BAG.get())
                    output.accept(ModItems.RARE_LOOT_BAG.get())
                    output.accept(ModItems.EPIC_LOOT_BAG.get())
                    output.accept(ModItems.LEGENDARY_LOOT_BAG.get())

                    output.accept(ModBlocks.LOOT_RECYCLER.get())
                    output.accept(ModBlocks.BAG_OPENER.get())
                    output.accept(ModBlocks.BAG_STORAGE.get())
                }
                .build()
        })

    fun register(eventBus: IEventBus) {
        CREATIVE_MODE_TAB.register(eventBus)
    }
}
