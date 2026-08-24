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
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;

public final class IblisArmorItem extends ArmorItem {
    private static final String DURABILITY = "durability";
    private static final UUID[] ARMOR_MODIFIERS = {
            UUID.fromString("845DB27C-C624-495F-8C9F-6020A9A58B6B"),
            UUID.fromString("D8499B04-0E66-4726-AB29-64469D734E0D"),
            UUID.fromString("9F3D476D-C118-4544-8365-64846904B48E"),
            UUID.fromString("2AD3F246-FEE1-4E67-B886-69FD380BB150")
    };
    private final Supplier<Multimap<Attribute, AttributeModifier>> attributeModifiers;

    public IblisArmorItem(ArmorMaterial material, Type type, Properties properties) {
        super(material, type, properties);
        attributeModifiers = Suppliers.memoize(this::createAttributeModifiers);
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(DURABILITY)
                ? stack.getTag().getInt(DURABILITY)
                : super.getMaxDamage(stack);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(
            EquipmentSlot slot, ItemStack stack) {
        if (slot != getEquipmentSlot()) {
            return super.getAttributeModifiers(slot, stack);
        }
        return attributeModifiers.get();
    }

    private Multimap<Attribute, AttributeModifier> createAttributeModifiers() {
        EquipmentSlot slot = getEquipmentSlot();
        Multimap<Attribute, AttributeModifier> base =
                super.getAttributeModifiers(slot, ItemStack.EMPTY);
        ImmutableMultimap.Builder<Attribute, AttributeModifier> result = ImmutableMultimap.builder();
        result.putAll(base);
        UUID uuid = ARMOR_MODIFIERS[slot.getIndex()];
        double amount = getMaterial().getToughness() / 2.0;
        result.put(IblisAttributes.EXPLOSION_DAMAGE_REDUCTION.get(), modifier(uuid, "Explosion", amount));
        result.put(IblisAttributes.FIRE_DAMAGE_REDUCTION.get(), modifier(uuid, "Fire", amount));
        result.put(IblisAttributes.PROJECTILE_DAMAGE_REDUCTION.get(), modifier(uuid, "Projectile", amount));
        result.put(IblisAttributes.MELEE_DAMAGE_REDUCTION.get(), modifier(uuid, "Melee", amount));
        return result.build();
    }

    private static AttributeModifier modifier(UUID uuid, String kind, double amount) {
        return new AttributeModifier(uuid, kind + " damage reduction modifier", amount,
                AttributeModifier.Operation.ADDITION);
    }
}
