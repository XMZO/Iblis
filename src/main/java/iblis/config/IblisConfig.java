package iblis.config;

import iblis.network.IblisNetwork;
import iblis.player.PlayerCharacteristic;
import iblis.player.PlayerSkill;
import java.util.EnumMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.event.config.ModConfigEvent;

public final class IblisConfig {
    private static final int GAMEPLAY_SYNC_VERSION = 2;
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    private static final Map<PlayerSkill, ForgeConfigSpec.BooleanValue> SKILL_ENABLED =
            new EnumMap<>(PlayerSkill.class);
    private static final Map<PlayerSkill, ForgeConfigSpec.DoubleValue> SKILL_CAP =
            new EnumMap<>(PlayerSkill.class);
    private static final Map<PlayerSkill, ForgeConfigSpec.DoubleValue> SKILL_XP =
            new EnumMap<>(PlayerSkill.class);
    private static final Map<PlayerCharacteristic, ForgeConfigSpec.BooleanValue> CHARACTERISTIC_ENABLED =
            new EnumMap<>(PlayerCharacteristic.class);
    private static final Map<PlayerCharacteristic, ForgeConfigSpec.DoubleValue> CHARACTERISTIC_START =
            new EnumMap<>(PlayerCharacteristic.class);
    private static final Map<PlayerCharacteristic, ForgeConfigSpec.DoubleValue> CHARACTERISTIC_POINTS =
            new EnumMap<>(PlayerCharacteristic.class);
    private static final Map<PlayerCharacteristic, ForgeConfigSpec.DoubleValue> CHARACTERISTIC_CAP =
            new EnumMap<>(PlayerCharacteristic.class);

    private static final ForgeConfigSpec.DoubleValue SKILL_TRAINING_BASE_RESISTANCE;
    private static final ForgeConfigSpec.DoubleValue SKILL_TRAINING_LINEAR_RESISTANCE;
    private static final ForgeConfigSpec.DoubleValue SKILL_TRAINING_QUADRATIC_RESISTANCE;
    private static final ForgeConfigSpec.DoubleValue BONUS_BASE_MULTIPLIER;
    private static final ForgeConfigSpec.DoubleValue BONUS_SOFT_CAP_SCALE;
    private static final ForgeConfigSpec.DoubleValue CHARACTERISTIC_COST_BASE_MULTIPLIER;
    private static final ForgeConfigSpec.IntValue CHARACTERISTIC_COST_MID_START_LEVEL;
    private static final ForgeConfigSpec.DoubleValue CHARACTERISTIC_COST_MID_MULTIPLIER;
    private static final ForgeConfigSpec.IntValue CHARACTERISTIC_COST_LATE_START_LEVEL;
    private static final ForgeConfigSpec.DoubleValue CHARACTERISTIC_COST_LATE_MULTIPLIER;

    private static final ForgeConfigSpec.BooleanValue SPAWN_PLAYER_ZOMBIE;
    private static final ForgeConfigSpec.BooleanValue NO_DEATH_PENALTY;
    private static final ForgeConfigSpec.BooleanValue NO_INCREASED_MOB_SEEK_RANGE;
    private static final ForgeConfigSpec.BooleanValue MOB_REACT_ONLY_ON_SHOOTING;
    private static final ForgeConfigSpec.BooleanValue MEDKIT_INSTANT_HEALING;
    private static final ForgeConfigSpec.BooleanValue TOGGLE_SPRINT;
    private static final ForgeConfigSpec.BooleanValue RENDER_HP_BAR;
    private static final ForgeConfigSpec.BooleanValue DISABLE_MEDKIT_RECIPE;
    private static final ForgeConfigSpec.BooleanValue DISABLE_SHOTGUN_RECIPE;
    private static final ForgeConfigSpec.IntValue SHOTGUN_FIRE_COOLDOWN_TICKS;
    private static final ForgeConfigSpec.IntValue CROSSBOW_FIRE_COOLDOWN_TICKS;
    private static final ForgeConfigSpec.BooleanValue SHOTGUN_HITS_ENDERMEN;
    private static final ForgeConfigSpec.BooleanValue SHOTGUN_DISABLES_SHIELDS;

    private static final int LEGACY_SHOTGUN_FIRE_COOLDOWN_TICKS = 20;
    private static final int LEGACY_CROSSBOW_FIRE_COOLDOWN_TICKS = 16;

    public static final ForgeConfigSpec SPEC;

