package iblis.item;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class AmmoItem extends Item {
    public static final String LEGACY_QUALITY = "iblisQuality";

    private final float baseDamage;
    private final int ammoType;

    public AmmoItem(Properties properties, float baseDamage, int ammoType) {
        super(properties);
        this.baseDamage = baseDamage;
        this.ammoType = ammoType;
    }

    public float getAmmoDamage(ItemStack stack) {
        float multiplier = getLegacyQuality(stack) * 0.2F + 1.0F;
        return baseDamage * multiplier * multiplier;
    }

    public int getAmmoType(ItemStack stack) {
        return ammoType;
    }

    public int getQuality(ItemStack stack) {
        return getLegacyQuality(stack) - 5;
    }

    public static int getLegacyQuality(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getInt(LEGACY_QUALITY) : 0;
    }

    public static void setLegacyQuality(ItemStack stack, int quality) {
        stack.getOrCreateTag().putInt(LEGACY_QUALITY, quality);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.translatable("iblis.ammo_damage", getAmmoDamage(stack)));
        TooltipComponents.addQuality(tooltip, getQuality(stack));
    }
}
