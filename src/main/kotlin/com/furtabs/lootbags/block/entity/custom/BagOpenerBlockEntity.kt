package com.furtabs.lootbags.block.entity.custom

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.Containers
import net.minecraft.world.MenuProvider
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.storage.loot.LootParams
import net.minecraft.world.level.storage.loot.parameters.LootContextParams
import net.minecraft.world.phys.Vec3
import net.minecraft.core.Direction
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.ForgeCapabilities
import net.minecraftforge.common.util.LazyOptional
import net.minecraftforge.items.ItemStackHandler
import com.furtabs.lootbags.block.entity.ModBlockEntities
import com.furtabs.lootbags.item.ModItems
import com.furtabs.lootbags.item.custom.LootBagItem
import com.furtabs.lootbags.screen.custom.BagOpenerMenu
import com.furtabs.lootbags.util.LootBagType
import com.furtabs.lootbags.util.asLootBagType
import com.furtabs.lootbags.util.rollLootBagDisplayedItemCount
import com.furtabs.lootbags.util.setChangedAndUpdateBlock

class BagOpenerBlockEntity(
    pos: BlockPos,
    blockState: BlockState
) : BlockEntity(ModBlockEntities.BAG_OPENER.get(), pos, blockState), MenuProvider {
    enum class ContainerDataType {
        PROGRESS,
        MAX_PROGRESS
    }

    private inner class BagOpenerItemHandler : LootBagItemHandler(INPUT_SLOTS_COUNT) {
        override fun isInputSlot(slot: Int): Boolean = true

        override fun onContentsChanged(slot: Int) {
            setChangedAndUpdateBlock()
        }
    }

    companion object {
        const val INPUT_SLOTS_COUNT = 9
        const val OUTPUT_SLOTS_COUNT = 9 * 3
        const val SLOTS_COUNT = INPUT_SLOTS_COUNT + OUTPUT_SLOTS_COUNT
    }

    val inputItemHandler: ItemStackHandler = BagOpenerItemHandler()
    val outputItemHandler: ItemStackHandler = object : ItemStackHandler(OUTPUT_SLOTS_COUNT) {
        // Refuse to insert any items because the slots are used for output purpose only.
        override fun isItemValid(slot: Int, stack: ItemStack): Boolean = false

        // Refuse to insert any items because the slots are used for output purpose only.
        override fun insertItem(slot: Int, stack: ItemStack, simulate: Boolean): ItemStack = stack.copy()

        override fun onContentsChanged(slot: Int) {
            setChangedAndUpdateBlock()
        }
    }

    private val lazyInputCap: LazyOptional<net.minecraftforge.items.IItemHandler> =
        LazyOptional.of { inputItemHandler }
    private val lazyOutputCap: LazyOptional<net.minecraftforge.items.IItemHandler> =
        LazyOptional.of { outputItemHandler }

    private var progress = 0
    private var maxProgress = 60
    private val data = object : ContainerData {
        override fun get(index: Int): Int = when (index) {
            ContainerDataType.PROGRESS.ordinal -> progress
            ContainerDataType.MAX_PROGRESS.ordinal -> maxProgress
            else -> 0
        }

        override fun set(index: Int, value: Int) {
            when (index) {
                ContainerDataType.PROGRESS.ordinal -> progress = value
                ContainerDataType.MAX_PROGRESS.ordinal -> maxProgress = value
            }
        }

        override fun getCount(): Int = ContainerDataType.entries.size
    }

    override fun getDisplayName(): Component = Component.translatable("block.lootbags.bag_opener")

    override fun createMenu(containerId: Int, playerInventory: Inventory, player: Player): AbstractContainerMenu =
        BagOpenerMenu(containerId, playerInventory, player.level(), this, data)

    fun drops() {
        level?.let { level ->
            for (handler in listOf(inputItemHandler, outputItemHandler)) {
                val inventory = SimpleContainer(handler.slots)
                for (i in 0 ..< handler.slots) {
                    inventory.setItem(i, handler.getStackInSlot(i))
                }
                Containers.dropContents(level, worldPosition, inventory)
            }
        }
    }

    override fun saveAdditional(tag: CompoundTag) {
        tag.put("input_inventory", inputItemHandler.serializeNBT())
        tag.put("output_inventory", outputItemHandler.serializeNBT())
        tag.putInt("progress", progress)
        tag.putInt("max_progress", maxProgress)

        super.saveAdditional(tag)
    }

    override fun load(tag: CompoundTag) {
        super.load(tag)
        inputItemHandler.deserializeNBT(tag.getCompound("input_inventory"))
        outputItemHandler.deserializeNBT(tag.getCompound("output_inventory"))
        progress = tag.getInt("progress")
        maxProgress = tag.getInt("max_progress")
    }

    fun tick(level: Level, pos: BlockPos, state: BlockState) {
        val inputSlot = availableInputSlotForCrafting
        if (inputSlot == null) {
            decreaseCraftingProgress()
        } else {
            increaseCraftingProgress()
            setChanged(level, pos, state)

            if (hasCraftingFinished) {
                craftItem(inputSlot)
                resetProgress()
            }
        }
    }

    private val availableInputSlotForCrafting: Int?
        get() {
            // If all the output slots are occupied, the crafting is not available.
            var allOccupied = true
            for (i in 0 ..< outputItemHandler.slots) {
                allOccupied = allOccupied && !outputItemHandler.getStackInSlot(i).isEmpty
            }
            if (allOccupied) {
                return null
            }

            // Then check whether there is any input item in the input slots.
            for (i in 0 ..< inputItemHandler.slots) {
                if (!inputItemHandler.getStackInSlot(i).isEmpty) {
                    return i
                }
            }

            // If no input item is found, the crafting is not available.
            return null
        }

    private fun increaseCraftingProgress() {
        if (progress < maxProgress) {
            progress++
        }
    }

    private fun decreaseCraftingProgress() {
        if (progress > 0) {
            progress--
        }
    }

    private val hasCraftingFinished: Boolean
        get() = progress >= maxProgress

    private fun craftItem(inputSlot: Int) {
        // Ensure the input slot index is valid.
        if (inputSlot < 0 || inputSlot >= inputItemHandler.slots) {
            return
        }

        // Get the input item and check whether it is valid for crafting.
        val inputItemStack = inputItemHandler.getStackInSlot(inputSlot).copy()
        if (inputItemStack.isEmpty) {
            return
        }
        var isValid = false
        for (bag in ModItems.LOOT_BAGS) {
            if (inputItemStack.item == bag.get()) {
                isValid = true
            }
        }
        if (!isValid) {
            return
        }
        val bagItem = inputItemStack.item as? LootBagItem ?: return
        val level = this.level ?: return
        val serverLevel = level as? ServerLevel ?: return

        // Consume one item from the input slot and put the output loot item into the output slots.
        val extracted = inputItemHandler.extractItem(inputSlot, 1, false)
        if (extracted.isEmpty) {
            return
        }
        val pos = Vec3(blockPos.x.toDouble() + 0.5, blockPos.y.toDouble() + 0.5, blockPos.z.toDouble() + 0.5)
        val bagType = bagItem.asLootBagType()
        val maxStacks = rollLootBagDisplayedItemCount(serverLevel.random)
        val loots = bagType.lootGenerator.generateLoot(
            serverLevel,
            LootParams.Builder(serverLevel)
                .withParameter(LootContextParams.BLOCK_STATE, blockState)
                .withParameter(LootContextParams.BLOCK_ENTITY, this)
                .withParameter(LootContextParams.ORIGIN, pos),
            maxStacks = maxStacks
        )
        for (loot in loots) {
            if (!putIntoOutputSlots(loot)) {
                break
            }
        }
    }

    private fun putIntoOutputSlots(stack: ItemStack): Boolean {
        val remainingStack = stack.copy()
        for (i in 0 ..< outputItemHandler.slots) {
            val currentStack = outputItemHandler.getStackInSlot(i)
            if (currentStack.isEmpty) {
                outputItemHandler.setStackInSlot(i, remainingStack.copy())
                remainingStack.count = 0
                break
            }
            if (ItemStack.isSameItemSameTags(remainingStack, currentStack)) {
                val maxCapacity = currentStack.maxStackSize
                val amountToPutInto = maxCapacity - currentStack.count
                val remainingAfterPut = remainingStack.count - amountToPutInto
                if (remainingAfterPut < 0) {
                    // Too few items to be put into the current stack, put the available count and set the
                    // remaining stack to empty.
                    outputItemHandler.setStackInSlot(i, currentStack.copy().also { it.count += remainingStack.count })
                    remainingStack.count = 0
                } else {
                    // Remaining items are enough to be put into the current stack, put the amount to let
                    // the current stack become full.
                    outputItemHandler.setStackInSlot(i, currentStack.copy().also { it.count = maxCapacity })
                    remainingStack.count = remainingAfterPut
                }
            }
            if (remainingStack.isEmpty) {
                // No items left to be put; quit the loop.
                break
            }
        }

        return remainingStack.isEmpty
    }

    private fun resetProgress() {
        progress = 0
        maxProgress = 60 // Reset max progress as well in order to get rid of unexpected situations.
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