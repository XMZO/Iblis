package iblis.crafting;

import com.google.gson.JsonObject;
import iblis.IblisMod;
import iblis.config.IblisConfig;
import iblis.config.Legacy112Feature;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.IConditionSerializer;

public record ConfigEnabledCondition(String option, boolean expected) implements ICondition {
    public static final ResourceLocation ID =
            new ResourceLocation(IblisMod.MOD_ID, "config_enabled");

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public boolean test(IContext context) {
        Boolean enabled = switch (option) {
            case "medkit" -> !IblisConfig.disableMedkitRecipe;
            case "shotgun" -> !IblisConfig.disableShotgunRecipe;
            default -> {
                Legacy112Feature feature = Legacy112Feature.byKey(option);
                yield feature == null ? null : IblisConfig.useLegacy112(feature);
            }
        };
        return enabled != null && enabled == expected;
    }

    public static final class Serializer implements IConditionSerializer<ConfigEnabledCondition> {
        public static final Serializer INSTANCE = new Serializer();

        private Serializer() {
        }

        @Override
        public void write(JsonObject json, ConfigEnabledCondition value) {
            json.addProperty("option", value.option);
            json.addProperty("expected", value.expected);
        }

        @Override
        public ConfigEnabledCondition read(JsonObject json) {
            return new ConfigEnabledCondition(
                    GsonHelper.getAsString(json, "option"),
                    GsonHelper.getAsBoolean(json, "expected", true));
        }

        @Override
        public ResourceLocation getID() {
            return ID;
        }
    }
}
