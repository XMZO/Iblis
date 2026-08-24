package iblis.config;

import java.util.HashMap;
import java.util.Map;

/** Optional, independent overrides for intentional 1.12.2 gameplay behavior. */
public enum Legacy112Feature {
    SKILL_TRAINING_CURVE(
            "skill_training_curve",
            "Use the 1.12.2 linear skill-training slowdown; progression resistance settings are ignored."),
    SKILL_BONUS_CURVE(
            "skill_bonus_curve",
            "Use raw 1.12.2 skill bonuses without the modern diminishing-return curve."),
    MELEE_CHARACTERISTIC_BONUS(
            "melee_characteristic_bonus",
            "Use the raw 1.12.2 melee characteristic bonus without diminishing returns."),
    CHARACTERISTIC_XP_COST(
            "characteristic_xp_cost",
            "Make each characteristic upgrade cost its displayed level, as in 1.12.2."),
    SHOTGUN_FIRE_COOLDOWN(
            "shotgun_fire_cooldown",
            "Remove the added shotgun firing cooldown; per-stack safety and dry-fire sound limits remain."),
    CROSSBOW_FIRE_COOLDOWN(
            "crossbow_fire_cooldown",
            "Remove the added double-crossbow firing cooldown; per-stack safety remains."),
    SHOTGUN_RELOAD_TIMING(
            "shotgun_reload_timing",
            "Use the 1.12.2 skill-based per-shell reload timing and attack-recovery finish delay."),
    FIREARM_AIMING_SPEED(
            "firearm_aiming_speed",
            "Use the 1.12.2 aiming speed without modern Attack Speed, Intelligence or Luck acceleration."),
    FIREARM_LUCKY_SHOT_DAMAGE(
            "firearm_lucky_shot_damage",
            "Restore 100x lucky-shot damage against non-bosses; boss one-shot safeguards remain."),
    BOSS_FIREARM_DAMAGE(
            "boss_firearm_damage",
            "Remove the modern Boss base-damage reduction and headshot cap; duplicate-hit fixes remain."),
    SHOTGUN_ENTITY_INTERACTIONS(
            "shotgun_entity_interactions",
            "Do not let shotgun hits break boats or other non-living fragile targets, matching 1.12.2."),
    SHOTGUN_BLOCK_INTERACTIONS(
            "shotgun_block_interactions",
            "Use old shotgun block rules: pass through leaves and break only fragile glass or ice."),
    CROSSBOW_BLOCK_INTERACTIONS(
            "crossbow_block_interactions",
            "Disable the added crossbow block breaking and penetration, matching 1.12.2."),
    SHOTGUN_RECIPE(
            "shotgun_recipe",
            "Use the cheaper 1.12.2 shotgun recipe. Run /reload or restart after changing recipe options."),
    TRIGGER_SPRING_RECIPE(
            "trigger_spring_recipe",
            "Use the two-nugget 1.12.2 spring recipe. Run /reload or restart after changing it."),
    SHOTGUN_BULLET_RECIPE(
            "shotgun_bullet_recipe",
            "Use the 1.12.2 bullet recipe and 32-round output. Run /reload or restart after changing it."),
    SHOTGUN_SHOT_RECIPE(
            "shotgun_shot_recipe",
            "Use the 1.12.2 shot recipe and 32-round output. Run /reload or restart after changing it."),
    CROSSBOW_BOLT_RECIPE(
            "crossbow_bolt_recipe",
            "Use an iron nugget for the 1.12.2 bolt recipe. Run /reload or restart after changing it."),
    BOULDER_RECIPES(
            "boulder_recipes",
            "Use the 1.12.2 cobblestone conversion: one block makes nine boulders and nine rebuild it.");

    private static final Map<String, Legacy112Feature> BY_KEY = createLookup();

    private final String key;
    private final String comment;

    Legacy112Feature(String key, String comment) {
        this.key = key;
        this.comment = comment;
    }

    public String key() {
        return key;
    }

    public String comment() {
        return comment;
    }

    public static Legacy112Feature byKey(String key) {
        return BY_KEY.get(key);
    }

    private static Map<String, Legacy112Feature> createLookup() {
        Map<String, Legacy112Feature> values = new HashMap<>();
        for (Legacy112Feature feature : Legacy112Feature.values()) {
            values.put(feature.key, feature);
        }
        return Map.copyOf(values);
    }
}
