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
import net.minecraft.core.HolderLookup
import net.neoforged.neoforge.items.ItemStackHandler
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
        override fun isItemValid(slot: Int, stack: ItemStack): Boolean = false
        override fun insertItem(slot: Int, stack: ItemStack, simulate: Boolean): ItemStack = stack.copy()

        override fun onContentsChanged(slot: Int) {
            setChangedAndUpdateBlock()
        }
    }

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

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        tag.put("input_inventory", inputItemHandler.serializeNBT(registries))
        tag.put("output_inventory", outputItemHandler.serializeNBT(registries))
        tag.putInt("progress", progress)
        tag.putInt("max_progress", maxProgress)
        super.saveAdditional(tag, registries)
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        inputItemHandler.deserializeNBT(registries, tag.getCompound("input_inventory"))
        outputItemHandler.deserializeNBT(registries, tag.getCompound("output_inventory"))
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
            var allOccupied = true
            for (i in 0 ..< outputItemHandler.slots) {
                allOccupied = allOccupied && !outputItemHandler.getStackInSlot(i).isEmpty
            }
            if (allOccupied) return null

            for (i in 0 ..< inputItemHandler.slots) {
                if (!inputItemHandler.getStackInSlot(i).isEmpty) return i
            }
            return null
        }

    private fun increaseCraftingProgress() {
        if (progress < maxProgress) progress++
    }

    private fun decreaseCraftingProgress() {
        if (progress > 0) progress--
    }

    private val hasCraftingFinished: Boolean
        get() = progress >= maxProgress

    private fun craftItem(inputSlot: Int) {
        if (inputSlot < 0 || inputSlot >= inputItemHandler.slots) return

        val inputItemStack = inputItemHandler.getStackInSlot(inputSlot)
        if (inputItemStack.isEmpty) return

        val bagItem = inputItemStack.item as? LootBagItem ?: return
        val level = this.level ?: return
        val serverLevel = level as? ServerLevel ?: return

        val loots: List<ItemStack> = bagItem.asLootBagType().lootGenerator.generateLoot(
            serverLevel,
            LootParams.Builder(serverLevel)
                .withParameter(LootContextParams.BLOCK_STATE, blockState)
                .withParameter(LootContextParams.BLOCK_ENTITY, this)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(blockPos)),
            maxStacks = 5
        )

        if (loots.isNotEmpty()) {
            val extracted = inputItemHandler.extractItem(inputSlot, 1, false)
            if (!extracted.isEmpty) {
                for (loot in loots) {
                    if (!putIntoOutputSlots(loot)) {
                        // Optional: If output is full, drop the remaining items on the ground
                        Containers.dropItemStack(level, worldPosition.x.toDouble(), worldPosition.y.toDouble(), worldPosition.z.toDouble(), loot)
                    }
                }
            }
        }
    }

    private fun getContentsFromNBT(stack: ItemStack): List<ItemStack> {
        return emptyList()
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
            if (ItemStack.isSameItemSameComponents(remainingStack, currentStack)) {
                val maxCapacity = currentStack.maxStackSize
                val amountToPutInto = maxCapacity - currentStack.count
                val remainingAfterPut = remainingStack.count - amountToPutInto
                if (remainingAfterPut < 0) {
                    outputItemHandler.setStackInSlot(i, currentStack.copy().also { it.count += remainingStack.count })
                    remainingStack.count = 0
                } else {
                    outputItemHandler.setStackInSlot(i, currentStack.copy().also { it.count = maxCapacity })
                    remainingStack.count = remainingAfterPut
                }
            }
            if (remainingStack.isEmpty) break
        }
        return remainingStack.isEmpty
    }

    private fun resetProgress() {
        progress = 0
        maxProgress = 60
    }

    override fun getUpdatePacket(): Packet<ClientGamePacketListener> =
        ClientboundBlockEntityDataPacket.create(this)

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag = saveWithoutMetadata(registries)

    private fun setChangedAndUpdateBlock() {
        setChangedAndUpdateBlock(level)
    }

}