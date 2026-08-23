package iblis.crafting;

import iblis.IblisMod;
import iblis.item.AmmoItem;
import iblis.item.GuideBookItem;
import iblis.player.PlayerSkill;
import iblis.registry.IblisItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.event.entity.player.AnvilRepairEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = IblisMod.MOD_ID)
public final class IblisCraftingEvents {
    private IblisCraftingEvents() {
    }

    @SubscribeEvent
    public static void itemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack output = event.getCrafting();
        if (output.getItem() instanceof GuideBookItem) {
            GuideBookItem.fillDiary(output, player);
            return;
        }

        CraftingProfile profile = profileFor(output);
        if (profile == null) {
            return;
        }
        double fullSkill = profile.skill.getRawFullValue(player);
        if (profile.skill.enabled) {
            if (profile.ammunitionQuality) {
                AmmoItem.setLegacyQuality(output, Math.max((int) (fullSkill - profile.minimum), 0));
            } else if (profile.applyQuality) {
                CraftingQuality.apply(output, fullSkill - profile.minimum,
                        profile.baseDurability, false);
            }
        }
        profile.skill.raise(player, profile.experience);
    }

    @SubscribeEvent
    public static void itemRepaired(AnvilRepairEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        CraftingProfile profile = generalProfile(event.getOutput());
        if (profile != null && profile.skill.enabled && profile.applyQuality) {
            CraftingQuality.apply(event.getOutput(),
                    profile.skill.getRawFullValue(player) - profile.minimum, 0, true);
        }
    }

    /**
     * Returns the legacy skill profile for a crafting result.  This is shared
     * with the workbench training button so the client hint and server-side
     * validation always use the exact same classification as real crafting.
     */
    public static CraftingProfile profileFor(ItemStack stack) {
        Item item = stack.getItem();
        if (item == IblisItems.SHOTGUN_BULLET.get() || item == IblisItems.SHOTGUN_SHOT.get()) {
            return new CraftingProfile(PlayerSkill.CHEMISTRY, 0.0, 1.0, 0, false, true);
        }
        if (item == IblisItems.CROSSBOW_BOLT.get()) {
            return new CraftingProfile(PlayerSkill.WEAPONSMITH, 0.0, 2.0, 0, false, true);
        }
        if (item == IblisItems.IRON_THROWING_KNIFE.get()) {
            return new CraftingProfile(PlayerSkill.WEAPONSMITH, 0.0, 5.0, 0, false, true);
        }
        if (item == IblisItems.HEAVY_SHIELD.get()) {
            return new CraftingProfile(PlayerSkill.ARMORSMITH, 4.0, 4.0, 600, true, false);
        }
        if (item == IblisItems.CROSSBOW.get()) {
            return new CraftingProfile(PlayerSkill.MECHANICS, 4.0, 4.0, 600, true, false);
        }
        if (item == IblisItems.SHOTGUN.get()) {
            return new CraftingProfile(PlayerSkill.MECHANICS, 12.0, 20.0, 600, true, false);
        }
        if (item == IblisItems.STEEL_HELMET.get()
                || item == IblisItems.STEEL_CHESTPLATE.get()
                || item == IblisItems.STEEL_LEGGINS.get()
                || item == IblisItems.STEEL_BOOTS.get()) {
            return new CraftingProfile(PlayerSkill.ARMORSMITH, 12.0, 12.0,
                    stack.getMaxDamage(), true, false);
        }
        return generalProfile(stack);
    }

    private static CraftingProfile generalProfile(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof ArmorItem armor) {
            double required = armorRequirement(armor);
            return new CraftingProfile(PlayerSkill.ARMORSMITH, required, required + 1.0,
                    0, true, false);
        }
        if (isAttributeArmor(stack)) {
            double required = armorRequirement(stack);
            return new CraftingProfile(PlayerSkill.ARMORSMITH, required, required + 1.0,
                    0, true, false);
        }
        double attackDamage = stack.getAttributeModifiers(EquipmentSlot.MAINHAND)
                .get(Attributes.ATTACK_DAMAGE).stream()
                .mapToDouble(AttributeModifier::getAmount).sum();
        if (attackDamage != 0.0) {
            double required = attackDamage * 2.0;
            double experience = required + 1.0;
            if (item instanceof DiggerItem digger
                    && (digger.getTier() == Tiers.WOOD || digger.getTier() == Tiers.STONE)) {
                experience = 0.2;
            }
            return new CraftingProfile(PlayerSkill.WEAPONSMITH, required, experience,
                    0, true, false);
        }
        if (item instanceof BowItem || item instanceof CrossbowItem) {
            return new CraftingProfile(PlayerSkill.MECHANICS, 1.0, 2.0, 0, true, false);
        }
        if (item instanceof ShieldItem) {
            return new CraftingProfile(PlayerSkill.ARMORSMITH, 0.0, 2.0, 0, false, false);
        }
        if (item == Items.PISTON || item == Items.CLOCK || item == Items.NOTE_BLOCK
                || item == Items.DISPENSER || item == Items.JUKEBOX) {
            return new CraftingProfile(PlayerSkill.MECHANICS, 0.0, 1.0, 0, false, false);
        }
        return null;
    }

    private static double armorRequirement(ArmorItem armor) {
        if (armor.getMaterial() == ArmorMaterials.LEATHER) {
            return 1.0;
        }
        if (armor.getMaterial() == ArmorMaterials.CHAIN) {
            return 2.0;
        }
        if (armor.getMaterial() == ArmorMaterials.GOLD) {
            return 4.0;
        }
        if (armor.getMaterial() == ArmorMaterials.IRON) {
            return 6.0;
        }
        if (armor.getMaterial() == ArmorMaterials.DIAMOND) {
            return 10.0;
        }
        if (armor.getMaterial() == ArmorMaterials.NETHERITE) {
            return 12.0;
        }
        double armorValue = armor.getDefense();
        double toughness = armor.getToughness();
        return armorValue + toughness;
    }

    private static boolean isAttributeArmor(ItemStack stack) {
        if (stack.getMaxStackSize() != 1) {
            return false;
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!stack.getAttributeModifiers(slot).get(Attributes.ARMOR).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static double armorRequirement(ItemStack stack) {
        double result = 0.0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            result += stack.getAttributeModifiers(slot).get(Attributes.ARMOR).stream()
                    .mapToDouble(AttributeModifier::getAmount).sum();
            result += stack.getAttributeModifiers(slot).get(Attributes.ARMOR_TOUGHNESS).stream()
                    .mapToDouble(AttributeModifier::getAmount).sum();
        }
        return result;
    }

    public record CraftingProfile(PlayerSkill skill, double minimum, double experience,
                                  int baseDurability, boolean applyQuality,
                                  boolean ammunitionQuality) {
    }
}
