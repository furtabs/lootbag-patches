package com.furtabs.lootbags.screen

import com.furtabs.lootbags.LootBags
import com.furtabs.lootbags.screen.custom.BagOpenerMenu
import com.furtabs.lootbags.screen.custom.BagStorageMenu
import com.furtabs.lootbags.screen.custom.LootRecyclerMenu
import com.furtabs.lootbags.screen.custom.OpenLootBagMenu
import net.minecraft.core.registries.Registries
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.MenuType
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier

object ModMenuTypes {
    @JvmField
    val MENUS: DeferredRegister<MenuType<*>> =
        DeferredRegister.create(Registries.MENU, LootBags.MOD_ID)

    @JvmField
    val BAG_STORAGE: DeferredHolder<MenuType<*>, MenuType<BagStorageMenu>> =
        MENUS.register("bag_storage", Supplier {
            IMenuTypeExtension.create { containerId, inv, buf ->
                BagStorageMenu(containerId, inv, inv.player.level(), buf)
            }
        })

    @JvmField
    val BAG_OPENER: DeferredHolder<MenuType<*>, MenuType<BagOpenerMenu>> =
        MENUS.register("bag_opener", Supplier {
            IMenuTypeExtension.create { containerId, inv, buf ->
                BagOpenerMenu(containerId, inv, inv.player.level(), buf)
            }
        })

    @JvmField
    val LOOT_RECYCLER: DeferredHolder<MenuType<*>, MenuType<LootRecyclerMenu>> =
        MENUS.register("loot_recycler", Supplier {
            IMenuTypeExtension.create { containerId, inv, buf ->
                LootRecyclerMenu(containerId, inv, inv.player.level(), buf)
            }
        })

    @JvmField
    val OPEN_LOOT_BAG: DeferredHolder<MenuType<*>, MenuType<OpenLootBagMenu>> =
        MENUS.register("open_loot_bag", Supplier {
            IMenuTypeExtension.create { containerId, inv, buf ->
                OpenLootBagMenu(OPEN_LOOT_BAG.get(), containerId, inv, buf)
            }
        })

    fun register(eventBus: IEventBus) {
        MENUS.register(eventBus)
    }
}
