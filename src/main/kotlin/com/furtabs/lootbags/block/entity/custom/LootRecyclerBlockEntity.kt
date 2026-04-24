package com.furtabs.lootbags.block.entity.custom

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.util.Mth
import net.minecraft.world.MenuProvider
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.ForgeCapabilities
import net.minecraftforge.common.util.LazyOptional
import net.minecraftforge.items.ItemStackHandler
import com.furtabs.lootbags.block.entity.ModBlockEntities
import com.furtabs.lootbags.screen.custom.LootRecyclerMenu
import com.furtabs.lootbags.util.*
import kotlin.math.min

class LootRecyclerBlockEntity(
    pos: BlockPos,
    blockState: BlockState
) : BlockEntity(ModBlockEntities.LOOT_RECYCLER.get(), pos, blockState), MenuProvider {
    enum class ContainerDataType {
        STORED_BAG_AMOUNT
    }

    private inner class LootRecyclerItemHandler : LootBagItemHandler(SLOTS_COUNT) {
        override fun isInputSlot(slot: Int): Boolean = slot == INPUT_SLOT

        // We want to recycle everything, so only check whether the slot is the input slot.
        // (i.e. Don't need to check whether the item is a loot bag.)
        override fun isItemValid(slot: Int, stack: ItemStack): Boolean = isInputSlot(slot)

        // We want to recycle everything, so for the input slot unleash the maximum stack size.
        override fun getSlotLimit(slot: Int): Int = if (isInputSlot(slot)) {
            1_000_000
        } else {
            super.getSlotLimit(slot)
        }

        override fun extractItem(slot: Int, amount: Int, simulate: Boolean): ItemStack {
            if (simulate) { // NOTE to do simulation check!!!
                return super.extractItem(slot, amount, true)
            } else {
                val resultStack = super.extractItem(slot, amount, false)
                if (slot == OUTPUT_SLOT) {
                    storedBagAmount -= resultStack.count * targetBagType.amountFactorEquivalentTo(LootBagType.COMMON).toInt()
                }
                updateOutputSlot()

                return resultStack
            }
        }

        override fun onContentsChanged(slot: Int) {
            if (slot != OUTPUT_SLOT) {
                // When slots that aren't the output slot are changed, update the output slot.
                updateOutputSlot()
            }
        }

        fun updateOutputSlot(setChanged: Boolean = true) {
            val item = targetBagType.asItem()
            val cap = ItemStack(item, 1).maxStackSize
            stacks[OUTPUT_SLOT] = ItemStack(item, min(targetBagAmount, cap))
            if (setChanged) {
                setChangedAndUpdateBlock()
            }
        }
    }

    companion object {
        const val INPUT_SLOT = 0
        const val OUTPUT_SLOT = 1
        const val SLOTS_COUNT = OUTPUT_SLOT + 1
    }

    val itemHandler: ItemStackHandler = LootRecyclerItemHandler()
    val inputItemHandler = InputOnlyItemHandler(itemHandler, INPUT_SLOT)
    val outputItemHandler = OutputOnlyItemHandler(itemHandler, OUTPUT_SLOT)

    private val lazyInputCap: LazyOptional<net.minecraftforge.items.IItemHandler> =
        LazyOptional.of { inputItemHandler }
    private val lazyOutputCap: LazyOptional<net.minecraftforge.items.IItemHandler> =
        LazyOptional.of { outputItemHandler }

    var storedBagAmount: Int = 0
    /**
     * Currently `targetBagType` field is constant (no option to change it yet).
     */
    private val targetBagType: LootBagType = LootBagType.COMMON
    private val targetBagAmount: Int
        get() = (storedBagAmount.toFloat() * LootBagType.COMMON.amountFactorEquivalentTo(targetBagType)).toInt()
    private val data = object : ContainerData {
        override fun get(index: Int): Int = when (index) {
            ContainerDataType.STORED_BAG_AMOUNT.ordinal -> storedBagAmount
            else -> 0
        }

        override fun set(index: Int, value: Int) {
            when (index) {
                ContainerDataType.STORED_BAG_AMOUNT.ordinal -> storedBagAmount = value
            }
        }

        override fun getCount(): Int = ContainerDataType.entries.size
    }

    private var accumulation = 0.0

    override fun getDisplayName(): Component = Component.translatable("block.lootbags.loot_recycler")

    override fun createMenu(containerId: Int, playerInventory: Inventory, player: Player): AbstractContainerMenu =
        LootRecyclerMenu(containerId, playerInventory, player.level(), this, data)

    override fun saveAdditional(tag: CompoundTag) {
        tag.put("inventory", itemHandler.serializeNBT())
        tag.putInt("stored_bag_amount", storedBagAmount)

        super.saveAdditional(tag)
    }

    override fun load(tag: CompoundTag) {
        super.load(tag)
        itemHandler.deserializeNBT(tag.getCompound("inventory"))
        storedBagAmount = tag.getInt("stored_bag_amount")
    }

    fun tick(level: Level, pos: BlockPos, state: BlockState) {
        val inputStack = itemHandler.getStackInSlot(INPUT_SLOT).copy()
        if (!inputStack.isEmpty) {
            // If input is detected, consume (recycle) it and increase storedBagAmount accordingly.
            itemHandler.extractItem(INPUT_SLOT, inputStack.count, false)

            // Increase `accumulation` randomly.
            val random = level.random
            val rand = Mth.nextDouble(random, 0.0, 1.0)
            if (rand < 0.5) {
                val rand1 = Mth.nextDouble(random, 0.0, 0.1)
                accumulation += rand1 * inputStack.count
            }

            // If `accumulation` reached 1.0, we can increase `storedBagAmount` accordingly.
            if (accumulation >= 1.0) {
                val increment = accumulation.toInt()
                storedBagAmount += increment
                accumulation -= increment.toDouble()
            }
        }

        (itemHandler as LootRecyclerItemHandler).updateOutputSlot()
    }

    override fun getUpdatePacket(): Packet<ClientGamePacketListener> =
        ClientboundBlockEntityDataPacket.create(this)

    override fun getUpdateTag(): CompoundTag = saveWithoutMetadata()

    private fun setChangedAndUpdateBlock() {
        setChangedAndUpdateBlock(level)
    }

    override fun <T> getCapability(cap: Capability<T>, side: Direction?): LazyOptional<T> {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return if (side == Direction.DOWN) {
                lazyOutputCap.cast()
            } else {
                lazyInputCap.cast()
            }
        }
        return super.getCapability(cap, side)
    }

    override fun invalidateCaps() {
        super.invalidateCaps()
        lazyInputCap.invalidate()
        lazyOutputCap.invalidate()
    }
}