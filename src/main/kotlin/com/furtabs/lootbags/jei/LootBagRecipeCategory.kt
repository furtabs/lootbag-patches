package com.furtabs.lootbags.jei

import com.furtabs.lootbags.util.LootBagType
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder
import mezz.jei.api.gui.drawable.IDrawable
import mezz.jei.api.gui.ingredient.IRecipeSlotsView
import mezz.jei.api.recipe.IFocusGroup
import mezz.jei.api.recipe.RecipeIngredientRole
import mezz.jei.api.recipe.RecipeType
import mezz.jei.api.recipe.category.IRecipeCategory
import mezz.jei.api.helpers.IGuiHelper
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack

class LootBagRecipeCategory(guiHelper: IGuiHelper) : IRecipeCategory<LootBagRecipe> {
    companion object {
        val TYPE: RecipeType<LootBagRecipe> = RecipeType.create("lootbags", "loot_bag_items", LootBagRecipe::class.java)
        private const val RECIPE_WIDTH = 160
        private const val RECIPE_HEIGHT = 122
        private const val COLUMNS = 6
        private const val OUTPUT_START_X = 2
        private const val OUTPUT_START_Y = 30
        private const val SLOT_SPACING_X = 28
        private const val SLOT_SPACING_Y = 20
    }

    private val title: Component = Component.literal("Loot Bag Drops")
    private val icon: IDrawable = guiHelper.createDrawableItemStack(ItemStack(LootBagType.COMMON.asItem()))
    private val background: IDrawable = guiHelper.createBlankDrawable(RECIPE_WIDTH, RECIPE_HEIGHT)

    override fun getRecipeType(): RecipeType<LootBagRecipe> = TYPE

    override fun getTitle(): Component = title

    override fun getIcon(): IDrawable = icon

    override fun getWidth(): Int = RECIPE_WIDTH

    override fun getHeight(): Int = RECIPE_HEIGHT

    override fun getBackground(): IDrawable = background

    override fun setRecipe(builder: IRecipeLayoutBuilder, recipe: LootBagRecipe, focuses: IFocusGroup) {
        // Input slot (top center) - the loot bag
        builder.addSlot(RecipeIngredientRole.INPUT, 70, 5).addItemStack(recipe.bag)

        // Native JEI output slots so hover/click/recipe lookup works as expected.
        for ((index, stack) in recipe.outputs.withIndex()) {
            val row = index / COLUMNS
            val col = index % COLUMNS
            val x = OUTPUT_START_X + col * SLOT_SPACING_X
            val y = OUTPUT_START_Y + row * SLOT_SPACING_Y
            builder.addSlot(RecipeIngredientRole.OUTPUT, x, y).addItemStack(stack)
        }
    }

    override fun draw(
        recipe: LootBagRecipe,
        recipeSlotsView: IRecipeSlotsView,
        guiGraphics: GuiGraphics,
        mouseX: Double,
        mouseY: Double
    ) {
        val mc = Minecraft.getInstance()
        val pageText = Component.literal("Page ${recipe.pageIndex}/${recipe.totalPages}")
        guiGraphics.drawString(mc.font, pageText, 8, 108, 0x404040, false)
    }
}
