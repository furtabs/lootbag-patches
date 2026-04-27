package com.furtabs.lootbags.network

import com.furtabs.lootbags.LootBags
import com.furtabs.lootbags.network.custom.ServerboundSelectLootBagTypePacket
import net.minecraft.client.Minecraft

object ModNetworks {
    fun register() {}

    fun sendSelectLootBagTypePacket(packet: ServerboundSelectLootBagTypePacket) {
        val player = Minecraft.getInstance().player ?: return
        val menu = player.containerMenu
        if (menu is com.furtabs.lootbags.screen.custom.BagStorageMenu) {
            val idx = packet.lootBagTypeOrdinal % com.furtabs.lootbags.util.LootBagType.entries.size
            menu.targetBagType = com.furtabs.lootbags.util.LootBagType.entries[idx]
        }
    }
}
