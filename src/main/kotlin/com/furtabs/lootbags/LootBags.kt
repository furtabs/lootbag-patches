package com.furtabs.lootbags

import com.furtabs.lootbags.block.ModBlocks
import com.furtabs.lootbags.block.entity.ModBlockEntities
import com.furtabs.lootbags.item.ModCreativeModeTabs
import com.furtabs.lootbags.item.ModItems
import com.furtabs.lootbags.jei.LootBagsJEIPlugin
import com.furtabs.lootbags.network.ModNetworks
import com.furtabs.lootbags.screen.ModMenuTypes
import com.furtabs.lootbags.screen.custom.BagOpenerScreen
import com.furtabs.lootbags.screen.custom.BagStorageScreen
import com.furtabs.lootbags.screen.custom.LootRecyclerScreen
import com.furtabs.lootbags.screen.custom.OpenLootBagScreen
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.IEventBus
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.Mod
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS

@Mod(LootBags.MOD_ID)
object LootBags {
    const val MOD_ID = "lootbags"

    val LOGGER: Logger = LogManager.getLogger(MOD_ID)

    init {
        LOGGER.info("$MOD_ID is loading...")
        val modBus: IEventBus = MOD_BUS

        modBus.addListener(::commonSetup)
        modBus.addListener(::clientSetup)

        ModCreativeModeTabs.register(modBus)
        ModItems.register(modBus)
        ModBlocks.register(modBus)
        ModBlockEntities.register(modBus)
        ModMenuTypes.register(modBus)
    }

    private fun commonSetup(event: FMLCommonSetupEvent) {
        event.enqueueWork { ModNetworks.register() }
    }

    private fun clientSetup(event: FMLClientSetupEvent) {
        // Registered via RegisterMenuScreensEvent below on 1.21.1.
    }

    @EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
    object ClientModEvents {
        @SubscribeEvent
        fun onRegisterMenuScreens(event: RegisterMenuScreensEvent) {
            event.register(ModMenuTypes.BAG_STORAGE.get(), ::BagStorageScreen)
            event.register(ModMenuTypes.BAG_OPENER.get(), ::BagOpenerScreen)
            event.register(ModMenuTypes.LOOT_RECYCLER.get(), ::LootRecyclerScreen)
            event.register(ModMenuTypes.OPEN_LOOT_BAG.get(), ::OpenLootBagScreen)
        }
    }

    @EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = [Dist.CLIENT])
    object ClientGameEvents {
        @SubscribeEvent
        fun onClientTick(event: ClientTickEvent.Post) {
            LootBagsJEIPlugin.onClientTickPreload()
        }
    }
}
