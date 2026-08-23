package iblis.config;

import java.util.List;
import net.minecraftforge.common.ForgeConfigSpec;

public record EditableConfigCategory(
        String titleKey,
        ForgeConfigSpec spec,
        List<EditableConfigValue> values) {
}
