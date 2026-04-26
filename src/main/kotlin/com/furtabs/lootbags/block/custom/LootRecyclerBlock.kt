package com.furtabs.lootbags.block.custom

import com.furtabs.lootbags.block.entity.ModBlockEntities
import com.furtabs.lootbags.block.entity.custom.LootRecyclerBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.MenuProvider
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.Mirror
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.DirectionProperty
import net.minecraft.world.phys.BlockHitResult

// Note: Ensure LootBagEntityBlock also extends BaseEntityBlock
class LootRecyclerBlock(
    properties: Properties = Properties.of()
        .strength(3.5F)
        .requiresCorrectToolForDrops()
) : LootBagEntityBlock(properties) {

    companion object {
        val FACING: DirectionProperty = BlockStateProperties.HORIZONTAL_FACING
    }

    init {
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {
        return defaultBlockState().setValue(FACING, context.horizontalDirection.opposite)
    }

    /* --- 1.21.1 Interaction Logic --- */

    override fun use(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult {
        if (!level.isClientSide) {
            val blockEntity = level.getBlockEntity(pos)
            if (blockEntity is LootRecyclerBlockEntity) {
                // In 1.21.1, NetworkHooks is gone. Use this instead:
                player.openMenu(blockEntity as MenuProvider)
            } else {
                throw IllegalStateException("Our Container provider is missing!")
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide)
    }

    /* --- Standard Boilerplate --- */

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity = 
        LootRecyclerBlockEntity(pos, state)

    override fun <T : BlockEntity?> getTicker(
        level: Level,
        state: BlockState,
        blockEntityType: BlockEntityType<T>
    ): BlockEntityTicker<T>? = if (level.isClientSide) {
        null
    } else {
        createTickerHelper(blockEntityType, ModBlockEntities.LOOT_RECYCLER.get()) { level1, pos, state1, blockEntity ->
            blockEntity.tick(level1, pos, state1)
        }
    }

    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, movedByPiston: Boolean) {
        if (state.block != newState.block) {
            val blockEntity = level.getBlockEntity(pos)
            if (blockEntity is LootRecyclerBlockEntity) {
                // Drop items here if you have an inventory
                blockEntity.invalidateCaps() 
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston)
    }

    // Rotations
    override fun rotate(state: BlockState, rotation: Rotation): BlockState =
        state.setValue(FACING, rotation.rotate(state.getValue(FACING)))

    override fun mirror(state: BlockState, mirror: Mirror): BlockState =
        state.rotate(mirror.getRotation(state.getValue(FACING)))
}