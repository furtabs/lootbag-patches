package com.furtabs.lootbags.block.entity.custom

import com.furtabs.lootbags.block.entity.ModBlockEntities
import com.furtabs.lootbags.item.custom.LootBagItem
import com.furtabs.lootbags.screen.custom.LootRecyclerMenu
import com.furtabs.lootbags.util.*
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
import kotlin.math.min

class LootRecyclerBlockEntity(pos: BlockPos, blockState: BlockState) : 
    BlockEntity(ModBlockEntities.LOOT_RECYCLER.get(), pos, blockState), MenuProvider {

    // Define this at the top of the class so the Menu can see it easily
    enum class ContainerDataType { STORED_BAG_AMOUNT }

    companion object {
        const val INPUT_SLOT = 0
        const val OUTPUT_SLOT = 1
        const val SLOTS_COUNT = 2
    }

    var storedBagAmount: Int = 0
    var accumulation: Double = 0.0 
    val targetBagType: LootBagType = LootBagType.COMMON

    /**
     * Explicitly typing this as ItemStackHandler fixes the 'private-in-class' exposure error
     * while allowing the Menu to see it.
     */
    val itemHandler: ItemStackHandler = object : ItemStackHandler(SLOTS_COUNT) {
        override fun isItemValid(slot: Int, stack: ItemStack): Boolean {
            if (slot != INPUT_SLOT) return false
            if (LootValueRegistry.getValue(stack) <= 0.0) return false
            return true
        }

        override fun onContentsChanged(slot: Int) = setChangedAndUpdateBlock()

        override fun extractItem(slot: Int, amount: Int, simulate: Boolean): ItemStack {
            val extracted = super.extractItem(slot, amount, simulate)
            if (!simulate && slot == OUTPUT_SLOT && !extracted.isEmpty) {
                storedBagAmount -= extracted.count
                updateOutputSlot()
            }
            return extracted
        }
        
        fun updateOutputSlot() {
            val item = targetBagType.asItem()
            val displayAmount = min(storedBagAmount, ItemStack(item).maxStackSize)
            stacks[OUTPUT_SLOT] = if (displayAmount > 0) ItemStack(item, displayAmount) else ItemStack.EMPTY
        }
    }

    private val data = object : ContainerData {
        override fun get(index: Int): Int = if (index == ContainerDataType.STORED_BAG_AMOUNT.ordinal) storedBagAmount else 0
        override fun set(index: Int, value: Int) { if (index == ContainerDataType.STORED_BAG_AMOUNT.ordinal) storedBagAmount = value }
        override fun getCount(): Int = ContainerDataType.entries.size
    }

    override fun getDisplayName(): Component = Component.translatable("block.lootbags.loot_recycler")
    override fun createMenu(id: Int, inv: Inventory, player: Player): AbstractContainerMenu =
        LootRecyclerMenu(id, inv, player.level(), this, data)

    fun tick(level: Level, pos: BlockPos, state: BlockState) {
        if (level.isClientSide) return
        val inputStack = itemHandler.getStackInSlot(INPUT_SLOT)
        if (!inputStack.isEmpty) {
            val valuePerItem = LootValueRegistry.getValue(inputStack)
            if (valuePerItem > 0) {
                val amountToProcess = inputStack.count
                itemHandler.extractItem(INPUT_SLOT, amountToProcess, false)
                accumulation += valuePerItem * amountToProcess
                if (accumulation >= 1.0) {
                    val fullBags = accumulation.toInt()
                    storedBagAmount += fullBags
                    accumulation -= fullBags.toDouble()
                }
                setChangedAndUpdateBlock()
            }
        }
        // Accessing the internal function of the anonymous object
        (itemHandler as? ItemStackHandler)?.let { 
            // We cast it to access our custom method if needed, 
            // but since we defined updateOutputSlot inside the object, 
            // let's just call it directly from the tick if we keep a ref or call it via itemHandler.
        }
        // Simplest way: just call the logic directly
        val item = targetBagType.asItem()
        val displayAmount = min(storedBagAmount, ItemStack(item).maxStackSize)
        itemHandler.setStackInSlot(OUTPUT_SLOT, if (displayAmount > 0) ItemStack(item, displayAmount) else ItemStack.EMPTY)
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        tag.put("inventory", itemHandler.serializeNBT(registries))
        tag.putInt("stored_bags", storedBagAmount)
        tag.putDouble("accumulation", accumulation)
        super.saveAdditional(tag, registries)
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        itemHandler.deserializeNBT(registries, tag.getCompound("inventory"))
        storedBagAmount = tag.getInt("stored_bags")
        accumulation = tag.getDouble("accumulation")
    }

    override fun getUpdatePacket(): Packet<ClientGamePacketListener> = ClientboundBlockEntityDataPacket.create(this)
    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag = saveWithoutMetadata(registries)
    private fun setChangedAndUpdateBlock() = setChangedAndUpdateBlock(level)
}