    public static volatile boolean spawnPlayerZombie;
    public static volatile boolean noDeathPenalty = true;
    public static volatile boolean noIncreasedMobSeekRange;
    public static volatile boolean mobReactOnlyOnShooting;
    public static volatile boolean medkitInstantHealing;
    public static volatile boolean toggleSprint;
    public static volatile boolean renderHpBar;
    public static volatile boolean disableMedkitRecipe;
    public static volatile boolean disableShotgunRecipe;
    public static volatile int shotgunFireCooldownTicks = 14;
    public static volatile int crossbowFireCooldownTicks = 12;
    public static volatile boolean shotgunHitsEndermen = true;
    public static volatile boolean shotgunDisablesShields = true;
    public static volatile double skillTrainingBaseResistance = 1.0;
    public static volatile double skillTrainingLinearResistance = 0.5;
    public static volatile double skillTrainingQuadraticResistance = 0.0125;
    public static volatile double bonusBaseMultiplier = 0.95;
    public static volatile double bonusSoftCapScale = 30.0;
    public static volatile double characteristicCostBaseMultiplier = 1.0;
    public static volatile int characteristicCostMidStartLevel = 5;
    public static volatile double characteristicCostMidMultiplier = 0.25;
    public static volatile int characteristicCostLateStartLevel = 15;
    public static volatile double characteristicCostLateMultiplier = 0.35;
    private static volatile boolean remoteServerAuthority;

