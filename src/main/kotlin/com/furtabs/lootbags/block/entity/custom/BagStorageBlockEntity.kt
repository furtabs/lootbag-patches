package com.furtabs.lootbags.block.entity.custom

import com.furtabs.lootbags.block.entity.ModBlockEntities
import com.furtabs.lootbags.item.custom.LootBagItem
import com.furtabs.lootbags.screen.custom.BagStorageMenu
import com.furtabs.lootbags.util.InputOnlyItemHandler
import com.furtabs.lootbags.util.LootBagType
import com.furtabs.lootbags.util.OutputOnlyItemHandler
import com.furtabs.lootbags.util.asLootBagType
import com.furtabs.lootbags.util.setChangedAndUpdateBlock
import kotlin.math.min
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.world.MenuProvider
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.items.ItemStackHandler

class BagStorageBlockEntity(
    pos: BlockPos,
    blockState: BlockState
) : BlockEntity(ModBlockEntities.BAG_STORAGE.get(), pos, blockState), MenuProvider {

    enum class ContainerDataType {
        STORED_BAG_AMOUNT,
        TARGET_BAG_TYPE
    }

    private inner class BagStorageItemHandler : LootBagItemHandler(SLOTS_COUNT) {
        override fun isInputSlot(slot: Int): Boolean = slot == INPUT_SLOT

        /**
         * Logic to reject "dirty" bags from entering storage.
         */
        override fun isItemValid(slot: Int, stack: ItemStack): Boolean {
            if (!isInputSlot(slot)) return false
            if (stack.item !is LootBagItem) return false

            return true
        }

        override fun extractItem(slot: Int, amount: Int, simulate: Boolean): ItemStack {
            if (simulate) {
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

    val itemHandler: ItemStackHandler = BagStorageItemHandler()
    val inputItemHandler = InputOnlyItemHandler(itemHandler, INPUT_SLOT)
    val outputItemHandler = OutputOnlyItemHandler(itemHandler, OUTPUT_SLOT)

    var storedBagAmount: Int = 0
    private var targetBagType: LootBagType = LootBagType.COMMON
    private val targetBagAmount: Int
        get() = (storedBagAmount.toFloat() * LootBagType.COMMON.amountFactorEquivalentTo(targetBagType)).toInt()

    private val data = object : ContainerData {
        override fun get(index: Int): Int = when (index) {
            ContainerDataType.STORED_BAG_AMOUNT.ordinal -> storedBagAmount
            ContainerDataType.TARGET_BAG_TYPE.ordinal -> targetBagType.ordinal
            else -> 0
        }

        override fun set(index: Int, value: Int) {
            when (index) {
                ContainerDataType.STORED_BAG_AMOUNT.ordinal -> storedBagAmount = value
                ContainerDataType.TARGET_BAG_TYPE.ordinal -> {
                    targetBagType = LootBagType.entries[value]
                }
            }
        }

        override fun getCount(): Int = ContainerDataType.entries.size
    }

    override fun getDisplayName(): Component = Component.translatable("block.lootbags.bag_storage")

    override fun createMenu(containerId: Int, playerInventory: Inventory, player: Player): AbstractContainerMenu =
        BagStorageMenu(containerId, playerInventory, player.level(), this, data)

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        tag.put("inventory", itemHandler.serializeNBT(registries))
        tag.putInt("stored_bag_amount", storedBagAmount)
        tag.putInt("target_bag_type", targetBagType.ordinal)
        super.saveAdditional(tag, registries)
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        itemHandler.deserializeNBT(registries, tag.getCompound("inventory"))
        storedBagAmount = tag.getInt("stored_bag_amount")
        val targetBagTypeOrdinal = tag.getInt("target_bag_type")
        if (targetBagTypeOrdinal in LootBagType.entries.indices) {
            targetBagType = LootBagType.entries[targetBagTypeOrdinal]
        }
    }

    /**
     * Updated tick to validate bags before adding them to stored volume.
     */
    fun tick(level: Level, pos: BlockPos, state: BlockState) {
        val inputStack = itemHandler.getStackInSlot(INPUT_SLOT)
        
        // CHECK: Don't bypass the isItemValid check!
        if (!inputStack.isEmpty && itemHandler.isItemValid(INPUT_SLOT, inputStack)) {
            val inputItem = inputStack.item
            if (inputItem is LootBagItem) {
                val count = inputStack.count
                val bagType = inputItem.asLootBagType()
                
                // Safe to extract now
                itemHandler.extractItem(INPUT_SLOT, count, false)
                
                storedBagAmount += (count.toFloat() * bagType.amountFactorEquivalentTo(LootBagType.COMMON)).toInt()
            }
        }

        (itemHandler as BagStorageItemHandler).updateOutputSlot()
    }

    override fun getUpdatePacket(): Packet<ClientGamePacketListener> =
        ClientboundBlockEntityDataPacket.create(this)

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag = saveWithoutMetadata(registries)

    private fun setChangedAndUpdateBlock() {
        setChangedAndUpdateBlock(level)
    }

}