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

// The constructor now matches the 3 arguments being passed from your Plugin class
class LootBagRecipeCategory(
    guiHelper: IGuiHelper, 
    private val bagType: LootBagType, 
    private val recipeType: RecipeType<LootBagRecipe>
) : IRecipeCategory<LootBagRecipe> {

    companion object {
        // We REMOVED 'val TYPE' from here because each bag now has its own unique type
        private const val RECIPE_WIDTH = 160
        private const val RECIPE_HEIGHT = 125
        private const val COLUMNS = 6
        private const val OUTPUT_START_X = 3
        private const val OUTPUT_START_Y = 30
        private const val SLOT_SPACING_X = 26
        private const val SLOT_SPACING_Y = 20
    }

    // Dynamic title (e.g., "Common Loot Bag")
    private val title: Component = Component.literal("${bagType.name.lowercase().replaceFirstChar { it.uppercase() }} Loot Bag")
    
    // Dynamic icon (Uses the specific bag item for this tab)
    private val icon: IDrawable = guiHelper.createDrawableItemStack(ItemStack(bagType.asItem()))
    private val background: IDrawable = guiHelper.createBlankDrawable(RECIPE_WIDTH, RECIPE_HEIGHT)

    override fun getRecipeType(): RecipeType<LootBagRecipe> = recipeType

    override fun getTitle(): Component = title

    override fun getIcon(): IDrawable = icon

    override fun getWidth(): Int = RECIPE_WIDTH

    override fun getHeight(): Int = RECIPE_HEIGHT

    override fun getBackground(): IDrawable = background

    override fun setRecipe(builder: IRecipeLayoutBuilder, recipe: LootBagRecipe, focuses: IFocusGroup) {
        // Input slot (top center) - the loot bag
        builder.addSlot(RecipeIngredientRole.INPUT, 72, 5).addItemStack(recipe.bag)

        // Output slots
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
        // Draw page text at the bottom
        guiGraphics.drawString(mc.font, pageText, 5, 115, 0x404040, false)
    }
}