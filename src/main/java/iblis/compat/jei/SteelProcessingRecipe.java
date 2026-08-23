package iblis.compat.jei;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record SteelProcessingRecipe(ResourceLocation id, List<ItemStack> inputs,
                                    List<ItemStack> catalysts, ItemStack output,
                                    List<String> instructionKeys) {
}
