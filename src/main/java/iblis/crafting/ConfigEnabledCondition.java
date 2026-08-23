package iblis.crafting;

import com.google.gson.JsonObject;
import iblis.IblisMod;
import iblis.config.IblisConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.IConditionSerializer;

public record ConfigEnabledCondition(String option) implements ICondition {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(IblisMod.MOD_ID, "config_enabled");

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public boolean test(IContext context) {
        return switch (option) {
            case "medkit" -> !IblisConfig.disableMedkitRecipe;
            case "shotgun" -> !IblisConfig.disableShotgunRecipe;
            default -> false;
        };
    }

    public static final class Serializer implements IConditionSerializer<ConfigEnabledCondition> {
        public static final Serializer INSTANCE = new Serializer();

        private Serializer() {
        }

        @Override
        public void write(JsonObject json, ConfigEnabledCondition value) {
            json.addProperty("option", value.option);
        }

        @Override
        public ConfigEnabledCondition read(JsonObject json) {
            return new ConfigEnabledCondition(GsonHelper.getAsString(json, "option"));
        }

        @Override
        public ResourceLocation getID() {
            return ID;
        }
    }
}
