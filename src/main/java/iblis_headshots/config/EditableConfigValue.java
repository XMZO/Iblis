package iblis_headshots.config;

import net.minecraftforge.common.ForgeConfigSpec;

public record EditableConfigValue(
        String key,
        ForgeConfigSpec.ConfigValue<?> value,
        Double minimum,
        Double maximum) {

    public static EditableConfigValue booleanValue(
            String key, ForgeConfigSpec.BooleanValue value) {
        return new EditableConfigValue(key, value, null, null);
    }

    public static EditableConfigValue numberValue(
            String key, ForgeConfigSpec.ConfigValue<? extends Number> value,
            double minimum, double maximum) {
        return new EditableConfigValue(key, value, minimum, maximum);
    }

    public boolean isBoolean() {
        return value.getDefault() instanceof Boolean;
    }

    public boolean isInteger() {
        return value.getDefault() instanceof Integer;
    }

    @SuppressWarnings("unchecked")
    public void set(Object newValue) {
        Object defaultValue = value.getDefault();
        Object normalized = newValue;
        if (defaultValue instanceof Number) {
            if (!(newValue instanceof Number number)) {
                throw new IllegalArgumentException("Expected a number for " + key);
            }
            normalized = defaultValue instanceof Integer
                    ? (int) Math.round(number.doubleValue()) : number.doubleValue();
        } else if (defaultValue instanceof Boolean && !(newValue instanceof Boolean)) {
            throw new IllegalArgumentException("Expected a boolean for " + key);
        }
        ((ForgeConfigSpec.ConfigValue<Object>) value).set(normalized);
    }
}
