package iblis.compat.jei;

import iblis.registry.IblisItems;
import java.util.Arrays;
import java.util.List;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;

/**
 * A native-looking crafting view for Iblis equipment recipes with quality NBT
 * outputs, which may otherwise be omitted from JEI's vanilla crafting view.
 */
public final class IblisCraftingCategory implements IRecipeCategory<CraftingRecipe> {
    private static final int WIDTH = 116;
    private static final int HEIGHT = 54;

    private final IDrawable icon;
    private final IDrawableStatic recipeArrow;
    private final ICraftingGridHelper craftingGridHelper;

    public IblisCraftingCategory(IGuiHelper guiHelper) {
        icon = guiHelper.createDrawableItemStack(IblisItems.HEAVY_SHIELD.get().getDefaultInstance());
        recipeArrow = guiHelper.getRecipeArrow();
        craftingGridHelper = guiHelper.createCraftingGridHelper();
    }

    @Override
    public mezz.jei.api.recipe.RecipeType<CraftingRecipe> getRecipeType() {
        return IblisJeiPlugin.IBLIS_CRAFTING;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("iblis.jei.iblis_crafting");
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CraftingRecipe recipe,
                          IFocusGroup focuses) {
        int recipeWidth = 0;
        int recipeHeight = 0;
        if (recipe instanceof ShapedRecipe shapedRecipe) {
            recipeWidth = shapedRecipe.getWidth();
            recipeHeight = shapedRecipe.getHeight();
        } else {
            builder.setShapeless();
        }

        List<List<ItemStack>> inputs = recipe.getIngredients().stream()
                .map(Ingredient::getItems)
                .map(Arrays::asList)
                .toList();
        craftingGridHelper.createAndSetInputs(builder, inputs, recipeWidth, recipeHeight);

        var level = Minecraft.getInstance().level;
        if (level != null) {
            ItemStack result = recipe.getResultItem(level.registryAccess()).copy();
            craftingGridHelper.createAndSetOutputs(builder, List.of(result));
        }
    }

    @Override
    public void draw(CraftingRecipe recipe, IRecipeSlotsView recipeSlotsView,
                     GuiGraphics graphics, double mouseX, double mouseY) {
        recipeArrow.draw(graphics, 61, (HEIGHT - recipeArrow.getHeight()) / 2);
    }

    @Override
    public ResourceLocation getRegistryName(CraftingRecipe recipe) {
        return recipe.getId();
    }
}