    static {
        defineSkills();
        defineCharacteristics();

        BUILDER.push("progression_balance");
        SKILL_TRAINING_BASE_RESISTANCE = BUILDER
                .comment("Base divisor for all skill gains. Higher values train more slowly.")
                .defineInRange("skill_training_base_resistance", 1.0, 0.01, 1000.0);
        SKILL_TRAINING_LINEAR_RESISTANCE = BUILDER
                .comment("Extra skill-gain divisor per current skill point.")
                .defineInRange("skill_training_linear_resistance", 0.5, 0.0, 1000.0);
        SKILL_TRAINING_QUADRATIC_RESISTANCE = BUILDER
                .comment("Extra divisor per squared skill point; controls late-game slowdown.")
                .defineInRange("skill_training_quadratic_resistance", 0.0125, 0.0, 1000.0);
        BONUS_BASE_MULTIPLIER = BUILDER
                .comment("Multiplier applied before diminishing returns to progression bonuses.")
                .defineInRange("bonus_base_multiplier", 0.95, 0.0, 1000.0);
        BONUS_SOFT_CAP_SCALE = BUILDER
                .comment("Progression value where bonus efficiency is roughly halved.")
                .defineInRange("bonus_soft_cap_scale", 30.0, 0.01, 100000.0);
        CHARACTERISTIC_COST_BASE_MULTIPLIER = BUILDER
                .comment("XP-level cost per characteristic level before surcharges.")
                .defineInRange("characteristic_cost_base_multiplier", 1.0, 0.0, 1000.0);
        CHARACTERISTIC_COST_MID_START_LEVEL = BUILDER
                .comment("Characteristic level after which the mid-game surcharge starts.")
                .defineInRange("characteristic_cost_mid_start_level", 5, 1, 100000);
        CHARACTERISTIC_COST_MID_MULTIPLIER = BUILDER
                .comment("Extra XP levels per level above the mid-game threshold.")
                .defineInRange("characteristic_cost_mid_multiplier", 0.25, 0.0, 1000.0);
        CHARACTERISTIC_COST_LATE_START_LEVEL = BUILDER
                .comment("Characteristic level after which the late-game surcharge starts.")
                .defineInRange("characteristic_cost_late_start_level", 15, 1, 100000);
        CHARACTERISTIC_COST_LATE_MULTIPLIER = BUILDER
                .comment("Extra XP levels per level above the late-game threshold.")
                .defineInRange("characteristic_cost_late_multiplier", 0.35, 0.0, 1000.0);
        BUILDER.pop();

        BUILDER.push("firearms");
        SHOTGUN_FIRE_COOLDOWN_TICKS = BUILDER
                .comment("Base delay between successful shotgun shots, in game ticks (20 ticks = 1 second).",
                        "Attack Speed can raise firing rate by up to 25%. Set to 0 to disable firing cooldown entirely.")
                .defineInRange("shotgun_fire_cooldown_ticks", 14, 0, 1200);
        CROSSBOW_FIRE_COOLDOWN_TICKS = BUILDER
                .comment("Base delay between successful double-crossbow shots, in game ticks (20 ticks = 1 second).",
                        "Attack Speed can raise firing rate by up to 25%. Set to 0 to disable firing cooldown entirely.")
                .defineInRange("crossbow_fire_cooldown_ticks", 12, 0, 1200);
        SHOTGUN_HITS_ENDERMEN = bool("shotgun_hits_endermen", true,
                "Allow shotgun hits to damage Endermen instead of triggering their projectile dodge.");
        SHOTGUN_DISABLES_SHIELDS = bool("shotgun_disables_shields", true,
                "Give ordinary shields a 2-second cooldown after blocking a shotgun; the Iblis heavy shield is exempt.");
        BUILDER.pop();

        BUILDER.push("general");
        SPAWN_PLAYER_ZOMBIE = bool("spawn_player_zombie", false,
                "Spawn a zombie carrying the dead player's inventory.");
        NO_DEATH_PENALTY = bool("no_death_penalty", true,
                "Keep all skills and characteristics after death; false resets them.");
        NO_INCREASED_MOB_SEEK_RANGE = bool("no_increased_mob_seek_range", false,
                "Disable Iblis effects that increase hostile mob detection range.");
        MOB_REACT_ONLY_ON_SHOOTING = bool("mob_react_only_on_shooting", false,
                "Increase mob awareness only for 60 seconds after firing a shotgun.");
        MEDKIT_INSTANT_HEALING = bool("medkit_instant_healing", false,
                "Heal immediately when using a medkit instead of healing over time.");
        TOGGLE_SPRINT = bool("toggle_sprint_by_sprint_button", false,
                "Client only: press the sprint key once to toggle sprinting.");
        RENDER_HP_BAR = bool("render_hp_bar", false,
                "Client only: replace vanilla hearts with the Iblis health bar.");
        DISABLE_MEDKIT_RECIPE = bool("disable_medkit_recipe", false,
                "Remove the medkit crafting recipe.");
        DISABLE_SHOTGUN_RECIPE = bool("disable_shotgun_recipe", false,
                "Remove crafting recipes for the shotgun and its ammunition.");
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private IblisConfig() {
    }

    private static void defineSkills() {
        BUILDER.push("skills");
        for (PlayerSkill skill : PlayerSkill.values()) {
            SKILL_ENABLED.put(skill, bool(skill.name().toLowerCase(), true,
                    "If false, this skill provides no gameplay bonus."));
        }
        BUILDER.pop();

        BUILDER.push("skills_cap");
        for (PlayerSkill skill : PlayerSkill.values()) {
            SKILL_CAP.put(skill, BUILDER
                    .comment("Training stops when this skill reaches this value.")
                    .defineInRange(
                    skill.name().toLowerCase(), 1000.0, 0.0, 1000.0));
        }
        BUILDER.pop();

        BUILDER.push("skills_xp");
        for (PlayerSkill skill : PlayerSkill.values()) {
            SKILL_XP.put(skill, BUILDER
                    .comment("Base progress per matching action; mid and high levels train more slowly.")
                    .defineInRange(
                    skill.name().toLowerCase(), skill.defaultPointsPerAction, 0.0, 1000.0));
        }
        BUILDER.pop();
    }

    private static void defineCharacteristics() {
        BUILDER.push("characteristics_disabling");
        for (PlayerCharacteristic characteristic : PlayerCharacteristic.values()) {
            CHARACTERISTIC_ENABLED.put(characteristic, bool(characteristic.name().toLowerCase(), true,
                    "If false, players cannot spend levels to raise this characteristic."));
        }
        BUILDER.pop();

        BUILDER.push("characteristics_start_level");
        for (PlayerCharacteristic characteristic : PlayerCharacteristic.values()) {
            CHARACTERISTIC_START.put(characteristic, BUILDER
                    .comment("Baseline used to calculate this characteristic's level cost.")
                    .defineInRange(
                    characteristic.name().toLowerCase(), characteristic.defaultStartLevel, 0.0, 100.0));
        }
        BUILDER.pop();

        BUILDER.push("characteristics_points_per_level");
        for (PlayerCharacteristic characteristic : PlayerCharacteristic.values()) {
            CHARACTERISTIC_POINTS.put(characteristic, BUILDER
                    .comment("Raw value added per level; some bonuses have diminishing returns.")
                    .defineInRange(
                    characteristic.name().toLowerCase(), characteristic.defaultPointsPerLevel, 0.0, 100.0));
        }
        BUILDER.pop();

        BUILDER.push("characteristics_cap");
        for (PlayerCharacteristic characteristic : PlayerCharacteristic.values()) {
            CHARACTERISTIC_CAP.put(characteristic, BUILDER
                    .comment("Players cannot raise this characteristic after reaching this value.")
                    .defineInRange(
                    characteristic.name().toLowerCase(), 1000.0, 0.0, 1000.0));
        }
        BUILDER.pop();
    }

    private static ForgeConfigSpec.BooleanValue bool(String key, boolean defaultValue, String comment) {
        return BUILDER.comment(comment).define(key, defaultValue);
    }

    public static void onConfigChanged(ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) {
            return;
        }

        bake();
        IblisNetwork.broadcastGameplayConfig();
    }

