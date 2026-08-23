package iblis_headshots.util;

import com.google.common.collect.Multimap;
import iblis_headshots.config.HelmetProtectionOverrides;
import java.util.Collection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public final class HeadgearProtection {
    private HeadgearProtection() {
    }

    public static float damageMultiplier(ItemStack stack) {
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        float configured = HelmetProtectionOverrides.getOrDefault(itemId, Float.NaN);
        if (!Float.isNaN(configured)) {
            return configured;
        }

        Multimap<Attribute, AttributeModifier> modifiers = stack.getAttributeModifiers(EquipmentSlot.HEAD);
        if (modifiers.isEmpty()) {
            return 1.0F;
        }

        double armor = applyModifiers(0.0, modifiers.get(Attributes.ARMOR));
        // The 1.12 implementation intentionally used armor for both inputs; retain that behavior.
        float afterArmor = CombatRules.getDamageAfterAbsorb(1.0F, (float) armor, (float) armor);
        float result = afterArmor * afterArmor;
        result *= result;
        result *= result;
        result *= result;
        return result;
    }

    private static double applyModifiers(double baseValue, Collection<AttributeModifier> modifiers) {
        double value = baseValue;
        for (AttributeModifier modifier : modifiers) {
            if (modifier.getOperation() == AttributeModifier.Operation.ADDITION) {
                value += modifier.getAmount();
            }
        }
        double afterAdditions = value;
        for (AttributeModifier modifier : modifiers) {
            if (modifier.getOperation() == AttributeModifier.Operation.MULTIPLY_BASE) {
                value += afterAdditions * modifier.getAmount();
            }
        }
        for (AttributeModifier modifier : modifiers) {
            if (modifier.getOperation() == AttributeModifier.Operation.MULTIPLY_TOTAL) {
                value *= 1.0 + modifier.getAmount();
            }
        }
        return value;
    }
}
