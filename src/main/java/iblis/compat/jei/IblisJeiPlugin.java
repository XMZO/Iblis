package iblis.compat.jei;

import iblis.IblisMod;
import iblis.registry.IblisItems;
import java.util.Comparator;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Blocks;

@JeiPlugin
public final class IblisJeiPlugin implements IModPlugin {
    public static final RecipeType<CraftingRecipe> IBLIS_CRAFTING = RecipeType.create(
            IblisMod.MOD_ID, "crafting", CraftingRecipe.class);
    public static final RecipeType<SteelProcessingRecipe> STEEL_PROCESSING = RecipeType.create(
            IblisMod.MOD_ID, "steel_processing", SteelProcessingRecipe.class);
    private static final ResourceLocation PLUGIN_ID =
            new ResourceLocation(IblisMod.MOD_ID, "jei");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new IblisCraftingCategory(guiHelper),
                new SteelProcessingCategory(guiHelper));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level != null) {
            List<CraftingRecipe> craftingRecipes = level.getRecipeManager()
                    .getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING)
                    .stream()
                    .filter(recipe -> IblisMod.MOD_ID.equals(recipe.getId().getNamespace()))
                    .filter(recipe -> recipe.getResultItem(level.registryAccess()).hasTag())
                    .sorted(Comparator.comparing(recipe -> recipe.getId().toString()))
                    .toList();
            registration.addRecipes(IBLIS_CRAFTING, craftingRecipes);
        }

        SteelProcessingRecipe firing = new SteelProcessingRecipe(
                new ResourceLocation(IblisMod.MOD_ID, "jei/steel_firing"),
                List.of(
                        new ItemStack(IblisItems.IRON_COAL.get()),
                        new ItemStack(IblisItems.IRONORE_COAL.get())),
                List.of(new ItemStack(Items.FLINT_AND_STEEL), new ItemStack(Items.FIRE_CHARGE)),
                new ItemStack(IblisItems.SLAG.get()),
                List.of("iblis.jei.steel.fire"));
        SteelProcessingRecipe breaking = new SteelProcessingRecipe(
                new ResourceLocation(IblisMod.MOD_ID, "jei/slag_breaking"),
                List.of(new ItemStack(IblisItems.SLAG.get())),
                List.of(),
                new ItemStack(IblisItems.INGOT_STEEL.get()),
                List.of("iblis.jei.steel.break", "iblis.jei.steel.fortune"));
        registration.addRecipes(STEEL_PROCESSING, List.of(firing, breaking));

        registration.addItemStackInfo(List.of(
                        new ItemStack(IblisItems.IRON_COAL.get()),
                        new ItemStack(IblisItems.IRONORE_COAL.get())),
                Component.translatable("iblis.jei.info.steel_mixture"));
        registration.addItemStackInfo(new ItemStack(IblisItems.INGOT_BRONZE.get()),
                Component.translatable("iblis.jei.info.bronze"));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(Blocks.CRAFTING_TABLE), IBLIS_CRAFTING);
        registration.addRecipeCatalyst(new ItemStack(Items.FLINT_AND_STEEL), STEEL_PROCESSING);
        registration.addRecipeCatalyst(new ItemStack(Items.FIRE_CHARGE), STEEL_PROCESSING);
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(
                CraftingMenu.class, MenuType.CRAFTING, IBLIS_CRAFTING,
                1, 9, 10, 36);
    }
}
