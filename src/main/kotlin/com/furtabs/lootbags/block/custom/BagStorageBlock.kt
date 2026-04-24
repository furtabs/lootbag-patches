package com.furtabs.lootbags.block.custom

import com.furtabs.lootbags.block.entity.ModBlockEntities
import com.furtabs.lootbags.block.entity.custom.BagStorageBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraftforge.network.NetworkHooks

class BagStorageBlock(
    properties: Properties = Properties.of()
        .strength(3.5F)
        .requiresCorrectToolForDrops()
) : LootBagEntityBlock(properties) {

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity = BagStorageBlockEntity(pos, state)

    override fun <T : BlockEntity?> getTicker(
        level: Level,
        state: BlockState,
        blockEntityType: BlockEntityType<T>
    ): BlockEntityTicker<T>? = if (level.isClientSide) {
        null
    } else {
        createTickerHelper(blockEntityType, ModBlockEntities.BAG_STORAGE.get()) { level1, pos, state1, blockEntity ->
            blockEntity.tick(level1, pos, state1)
        }
    }

    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL

    override fun use(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS
        }
        val blockEntity = level.getBlockEntity(pos) as? BagStorageBlockEntity
            ?: return InteractionResult.FAIL
        if (player is ServerPlayer) {
            NetworkHooks.openScreen(player, blockEntity, pos)
        } else {
            player.openMenu(blockEntity)
        }
        return InteractionResult.CONSUME
    }

    override fun onRemove(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        newState: BlockState,
        movedByPiston: Boolean
    ) {
        level.getBlockEntity(pos)?.invalidateCaps()
        super.onRemove(state, level, pos, newState, movedByPiston)
    }

    override fun playerWillDestroy(level: Level, pos: BlockPos, state: BlockState, player: Player) {
        val blockEntity = level.getBlockEntity(pos)
        if (blockEntity is BagStorageBlockEntity) {
            if (!level.isClientSide && player.isCreative && blockEntity.storedBagAmount > 0) {
                val stack = ItemStack(this.asItem())
                blockEntity.saveToItem(stack)
                val entity = ItemEntity(level, pos.x.toDouble() + 0.5, pos.y.toDouble() + 0.5, pos.z.toDouble() + 0.5, stack)
                entity.setDefaultPickUpDelay()
                level.addFreshEntity(entity)
            }
        }
        super.playerWillDestroy(level, pos, state, player)
    }
}