    public static synchronized void bake() {
        // These two settings are intentionally controlled by each client.
        toggleSprint = TOGGLE_SPRINT.get();
        renderHpBar = RENDER_HP_BAR.get();

        // A connected remote server owns every gameplay setting below.  Config
        // reloads and the local config GUI must not overwrite its snapshot.
        if (remoteServerAuthority) {
            return;
        }
        bakeGameplay();
    }

    private static void bakeGameplay() {
        for (PlayerSkill skill : PlayerSkill.values()) {
            skill.enabled = SKILL_ENABLED.get(skill).get();
            skill.cap = SKILL_CAP.get(skill).get();
            skill.pointsPerAction = SKILL_XP.get(skill).get();
        }
        for (PlayerCharacteristic characteristic : PlayerCharacteristic.values()) {
            characteristic.enabled = CHARACTERISTIC_ENABLED.get(characteristic).get();
            characteristic.startLevel = CHARACTERISTIC_START.get(characteristic).get();
            characteristic.pointsPerLevel = CHARACTERISTIC_POINTS.get(characteristic).get();
            characteristic.cap = CHARACTERISTIC_CAP.get(characteristic).get();
        }

        skillTrainingBaseResistance = SKILL_TRAINING_BASE_RESISTANCE.get();
        skillTrainingLinearResistance = SKILL_TRAINING_LINEAR_RESISTANCE.get();
        skillTrainingQuadraticResistance = SKILL_TRAINING_QUADRATIC_RESISTANCE.get();
        bonusBaseMultiplier = BONUS_BASE_MULTIPLIER.get();
        bonusSoftCapScale = BONUS_SOFT_CAP_SCALE.get();
        characteristicCostBaseMultiplier = CHARACTERISTIC_COST_BASE_MULTIPLIER.get();
        characteristicCostMidStartLevel = CHARACTERISTIC_COST_MID_START_LEVEL.get();
        characteristicCostMidMultiplier = CHARACTERISTIC_COST_MID_MULTIPLIER.get();
        characteristicCostLateStartLevel = CHARACTERISTIC_COST_LATE_START_LEVEL.get();
        characteristicCostLateMultiplier = CHARACTERISTIC_COST_LATE_MULTIPLIER.get();
        shotgunFireCooldownTicks = SHOTGUN_FIRE_COOLDOWN_TICKS.get();
        crossbowFireCooldownTicks = CROSSBOW_FIRE_COOLDOWN_TICKS.get();
        shotgunHitsEndermen = SHOTGUN_HITS_ENDERMEN.get();
        shotgunDisablesShields = SHOTGUN_DISABLES_SHIELDS.get();

        spawnPlayerZombie = SPAWN_PLAYER_ZOMBIE.get();
        noDeathPenalty = NO_DEATH_PENALTY.get();
        noIncreasedMobSeekRange = NO_INCREASED_MOB_SEEK_RANGE.get();
        mobReactOnlyOnShooting = MOB_REACT_ONLY_ON_SHOOTING.get();
        medkitInstantHealing = MEDKIT_INSTANT_HEALING.get();
        disableMedkitRecipe = DISABLE_MEDKIT_RECIPE.get();
        disableShotgunRecipe = DISABLE_SHOTGUN_RECIPE.get();
    }

    public static void save() {
        SPEC.save();
        bake();
        IblisNetwork.broadcastGameplayConfig();
    }

