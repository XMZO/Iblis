package iblis.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** A small, loader-independent description consumed by the client config screens. */
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
                throw new IllegalArgumentException("Expected a numeric value for " + key);
            }
            normalized = normalizeNumber(number, defaultValue);
        } else if (defaultValue instanceof Boolean && !(newValue instanceof Boolean)) {
            throw new IllegalArgumentException("Expected a boolean value for " + key);
        }
        ((ForgeConfigSpec.ConfigValue<Object>) value).set(normalized);
    }

    private static Object normalizeNumber(Number value, Object defaultValue) {
        if (defaultValue instanceof Integer) {
            return (int) Math.round(value.doubleValue());
        }
        if (defaultValue instanceof Long) {
            return Math.round(value.doubleValue());
        }
        if (defaultValue instanceof Float) {
            return value.floatValue();
        }
        if (defaultValue instanceof Double) {
            return value.doubleValue();
        }
        if (defaultValue instanceof Short) {
            return value.shortValue();
        }
        if (defaultValue instanceof Byte) {
            return value.byteValue();
        }
        throw new IllegalArgumentException(
                "Unsupported numeric config type " + defaultValue.getClass().getName());
    }
}
