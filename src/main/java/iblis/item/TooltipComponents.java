package iblis.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public final class TooltipComponents {
    private static final String[] QUALITY_LEVELS = {
            "worthless", "trash", "miserable", "awful", "bad", "normal",
            "good", "excellent", "marvelous", "exceptional", "perfect"
    };

    private TooltipComponents() {
    }

    public static void addQuality(List<Component> tooltip, int rawQuality) {
        int quality = Math.max(-5, Math.min(rawQuality, 5));
        tooltip.add(Component.translatable("iblis.quality",
                        Component.translatable("iblis.qualityLevel."
                                + QUALITY_LEVELS[quality + 5]), rawQuality)
                .withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
