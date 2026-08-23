package iblis.compat.jei;

import iblis.registry.IblisItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class SteelProcessingCategory implements IRecipeCategory<SteelProcessingRecipe> {
    private static final int WIDTH = 150;
    private static final int HEIGHT = 58;
    private final IDrawable icon;

    public SteelProcessingCategory(IGuiHelper guiHelper) {
        icon = guiHelper.createDrawableItemStack(IblisItems.INGOT_STEEL.get().getDefaultInstance());
    }

    @Override
    public mezz.jei.api.recipe.RecipeType<SteelProcessingRecipe> getRecipeType() {
        return IblisJeiPlugin.STEEL_PROCESSING;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("iblis.jei.steel_processing");
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
    public void setRecipe(IRecipeLayoutBuilder builder, SteelProcessingRecipe recipe,
                          IFocusGroup focuses) {
        int inputX = recipe.catalysts().isEmpty() ? 28 : 8;
        builder.addInputSlot(inputX, 7)
                .setStandardSlotBackground()
                .addItemStacks(recipe.inputs());

        if (!recipe.catalysts().isEmpty()) {
            builder.addSlot(RecipeIngredientRole.CATALYST, 46, 7)
                    .setStandardSlotBackground()
                    .addItemStacks(recipe.catalysts());
        }

        builder.addOutputSlot(124, 7)
                .setOutputSlotBackground()
                .addItemStack(recipe.output());
    }

    @Override
    public void draw(SteelProcessingRecipe recipe, IRecipeSlotsView recipeSlotsView,
                     GuiGraphics graphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        if (!recipe.catalysts().isEmpty()) {
            graphics.drawString(font, "+", 33, 12, 0xFF808080, false);
            graphics.drawString(font, "→", 91, 12, 0xFF808080, false);
        } else {
            graphics.drawString(font, "→", 82, 12, 0xFF808080, false);
        }

        int y = 33;
        for (String key : recipe.instructionKeys()) {
            Component line = Component.translatable(key);
            int x = Math.max(0, (WIDTH - font.width(line)) / 2);
            graphics.drawString(font, line, x, y, 0xFF606060, false);
            y += 10;
        }
    }

    @Override
    public ResourceLocation getRegistryName(SteelProcessingRecipe recipe) {
        return recipe.id();
    }
}