    /**
     * Serializes only settings which affect gameplay.  This packet is created
     * on the logical server and is never accepted in the opposite direction.
     */
    public static synchronized CompoundTag createGameplaySnapshot() {
        CompoundTag root = new CompoundTag();
        root.putInt("version", GAMEPLAY_SYNC_VERSION);

        CompoundTag skills = new CompoundTag();
        for (PlayerSkill skill : PlayerSkill.values()) {
            CompoundTag value = new CompoundTag();
            value.putBoolean("enabled", skill.enabled);
            value.putDouble("cap", skill.cap);
            value.putDouble("points_per_action", skill.pointsPerAction);
            skills.put(skill.name(), value);
        }
        root.put("skills", skills);

        CompoundTag characteristics = new CompoundTag();
        for (PlayerCharacteristic characteristic : PlayerCharacteristic.values()) {
            CompoundTag value = new CompoundTag();
            value.putBoolean("enabled", characteristic.enabled);
            value.putDouble("start_level", characteristic.startLevel);
            value.putDouble("points_per_level", characteristic.pointsPerLevel);
            value.putDouble("cap", characteristic.cap);
            characteristics.put(characteristic.name(), value);
        }
        root.put("characteristics", characteristics);

        CompoundTag progression = new CompoundTag();
        progression.putDouble("skill_training_base_resistance", skillTrainingBaseResistance);
        progression.putDouble("skill_training_linear_resistance", skillTrainingLinearResistance);
        progression.putDouble("skill_training_quadratic_resistance", skillTrainingQuadraticResistance);
        progression.putDouble("bonus_base_multiplier", bonusBaseMultiplier);
        progression.putDouble("bonus_soft_cap_scale", bonusSoftCapScale);
        progression.putDouble("characteristic_cost_base_multiplier", characteristicCostBaseMultiplier);
        progression.putInt("characteristic_cost_mid_start_level", characteristicCostMidStartLevel);
        progression.putDouble("characteristic_cost_mid_multiplier", characteristicCostMidMultiplier);
        progression.putInt("characteristic_cost_late_start_level", characteristicCostLateStartLevel);
        progression.putDouble("characteristic_cost_late_multiplier", characteristicCostLateMultiplier);
        root.put("progression", progression);

        CompoundTag firearms = new CompoundTag();
        firearms.putInt("shotgun_fire_cooldown_ticks", shotgunFireCooldownTicks);
        firearms.putInt("crossbow_fire_cooldown_ticks", crossbowFireCooldownTicks);
        firearms.putBoolean("shotgun_hits_endermen", shotgunHitsEndermen);
        firearms.putBoolean("shotgun_disables_shields", shotgunDisablesShields);
        root.put("firearms", firearms);

        CompoundTag general = new CompoundTag();
        general.putBoolean("spawn_player_zombie", spawnPlayerZombie);
        general.putBoolean("no_death_penalty", noDeathPenalty);
        general.putBoolean("no_increased_mob_seek_range", noIncreasedMobSeekRange);
        general.putBoolean("mob_react_only_on_shooting", mobReactOnlyOnShooting);
        general.putBoolean("medkit_instant_healing", medkitInstantHealing);
        general.putBoolean("disable_medkit_recipe", disableMedkitRecipe);
        general.putBoolean("disable_shotgun_recipe", disableShotgunRecipe);
        root.put("general", general);
        return root;
    }

