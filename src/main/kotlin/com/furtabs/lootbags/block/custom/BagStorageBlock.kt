package com.furtabs.lootbags.block.custom

import com.furtabs.lootbags.block.entity.ModBlockEntities
import com.furtabs.lootbags.block.entity.custom.BagStorageBlockEntity
import com.mojang.serialization.MapCodec
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.BlockHitResult

class BagStorageBlock(
    properties: Properties = Properties.of()
        .strength(3.5F)
        .requiresCorrectToolForDrops()
        .noOcclusion() 
) : LootBagEntityBlock(properties) {

    companion object {
        val CODEC: MapCodec<BagStorageBlock> = simpleCodec(::BagStorageBlock)
        val FACING = BlockStateProperties.HORIZONTAL_FACING
    }

    override fun codec(): MapCodec<out LootBagEntityBlock> = CODEC

    init {
        // Sets default direction so the model loader doesn't crash
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {
        return defaultBlockState().setValue(FACING, context.horizontalDirection.opposite)
    }

    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity = BagStorageBlockEntity(pos, state)

    override fun <T : BlockEntity?> getTicker(
        level: Level,
        state: BlockState,
        blockEntityType: BlockEntityType<T>
    ): BlockEntityTicker<T>? = if (level.isClientSide) null else {
        createTickerHelper(blockEntityType, ModBlockEntities.BAG_STORAGE.get()) { level1, pos, state1, blockEntity ->
            blockEntity.tick(level1, pos, state1)
        }
    }

    override fun useWithoutItem(state: BlockState, level: Level, pos: BlockPos, player: Player, hit: BlockHitResult): InteractionResult {
        if (level.isClientSide) return InteractionResult.SUCCESS
        val blockEntity = level.getBlockEntity(pos) as? BagStorageBlockEntity ?: return InteractionResult.FAIL
        (player as? ServerPlayer)?.openMenu(blockEntity) { buf -> buf.writeBlockPos(pos) }
        return InteractionResult.CONSUME
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, movedByPiston: Boolean) {
        if (state.block != newState.block) {
            (level.getBlockEntity(pos) as? BagStorageBlockEntity)?.let { blockEntity ->
                val inventory = net.minecraft.world.SimpleContainer(blockEntity.itemHandler.slots)
                for (i in 0 until blockEntity.itemHandler.slots) {
                    inventory.setItem(i, blockEntity.itemHandler.getStackInSlot(i))
                }
                net.minecraft.world.Containers.dropContents(level, pos, inventory)
            }
            super.onRemove(state, level, pos, newState, movedByPiston)
        }
    }
}