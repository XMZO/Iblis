package iblis_headshots.client;

import iblis_headshots.IblisHeadshotsMod;
import iblis_headshots.util.HeadgearProtection;
import iblis_headshots.util.HeadshotRules;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = IblisHeadshotsMod.MOD_ID, value = Dist.CLIENT)
public final class HeadshotsClientEvents {
    private static final String[] PROTECTION_LEVELS = {
            "no", "weak", "miserable", "awful", "bad", "normal",
            "good", "excellent", "marvelous", "exceptional", "perfect"
    };

    private HeadshotsClientEvents() {
    }

    @SubscribeEvent
    public static void itemTooltip(ItemTooltipEvent event) {
        if (!HeadshotRules.allowsHeadshots(EntityType.PLAYER)) {
            return;
        }
        float damageMultiplier = HeadgearProtection.damageMultiplier(event.getItemStack());
        int absorption = Mth.ceil((1.0F - damageMultiplier) * 100.0F);
        if (damageMultiplier == 1.0F) {
            return;
        }

        event.getToolTip().add(Component.translatable("iblis.headshot_protection", absorption)
                .append("%")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        int level = Mth.clamp(absorption / 10, 0, PROTECTION_LEVELS.length - 1);
        event.getToolTip().add(Component.translatable(
                        "iblis.protectionLevel." + PROTECTION_LEVELS[level])
                .withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
