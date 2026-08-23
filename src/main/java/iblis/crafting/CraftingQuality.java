package iblis.crafting;

import iblis.registry.IblisAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;

public final class CraftingQuality {
    public static final String QUALITY = "quality";
    public static final String DURABILITY = "durability";
    private static final UUID BOW_DAMAGE =
            UUID.fromString("73b1f7c9-7f6f-0857-4cdf-000009f5f2d7");
    private static final UUID DIGGING =
            UUID.fromString("73b1f7c9-7f6f-0857-4cdf-000009f5f2d7");

    private CraftingQuality() {
    }

    public static void apply(ItemStack stack, double skillValue, int baseDurability,
                             boolean additive) {
        List<Modifier> modifiers = collectModifiers(stack);
        if (stack.getItem() instanceof BowItem
                && modifiers.stream().noneMatch(value -> value.attribute == IblisAttributes.PROJECTILE_DAMAGE.get())) {
            modifiers.add(new Modifier(IblisAttributes.PROJECTILE_DAMAGE.get(),
                    new AttributeModifier(BOW_DAMAGE, "Arrow damage", 2.0,
                            AttributeModifier.Operation.ADDITION), EquipmentSlot.MAINHAND));
        }
        if (stack.getItem() instanceof DiggerItem digger
                && modifiers.stream().noneMatch(value -> value.attribute == IblisAttributes.DIGGING.get())) {
            modifiers.add(new Modifier(IblisAttributes.DIGGING.get(),
                    new AttributeModifier(DIGGING, "Digging skill",
                            digger.getTier().getSpeed() * 0.1,
                            AttributeModifier.Operation.ADDITION), EquipmentSlot.MAINHAND));
        }

        stack.getOrCreateTag().putInt(QUALITY, (int) skillValue);
        if (additive) {
            if (stack.getTag().contains(DURABILITY)) {
                stack.getTag().putInt(DURABILITY,
                        modifyInt(stack.getTag().getInt(DURABILITY), skillValue, true));
            }
        } else if (baseDurability > 0) {
            stack.getTag().putInt(DURABILITY, modifyInt(baseDurability, skillValue, false));
        }

        stack.getTag().remove("AttributeModifiers");
        for (Modifier entry : modifiers) {
            double amount = isAffected(entry.attribute)
                    ? modifyDouble(entry.modifier.getAmount(), skillValue, additive)
                    : entry.modifier.getAmount();
            if (amount == 0.0) {
                continue;
            }
            stack.addAttributeModifier(entry.attribute,
                    new AttributeModifier(entry.modifier.getId(), entry.modifier.getName(),
                            amount, entry.modifier.getOperation()), entry.slot);
        }
    }

    private static List<Modifier> collectModifiers(ItemStack stack) {
        List<Modifier> result = new ArrayList<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            stack.getAttributeModifiers(slot).forEach((attribute, modifier) ->
                    result.add(new Modifier(attribute, modifier, slot)));
        }
        return result;
    }

    private static boolean isAffected(Attribute attribute) {
        return AFFECTED.contains(attribute);
    }

    private static final Set<Attribute> AFFECTED = Set.of(
            Attributes.ARMOR,
            Attributes.ARMOR_TOUGHNESS,
            Attributes.ATTACK_DAMAGE,
            IblisAttributes.DIGGING.get(),
            IblisAttributes.PROJECTILE_DAMAGE.get(),
            IblisAttributes.MELEE_DAMAGE_REDUCTION.get(),
            IblisAttributes.PROJECTILE_DAMAGE_REDUCTION.get(),
            IblisAttributes.FIRE_DAMAGE_REDUCTION.get(),
            IblisAttributes.EXPLOSION_DAMAGE_REDUCTION.get());

    private static double modifyDouble(double baseValue, double skillValue, boolean additive) {
        if (additive) {
            return baseValue + skillValue * 0.1;
        }
        if (skillValue < 0.0) {
            return baseValue / (1.0 - skillValue);
        }
        if (skillValue > 0.0) {
            return baseValue * skillValue * 0.1 + baseValue;
        }
        return baseValue;
    }

    private static int modifyInt(int baseValue, double skillValue, boolean additive) {
        if (additive) {
            return Mth.ceil(baseValue + skillValue * 0.1);
        }
        if (skillValue < 0.0) {
            return Mth.floor(baseValue / (1.0 - skillValue));
        }
        if (skillValue > 0.0) {
            return Mth.ceil(baseValue * skillValue * 0.1 + baseValue);
        }
        return baseValue;
    }

    private record Modifier(Attribute attribute, AttributeModifier modifier, EquipmentSlot slot) {
    }
}