    /**
     * Applies a server-authored snapshot after validating every field.  On a
     * remote connection the values stay locked until logout, so editing or
     * reloading iblis-common.toml cannot affect client-side gameplay hooks.
     */
    public static synchronized boolean applyServerSnapshot(
            CompoundTag root, boolean lockToRemoteServer) {
        if (root.getInt("version") != GAMEPLAY_SYNC_VERSION
                || !root.contains("skills", Tag.TAG_COMPOUND)
                || !root.contains("characteristics", Tag.TAG_COMPOUND)
                || !root.contains("progression", Tag.TAG_COMPOUND)
                || !root.contains("general", Tag.TAG_COMPOUND)) {
            return false;
        }

        CompoundTag skillTags = root.getCompound("skills");
        boolean[] skillEnabled = new boolean[PlayerSkill.values().length];
        double[] skillCaps = new double[skillEnabled.length];
        double[] skillPoints = new double[skillEnabled.length];
        for (PlayerSkill skill : PlayerSkill.values()) {
            if (!skillTags.contains(skill.name(), Tag.TAG_COMPOUND)) {
                return false;
            }
            CompoundTag value = skillTags.getCompound(skill.name());
            if (!validBoolean(value, "enabled")
                    || !validNumber(value, "cap", 0.0, 1000.0)
                    || !validNumber(value, "points_per_action", 0.0, 1000.0)) {
                return false;
            }
            int index = skill.ordinal();
            skillEnabled[index] = value.getBoolean("enabled");
            skillCaps[index] = value.getDouble("cap");
            skillPoints[index] = value.getDouble("points_per_action");
        }

        CompoundTag characteristicTags = root.getCompound("characteristics");
        boolean[] characteristicEnabled =
                new boolean[PlayerCharacteristic.values().length];
        double[] characteristicStarts = new double[characteristicEnabled.length];
        double[] characteristicPoints = new double[characteristicEnabled.length];
        double[] characteristicCaps = new double[characteristicEnabled.length];
        for (PlayerCharacteristic characteristic : PlayerCharacteristic.values()) {
            if (!characteristicTags.contains(characteristic.name(), Tag.TAG_COMPOUND)) {
                return false;
            }
            CompoundTag value = characteristicTags.getCompound(characteristic.name());
            if (!validBoolean(value, "enabled")
                    || !validNumber(value, "start_level", 0.0, 100.0)
                    || !validNumber(value, "points_per_level", 0.0, 100.0)
                    || !validNumber(value, "cap", 0.0, 1000.0)) {
                return false;
            }
            int index = characteristic.ordinal();
            characteristicEnabled[index] = value.getBoolean("enabled");
            characteristicStarts[index] = value.getDouble("start_level");
            characteristicPoints[index] = value.getDouble("points_per_level");
            characteristicCaps[index] = value.getDouble("cap");
        }

        CompoundTag progression = root.getCompound("progression");
        if (!validNumber(progression, "skill_training_base_resistance", 0.01, 1000.0)
                || !validNumber(progression, "skill_training_linear_resistance", 0.0, 1000.0)
                || !validNumber(progression, "skill_training_quadratic_resistance", 0.0, 1000.0)
                || !validNumber(progression, "bonus_base_multiplier", 0.0, 1000.0)
                || !validNumber(progression, "bonus_soft_cap_scale", 0.01, 100000.0)
                || !validNumber(progression, "characteristic_cost_base_multiplier", 0.0, 1000.0)
                || !validInteger(progression, "characteristic_cost_mid_start_level", 1, 100000)
                || !validNumber(progression, "characteristic_cost_mid_multiplier", 0.0, 1000.0)
                || !validInteger(progression, "characteristic_cost_late_start_level", 1, 100000)
                || !validNumber(progression, "characteristic_cost_late_multiplier", 0.0, 1000.0)) {
            return false;
        }
        double newSkillTrainingBaseResistance =
                progression.getDouble("skill_training_base_resistance");
        double newSkillTrainingLinearResistance =
                progression.getDouble("skill_training_linear_resistance");
        double newSkillTrainingQuadraticResistance =
                progression.getDouble("skill_training_quadratic_resistance");
        double newBonusBaseMultiplier = progression.getDouble("bonus_base_multiplier");
        double newBonusSoftCapScale = progression.getDouble("bonus_soft_cap_scale");
        double newCharacteristicCostBaseMultiplier =
                progression.getDouble("characteristic_cost_base_multiplier");
        int newCharacteristicCostMidStartLevel =
                progression.getInt("characteristic_cost_mid_start_level");
        double newCharacteristicCostMidMultiplier =
                progression.getDouble("characteristic_cost_mid_multiplier");
        int newCharacteristicCostLateStartLevel =
                progression.getInt("characteristic_cost_late_start_level");
        double newCharacteristicCostLateMultiplier =
                progression.getDouble("characteristic_cost_late_multiplier");

        int newShotgunFireCooldownTicks = LEGACY_SHOTGUN_FIRE_COOLDOWN_TICKS;
        int newCrossbowFireCooldownTicks = LEGACY_CROSSBOW_FIRE_COOLDOWN_TICKS;
        boolean newShotgunHitsEndermen = true;
        boolean newShotgunDisablesShields = true;
        if (root.contains("firearms")) {
            if (!root.contains("firearms", Tag.TAG_COMPOUND)) {
                return false;
            }
            CompoundTag firearms = root.getCompound("firearms");
            if (!validInteger(firearms, "shotgun_fire_cooldown_ticks", 0, 1200)
                    || !validInteger(firearms, "crossbow_fire_cooldown_ticks", 0, 1200)) {
                return false;
            }
            newShotgunFireCooldownTicks = firearms.getInt("shotgun_fire_cooldown_ticks");
            newCrossbowFireCooldownTicks = firearms.getInt("crossbow_fire_cooldown_ticks");
            if (firearms.contains("shotgun_hits_endermen")) {
                if (!validBoolean(firearms, "shotgun_hits_endermen")) {
                    return false;
                }
                newShotgunHitsEndermen = firearms.getBoolean("shotgun_hits_endermen");
            }
            if (firearms.contains("shotgun_disables_shields")) {
                if (!validBoolean(firearms, "shotgun_disables_shields")) {
                    return false;
                }
                newShotgunDisablesShields = firearms.getBoolean("shotgun_disables_shields");
            }
        }

        CompoundTag general = root.getCompound("general");
        String[] generalBooleans = {
                "spawn_player_zombie", "no_death_penalty",
                "no_increased_mob_seek_range", "mob_react_only_on_shooting",
                "medkit_instant_healing", "disable_medkit_recipe",
                "disable_shotgun_recipe"
        };
        for (String key : generalBooleans) {
            if (!validBoolean(general, key)) {
                return false;
            }
        }

        for (PlayerSkill skill : PlayerSkill.values()) {
            int index = skill.ordinal();
            skill.enabled = skillEnabled[index];
            skill.cap = skillCaps[index];
            skill.pointsPerAction = skillPoints[index];
        }
        for (PlayerCharacteristic characteristic : PlayerCharacteristic.values()) {
            int index = characteristic.ordinal();
            characteristic.enabled = characteristicEnabled[index];
            characteristic.startLevel = characteristicStarts[index];
            characteristic.pointsPerLevel = characteristicPoints[index];
            characteristic.cap = characteristicCaps[index];
        }
        skillTrainingBaseResistance = newSkillTrainingBaseResistance;
        skillTrainingLinearResistance = newSkillTrainingLinearResistance;
        skillTrainingQuadraticResistance = newSkillTrainingQuadraticResistance;
        bonusBaseMultiplier = newBonusBaseMultiplier;
        bonusSoftCapScale = newBonusSoftCapScale;
        characteristicCostBaseMultiplier = newCharacteristicCostBaseMultiplier;
        characteristicCostMidStartLevel = newCharacteristicCostMidStartLevel;
        characteristicCostMidMultiplier = newCharacteristicCostMidMultiplier;
        characteristicCostLateStartLevel = newCharacteristicCostLateStartLevel;
        characteristicCostLateMultiplier = newCharacteristicCostLateMultiplier;
        shotgunFireCooldownTicks = newShotgunFireCooldownTicks;
        crossbowFireCooldownTicks = newCrossbowFireCooldownTicks;
        shotgunHitsEndermen = newShotgunHitsEndermen;
        shotgunDisablesShields = newShotgunDisablesShields;
        spawnPlayerZombie = general.getBoolean("spawn_player_zombie");
        noDeathPenalty = general.getBoolean("no_death_penalty");
        noIncreasedMobSeekRange = general.getBoolean("no_increased_mob_seek_range");
        mobReactOnlyOnShooting = general.getBoolean("mob_react_only_on_shooting");
        medkitInstantHealing = general.getBoolean("medkit_instant_healing");
        disableMedkitRecipe = general.getBoolean("disable_medkit_recipe");
        disableShotgunRecipe = general.getBoolean("disable_shotgun_recipe");
        remoteServerAuthority = lockToRemoteServer;
        return true;
    }

