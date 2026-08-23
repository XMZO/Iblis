package iblis.item;

import iblis.IblisMod;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;

public enum IblisArmorMaterial implements ArmorMaterial {
    STEEL("steel", 33, 0, 4.0F, SoundEvents.ARMOR_EQUIP_IRON,
            Map.of(ArmorItem.Type.BOOTS, 6, ArmorItem.Type.LEGGINGS, 12,
                    ArmorItem.Type.CHESTPLATE, 16, ArmorItem.Type.HELMET, 6),
            () -> Ingredient.of(TagKey.create(Registries.ITEM,
                    ResourceLocation.fromNamespaceAndPath("forge", "ingots/steel"))));

    private static final Map<ArmorItem.Type, Integer> DURABILITY = new EnumMap<>(Map.of(
            ArmorItem.Type.BOOTS, 13,
            ArmorItem.Type.LEGGINGS, 15,
            ArmorItem.Type.CHESTPLATE, 16,
            ArmorItem.Type.HELMET, 11));

    private final String name;
    private final int durabilityMultiplier;
    private final int enchantmentValue;
    private final float toughness;
    private final SoundEvent equipSound;
    private final Map<ArmorItem.Type, Integer> defense;
    private final Supplier<Ingredient> repairIngredient;

    IblisArmorMaterial(String name, int durabilityMultiplier, int enchantmentValue, float toughness,
                       SoundEvent equipSound, Map<ArmorItem.Type, Integer> defense,
                       Supplier<Ingredient> repairIngredient) {
        this.name = name;
        this.durabilityMultiplier = durabilityMultiplier;
        this.enchantmentValue = enchantmentValue;
        this.toughness = toughness;
        this.equipSound = equipSound;
        this.defense = defense;
        this.repairIngredient = repairIngredient;
    }

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return DURABILITY.get(type) * durabilityMultiplier;
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return defense.getOrDefault(type, 0);
    }

    @Override
    public int getEnchantmentValue() {
        return enchantmentValue;
    }

    @Override
    public SoundEvent getEquipSound() {
        return equipSound;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return repairIngredient.get();
    }

    @Override
    public String getName() {
        return IblisMod.MOD_ID + ':' + name;
    }

    @Override
    public float getToughness() {
        return toughness;
    }

    @Override
    public float getKnockbackResistance() {
        return 0.0F;
    }
}
