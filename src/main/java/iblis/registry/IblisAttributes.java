package iblis.registry;

import iblis.IblisMod;
import java.util.List;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class IblisAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(ForgeRegistries.ATTRIBUTES, IblisMod.MOD_ID);

    public static final RegistryObject<Attribute> MELEE_DAMAGE_BONUS = register("melee_damage_bonus", 0.0);
    public static final RegistryObject<Attribute> PROJECTILE_DAMAGE = register("projectile_damage", 0.0);
    public static final RegistryObject<Attribute> MELEE_DAMAGE_REDUCTION = register("melee_damage_reduction", 0.0);
    public static final RegistryObject<Attribute> EXPLOSION_DAMAGE_REDUCTION = register("explosion_damage_reduction", 0.0);
    public static final RegistryObject<Attribute> FIRE_DAMAGE_REDUCTION = register("fire_damage_reduction", 0.0);
    public static final RegistryObject<Attribute> PROJECTILE_DAMAGE_REDUCTION = register("projectile_damage_reduction", 0.0);
    public static final RegistryObject<Attribute> INTELLIGENCE = register("intelligence", 0.0);
    public static final RegistryObject<Attribute> GLUTTONY = register("gluttony", 20.0);

    public static final RegistryObject<Attribute> WISDOM = register("wisdom", 0.0);
    public static final RegistryObject<Attribute> MARTIAL_ARTS = register("martial_arts", 0.0);
    public static final RegistryObject<Attribute> BOXING = register("boxing", 0.0);
    public static final RegistryObject<Attribute> SWORDSMANSHIP = register("swordsmanship", 0.0);
    public static final RegistryObject<Attribute> PARRY = register("parry", 0.0);
    public static final RegistryObject<Attribute> ARCHERY = register("archery", 0.0);
    public static final RegistryObject<Attribute> THROWING = register("throwing", 0.0);
    public static final RegistryObject<Attribute> SHARPSHOOTING = register("sharpshooting", 0.0);

    public static final RegistryObject<Attribute> CRAFTMANSHIP = register("craftmanship", 0.0);
    public static final RegistryObject<Attribute> WEAPONSMITH = register("weaponsmith", 0.0);
    public static final RegistryObject<Attribute> ARMORSMITH = register("armorsmith", 0.0);
    public static final RegistryObject<Attribute> MECHANICS = register("mechanics", 0.0);
    public static final RegistryObject<Attribute> MEDICAL_AID = register("medical_aid", 0.0);
    public static final RegistryObject<Attribute> DIGGING = register("digging", 0.0);
    public static final RegistryObject<Attribute> CHEMISTRY = register("chemistry", 0.0);

    public static final RegistryObject<Attribute> ACROBATICS = register("acrobatics", 0.0);
    public static final RegistryObject<Attribute> RUNNING = register("running", 0.0);
    public static final RegistryObject<Attribute> JUMPING = register("jumping", 0.0);
    public static final RegistryObject<Attribute> FALLING = register("falling", 0.0);
    public static final RegistryObject<Attribute> EQUILIBRIUM = register("equilibrium", 0.0);

    private static final List<RegistryObject<Attribute>> PLAYER_ATTRIBUTES = List.of(
            MELEE_DAMAGE_BONUS, PROJECTILE_DAMAGE, MELEE_DAMAGE_REDUCTION,
            EXPLOSION_DAMAGE_REDUCTION, FIRE_DAMAGE_REDUCTION, PROJECTILE_DAMAGE_REDUCTION,
            INTELLIGENCE, GLUTTONY, WISDOM, MARTIAL_ARTS, BOXING, SWORDSMANSHIP,
            PARRY, ARCHERY, THROWING, SHARPSHOOTING, CRAFTMANSHIP, WEAPONSMITH,
            ARMORSMITH, MECHANICS, MEDICAL_AID, DIGGING, CHEMISTRY, ACROBATICS,
            RUNNING, JUMPING, FALLING, EQUILIBRIUM);

    private IblisAttributes() {
    }

    private static RegistryObject<Attribute> register(String name, double defaultValue) {
        return ATTRIBUTES.register(name, () -> new RangedAttribute(
                "attribute.name." + IblisMod.MOD_ID + "." + name,
                defaultValue, 0.0, Double.MAX_VALUE).setSyncable(true));
    }

    public static void register(IEventBus modBus) {
        ATTRIBUTES.register(modBus);
    }

    public static void addPlayerAttributes(EntityAttributeModificationEvent event) {
        for (RegistryObject<Attribute> attribute : PLAYER_ATTRIBUTES) {
            if (!event.has(EntityType.PLAYER, attribute.get())) {
                event.add(EntityType.PLAYER, attribute.get());
            }
        }
    }

    public static List<Attribute> playerAttributes() {
        return PLAYER_ATTRIBUTES.stream().map(RegistryObject::get).toList();
    }
}