    public static synchronized void clearServerSnapshot() {
        if (!remoteServerAuthority) {
            return;
        }
        remoteServerAuthority = false;
        bake();
    }

    public static boolean hasRemoteServerAuthority() {
        return remoteServerAuthority;
    }

    private static boolean validBoolean(CompoundTag tag, String key) {
        return tag.contains(key, Tag.TAG_BYTE);
    }

    private static boolean validNumber(
            CompoundTag tag, String key, double minimum, double maximum) {
        if (!tag.contains(key, Tag.TAG_ANY_NUMERIC)) {
            return false;
        }
        double value = tag.getDouble(key);
        return Double.isFinite(value) && value >= minimum && value <= maximum;
    }

    private static boolean validInteger(
            CompoundTag tag, String key, int minimum, int maximum) {
        if (!tag.contains(key, Tag.TAG_INT)) {
            return false;
        }
        int value = tag.getInt(key);
        return value >= minimum && value <= maximum;
    }

    public static List<EditableConfigCategory> editableCategories() {
        return List.of(
                new EditableConfigCategory("iblis.generalConfig", SPEC, List.of(
                        EditableConfigValue.booleanValue("spawn_player_zombie", SPAWN_PLAYER_ZOMBIE),
                        EditableConfigValue.booleanValue("no_death_penalty", NO_DEATH_PENALTY),
                        EditableConfigValue.booleanValue("no_increased_mob_seek_range", NO_INCREASED_MOB_SEEK_RANGE),
                        EditableConfigValue.booleanValue("mob_react_only_on_shooting", MOB_REACT_ONLY_ON_SHOOTING),
                        EditableConfigValue.booleanValue("medkit_instant_healing", MEDKIT_INSTANT_HEALING),
                        EditableConfigValue.booleanValue("toggle_sprint_by_sprint_button", TOGGLE_SPRINT),
                        EditableConfigValue.booleanValue("render_hp_bar", RENDER_HP_BAR),
                        EditableConfigValue.booleanValue("disable_medkit_recipe", DISABLE_MEDKIT_RECIPE),
                        EditableConfigValue.booleanValue("disable_shotgun_recipe", DISABLE_SHOTGUN_RECIPE))),
                new EditableConfigCategory("iblis.firearmBalanceConfig", SPEC, List.of(
                        EditableConfigValue.numberValue("shotgun_fire_cooldown_ticks",
                                SHOTGUN_FIRE_COOLDOWN_TICKS, 0.0, 1200.0),
                        EditableConfigValue.numberValue("crossbow_fire_cooldown_ticks",
                                CROSSBOW_FIRE_COOLDOWN_TICKS, 0.0, 1200.0),
                        EditableConfigValue.booleanValue(
                                "shotgun_hits_endermen", SHOTGUN_HITS_ENDERMEN),
                        EditableConfigValue.booleanValue(
                                "shotgun_disables_shields", SHOTGUN_DISABLES_SHIELDS))),
                new EditableConfigCategory("iblis.progressionBalanceConfig", SPEC, List.of(
                        EditableConfigValue.numberValue("skill_training_base_resistance",
                                SKILL_TRAINING_BASE_RESISTANCE, 0.01, 1000.0),
                        EditableConfigValue.numberValue("skill_training_linear_resistance",
                                SKILL_TRAINING_LINEAR_RESISTANCE, 0.0, 1000.0),
                        EditableConfigValue.numberValue("skill_training_quadratic_resistance",
                                SKILL_TRAINING_QUADRATIC_RESISTANCE, 0.0, 1000.0),
                        EditableConfigValue.numberValue("bonus_base_multiplier",
                                BONUS_BASE_MULTIPLIER, 0.0, 1000.0),
                        EditableConfigValue.numberValue("bonus_soft_cap_scale",
                                BONUS_SOFT_CAP_SCALE, 0.01, 100000.0),
                        EditableConfigValue.numberValue("characteristic_cost_base_multiplier",
                                CHARACTERISTIC_COST_BASE_MULTIPLIER, 0.0, 1000.0),
                        EditableConfigValue.numberValue("characteristic_cost_mid_start_level",
                                CHARACTERISTIC_COST_MID_START_LEVEL, 1.0, 100000.0),
                        EditableConfigValue.numberValue("characteristic_cost_mid_multiplier",
                                CHARACTERISTIC_COST_MID_MULTIPLIER, 0.0, 1000.0),
                        EditableConfigValue.numberValue("characteristic_cost_late_start_level",
                                CHARACTERISTIC_COST_LATE_START_LEVEL, 1.0, 100000.0),
                        EditableConfigValue.numberValue("characteristic_cost_late_multiplier",
                                CHARACTERISTIC_COST_LATE_MULTIPLIER, 0.0, 1000.0))),
                enumBooleanCategory("iblis.characteristicsDisablingConfig", CHARACTERISTIC_ENABLED),
                enumNumberCategory("iblis.characteristicsStartLevelConfig", CHARACTERISTIC_START, 0.0, 100.0),
                enumNumberCategory("iblis.characteristicsPointsPerLevelConfig", CHARACTERISTIC_POINTS, 0.0, 100.0),
                enumNumberCategory("iblis.characteristicsCapConfig", CHARACTERISTIC_CAP, 0.0, 1000.0),
                enumBooleanCategory("iblis.skillsConfig", SKILL_ENABLED),
                enumNumberCategory("iblis.skillsCapConfig", SKILL_CAP, 0.0, 1000.0),
                enumNumberCategory("iblis.skillsXPConfig", SKILL_XP, 0.0, 1000.0));
    }

