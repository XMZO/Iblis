package iblis_headshots.config;

import java.util.List;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.event.config.ModConfigEvent;

public final class HeadshotsConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.DoubleValue PARTICLE_SIZE;
    private static final ForgeConfigSpec.IntValue PARTICLE_TYPE;
    private static final ForgeConfigSpec.DoubleValue NON_PROJECTILE_MIN_DISTANCE;
    private static final ForgeConfigSpec.DoubleValue DAMAGE_MULTIPLIER;
    private static final ForgeConfigSpec.DoubleValue HEADGEAR_DAMAGE_MULTIPLIER;
    private static final ForgeConfigSpec.DoubleValue BODYSHOT_DAMAGE_MULTIPLIER;
    private static final ForgeConfigSpec.DoubleValue PLAYER_HEADSHOT_CHANCE;
    private static final ForgeConfigSpec.BooleanValue PLAYER_HEADSHOT_CHANCE_AFFECTS_PVP;
    private static final ForgeConfigSpec.BooleanValue PLAYERS_HAVE_NO_HEADS;

    public static final ForgeConfigSpec SPEC;

    public static volatile float particleSize = 10.0F;
    public static volatile int particleType = 1;
    public static volatile double nonProjectileMinDistanceSquared = 256.0;
    public static volatile float damageMultiplier = 4.0F;
    public static volatile float headgearDamageMultiplier = 4.0F;
    public static volatile float bodyshotDamageMultiplier = 1.0F;
    public static volatile float playerHeadshotChance = 1.0F;
    public static volatile boolean playerHeadshotChanceAffectsPvp;
    public static volatile boolean playersHaveNoHeads;

    static {
        BUILDER.push("general");
        PARTICLE_SIZE = BUILDER.comment("Visual size of the headshot particle effect.")
                .defineInRange("headshot_particle_size", 10.0, 0.0, 1_000_000.0);
        PARTICLE_TYPE = BUILDER.comment(
                        "Headshot particle: 0 = none, 1 = skull, 2 = aim, 3 = star.")
                .defineInRange("headshot_particle_type", 1, 0, 3);
        NON_PROJECTILE_MIN_DISTANCE = BUILDER.comment(
                        "Minimum distance for non-projectile headshots, in blocks.")
                .defineInRange("non_projectile_headshot_min_distance", 16.0, 0.0, 1_000_000.0);
        DAMAGE_MULTIPLIER = BUILDER.comment(
                        "Headshot damage multiplier; 1.0 keeps normal damage.")
                .defineInRange("headshot_damage_mutiplier", 4.0, 0.0, 1_000_000.0);
        HEADGEAR_DAMAGE_MULTIPLIER = BUILDER.comment(
                        "Multiplier for durability damage dealt to headgear on a headshot.")
                .defineInRange("headgear_damage_mutiplier", 4.0, 0.0, 1_000_000.0);
        BODYSHOT_DAMAGE_MULTIPLIER = BUILDER.comment(
                        "Non-headshot damage multiplier; 1.0 keeps normal damage.")
                .defineInRange("bodyshot_damage_mutiplier", 1.0, 0.0, 1_000_000.0);
        PLAYER_HEADSHOT_CHANCE = BUILDER.comment(
                        "Chance that a head hit on a player counts as a headshot; 1.0 = always.")
                .defineInRange("player_headshot_chance", 1.0, 0.0, 1.0);
        PLAYER_HEADSHOT_CHANCE_AFFECTS_PVP = BUILDER.comment(
                        "If true, player_headshot_chance also applies to PvP head hits.")
                .define("player_headshot_chance_affects_pvp", false);
        PLAYERS_HAVE_NO_HEADS = BUILDER.comment(
                        "If true, hits on players always use the non-headshot multiplier.")
                .define("players_have_no_heads", false);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    private HeadshotsConfig() {
    }

    public static void onConfigChanged(ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) {
            return;
        }

        bake();
    }

    public static void bake() {

        particleSize = PARTICLE_SIZE.get().floatValue();
        particleType = PARTICLE_TYPE.get();
        double distance = NON_PROJECTILE_MIN_DISTANCE.get();
        nonProjectileMinDistanceSquared = distance * distance;
        damageMultiplier = DAMAGE_MULTIPLIER.get().floatValue();
        headgearDamageMultiplier = HEADGEAR_DAMAGE_MULTIPLIER.get().floatValue();
        bodyshotDamageMultiplier = BODYSHOT_DAMAGE_MULTIPLIER.get().floatValue();
        playerHeadshotChance = PLAYER_HEADSHOT_CHANCE.get().floatValue();
        playerHeadshotChanceAffectsPvp = PLAYER_HEADSHOT_CHANCE_AFFECTS_PVP.get();
        playersHaveNoHeads = PLAYERS_HAVE_NO_HEADS.get();
    }

    public static void save() {
        SPEC.save();
        bake();
    }

    public static EditableConfigCategory editableCategory() {
        return new EditableConfigCategory("iblis_headshots.config.general", SPEC, List.of(
                EditableConfigValue.numberValue(
                        "headshot_particle_size", PARTICLE_SIZE, 0.0, 1_000_000.0),
                EditableConfigValue.numberValue(
                        "headshot_particle_type", PARTICLE_TYPE, 0.0, 3.0),
                EditableConfigValue.numberValue(
                        "non_projectile_headshot_min_distance", NON_PROJECTILE_MIN_DISTANCE,
                        0.0, 1_000_000.0),
                EditableConfigValue.numberValue(
                        "headshot_damage_mutiplier", DAMAGE_MULTIPLIER, 0.0, 1_000_000.0),
                EditableConfigValue.numberValue(
                        "headgear_damage_mutiplier", HEADGEAR_DAMAGE_MULTIPLIER, 0.0, 1_000_000.0),
                EditableConfigValue.numberValue(
                        "bodyshot_damage_mutiplier", BODYSHOT_DAMAGE_MULTIPLIER, 0.0, 1_000_000.0),
                EditableConfigValue.numberValue(
                        "player_headshot_chance", PLAYER_HEADSHOT_CHANCE, 0.0, 1.0),
                EditableConfigValue.booleanValue(
                        "player_headshot_chance_affects_pvp",
                        PLAYER_HEADSHOT_CHANCE_AFFECTS_PVP),
                EditableConfigValue.booleanValue(
                        "players_have_no_heads", PLAYERS_HAVE_NO_HEADS)));
    }
}
