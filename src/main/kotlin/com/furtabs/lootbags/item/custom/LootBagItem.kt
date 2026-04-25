package com.furtabs.lootbags.item.custom

import com.furtabs.lootbags.screen.ModMenuTypes
import com.furtabs.lootbags.screen.custom.OpenLootBagMenu
import com.furtabs.lootbags.screen.custom.newLootResultHandlerWithLoot
import com.furtabs.lootbags.util.LootBagType
import com.furtabs.lootbags.util.MAX_LOOT_BAG_ITEM_STACKS
import com.furtabs.lootbags.util.readStoredOpenLoot
import com.furtabs.lootbags.util.rollLootBagDisplayedItemCount
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.stats.Stats
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.MenuProvider
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.storage.loot.LootParams
import net.minecraft.world.level.storage.loot.parameters.LootContextParams
import net.minecraftforge.network.NetworkHooks

class LootBagItem(
    val type: LootBagType,
    properties: Properties = Properties().stacksTo(1)
) : Item(properties) {

    override fun use(level: Level, player: Player, usedHand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = player.getItemInHand(usedHand)

        if (!level.isClientSide && level is ServerLevel && player is ServerPlayer) {
            val stored = readStoredOpenLoot(stack)
            
            // The if (stored != null) block below fixes the "Argument type mismatch"
            val handler = if (stored != null) {
                newLootResultHandlerWithLoot(stored)
            } else {
                val maxStacks = rollLootBagDisplayedItemCount(level.random)
                val loots = type.lootGenerator.generateLoot(
                    level,
                    LootParams.Builder(level)
                        .withParameter(LootContextParams.THIS_ENTITY, player)
                        .withParameter(LootContextParams.ORIGIN, player.position())
                        .withParameter(LootContextParams.TOOL, stack),
                    maxStacks = maxStacks
                )
                writeStoredOpenLoot(stack, loots)
                newLootResultHandlerWithLoot(loots)
            }

            val menuProvider = object : MenuProvider {
                override fun getDisplayName() = stack.hoverName
                override fun createMenu(id: Int, inv: Inventory, p: Player) = 
                    OpenLootBagMenu(ModMenuTypes.OPEN_LOOT_BAG.get(), id, inv, handler, usedHand)
            }

            NetworkHooks.openScreen(player, menuProvider) { buf ->
                buf.writeByte(usedHand.ordinal)
                for (i in 0 until MAX_LOOT_BAG_ITEM_STACKS) {
                    buf.writeItem(handler.getStackInSlot(i))
                }
            }
        }

        player.awardStat(Stats.ITEM_USED.get(this))
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide)
    }
}