    /** Client preferences remain editable while connected to a remote server. */
    public static List<EditableConfigCategory> clientEditableCategories() {
        return List.of(new EditableConfigCategory("iblis.generalConfig", SPEC, List.of(
                EditableConfigValue.booleanValue(
                        "toggle_sprint_by_sprint_button", TOGGLE_SPRINT),
                EditableConfigValue.booleanValue("render_hp_bar", RENDER_HP_BAR))));
    }

    private static <E extends Enum<E>> EditableConfigCategory enumBooleanCategory(
            String titleKey, Map<E, ForgeConfigSpec.BooleanValue> values) {
        List<EditableConfigValue> editable = new ArrayList<>(values.size());
        values.forEach((key, value) -> editable.add(
                EditableConfigValue.booleanValue(key.name().toLowerCase(), value)));
        return new EditableConfigCategory(titleKey, SPEC, List.copyOf(editable));
    }

    private static <E extends Enum<E>> EditableConfigCategory enumNumberCategory(
            String titleKey, Map<E, ForgeConfigSpec.DoubleValue> values,
            double minimum, double maximum) {
        List<EditableConfigValue> editable = new ArrayList<>(values.size());
        values.forEach((key, value) -> editable.add(
                EditableConfigValue.numberValue(
                        key.name().toLowerCase(), value, minimum, maximum)));
        return new EditableConfigCategory(titleKey, SPEC, List.copyOf(editable));
    }
}
