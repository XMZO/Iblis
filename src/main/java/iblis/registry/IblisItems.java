package iblis.registry;

import iblis.IblisMod;
import iblis.item.GuideBookItem;
import iblis.item.AmmoItem;
import iblis.item.HeavyShieldItem;
import iblis.item.IblisArmorItem;
import iblis.item.IblisArmorMaterial;
import iblis.item.MedkitItem;
import iblis.item.NonSterileMedkitItem;
import iblis.item.ThrowingWeaponItem;
import iblis.item.CrossbowItem;
import iblis.item.CrossbowReloadingItem;
import iblis.item.ShotgunItem;
import iblis.item.ShotgunReloadingItem;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ArmorItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class IblisItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, IblisMod.MOD_ID);

    public static final RegistryObject<Item> IRON_COAL = blockItem("iron_coal", IblisBlocks.IRON_COAL);
    public static final RegistryObject<Item> IRONORE_COAL = blockItem("ironore_coal", IblisBlocks.IRONORE_COAL);
    public static final RegistryObject<Item> SLAG = blockItem("slag", IblisBlocks.SLAG);

    public static final RegistryObject<GuideBookItem> GUIDE = ITEMS.register(
            "guide", () -> new GuideBookItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<AmmoItem> SHOTGUN_BULLET = ITEMS.register(
            "shotgun_bullet", () -> new AmmoItem(new Item.Properties(), 2.0F, 0));
    public static final RegistryObject<AmmoItem> SHOTGUN_SHOT = ITEMS.register(
            "shotgun_shot", () -> new AmmoItem(new Item.Properties(), 1.0F, 1));
    public static final RegistryObject<AmmoItem> CROSSBOW_BOLT = ITEMS.register(
            "crossbow_bolt", () -> new AmmoItem(new Item.Properties(), 0.5F, 0));
    public static final RegistryObject<ShotgunItem> SHOTGUN = ITEMS.register(
            "six_barrels_shotgun", () -> new ShotgunItem(new Item.Properties().durability(1561)));
    public static final RegistryObject<ShotgunReloadingItem> SHOTGUN_RELOADING = ITEMS.register(
            "six_barrels_shotgun_reloading", () -> new ShotgunReloadingItem(
                    new Item.Properties().durability(1561), SHOTGUN));
    public static final RegistryObject<CrossbowItem> CROSSBOW = ITEMS.register(
            "double_crossbow", () -> new CrossbowItem(new Item.Properties().durability(1561)));
    public static final RegistryObject<CrossbowReloadingItem> CROSSBOW_RELOADING = ITEMS.register(
            "double_crossbow_reloading", () -> new CrossbowReloadingItem(
                    new Item.Properties().durability(1561), CROSSBOW));
    public static final RegistryObject<Item> INGOT_STEEL = simple("ingot_steel");
    public static final RegistryObject<Item> INGOT_BRONZE = simple("ingot_bronze");
    public static final RegistryObject<Item> NUGGET_STEEL = simple("nugget_steel");
    public static final RegistryObject<Item> TRIGGER_SPRING = simple("trigger_spring");
    public static final RegistryObject<Item> RAISIN = ITEMS.register("raisin", () -> new Item(
            new Item.Properties().food(new FoodProperties.Builder()
                    .nutrition(4).saturationMod(0.3F).build())));
    public static final RegistryObject<NonSterileMedkitItem> NONSTERILE_MEDKIT = ITEMS.register(
            "non-sterile_medkit", () -> new NonSterileMedkitItem(
                    new Item.Properties().stacksTo(1)));
    public static final RegistryObject<MedkitItem> MEDKIT = ITEMS.register(
            "medkit", () -> new MedkitItem(new Item.Properties().durability(10)));
    public static final RegistryObject<HeavyShieldItem> HEAVY_SHIELD = ITEMS.register(
            "heavy_shield", () -> new HeavyShieldItem(new Item.Properties().durability(600)));
    public static final RegistryObject<ThrowingWeaponItem> BOULDER = ITEMS.register(
            "boulder", () -> new ThrowingWeaponItem(new Item.Properties().stacksTo(16),
                    ThrowingWeaponItem.Kind.BOULDER));
    public static final RegistryObject<ThrowingWeaponItem> IRON_THROWING_KNIFE = ITEMS.register(
            "iron_throwing_knife", () -> new ThrowingWeaponItem(new Item.Properties().stacksTo(16),
                    ThrowingWeaponItem.Kind.IRON_KNIFE));
    public static final RegistryObject<IblisArmorItem> STEEL_HELMET = armor("steel_helmet", ArmorItem.Type.HELMET);
    public static final RegistryObject<IblisArmorItem> STEEL_CHESTPLATE = armor(
            "steel_chestplate", ArmorItem.Type.CHESTPLATE);
    public static final RegistryObject<IblisArmorItem> STEEL_LEGGINS = armor(
            "steel_leggins", ArmorItem.Type.LEGGINGS);
    public static final RegistryObject<IblisArmorItem> STEEL_BOOTS = armor("steel_boots", ArmorItem.Type.BOOTS);

    private IblisItems() {
    }

    private static RegistryObject<Item> simple(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties()));
    }

    private static RegistryObject<IblisArmorItem> armor(String name, ArmorItem.Type type) {
        return ITEMS.register(name, () -> new IblisArmorItem(
                IblisArmorMaterial.STEEL, type, new Item.Properties()));
    }

    private static RegistryObject<Item> blockItem(String name, RegistryObject<? extends net.minecraft.world.level.block.Block> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
