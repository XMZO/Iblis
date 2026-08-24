package iblis.item;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import iblis.registry.IblisAttributes;
import java.util.UUID;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;

public final class HeavyShieldItem extends ShieldItem {
    private static final UUID RUNNING_MODIFIER =
            UUID.fromString("00051e7f-fefd-f1e7-07e4-0000000000e9");
    private static final Supplier<Multimap<Attribute, AttributeModifier>> OFF_HAND_MODIFIERS =
            Suppliers.memoize(() -> ImmutableMultimap.of(
                    IblisAttributes.RUNNING.get(),
                    new AttributeModifier(RUNNING_MODIFIER, "Running skill modifier", -0.4,
                            AttributeModifier.Operation.MULTIPLY_BASE)));

    public HeavyShieldItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains("durability")
                ? stack.getTag().getInt("durability")
                : super.getMaxDamage(stack);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(
            EquipmentSlot slot, ItemStack stack) {
        if (slot != EquipmentSlot.OFFHAND) {
            return super.getAttributeModifiers(slot, stack);
        }
        return OFF_HAND_MODIFIERS.get();
    }
}
