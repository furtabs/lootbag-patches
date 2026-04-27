package com.furtabs.lootbags.block.custom

import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Item
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.block.BaseEntityBlock

abstract class LootBagEntityBlock(properties: Properties) : BaseEntityBlock(properties) {
    override fun appendHoverText(
        stack: ItemStack,
        context: Item.TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag)
    }
}
