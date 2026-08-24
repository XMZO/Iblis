package iblis.player;

import iblis.registry.IblisAttributes;
import java.util.function.Supplier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

public enum PlayerCharacteristic {
    MAX_HP(() -> Attributes.MAX_HEALTH, 10.0, 2.0),
    MELEE_DAMAGE_REDUCTION(IblisAttributes.MELEE_DAMAGE_REDUCTION, 0.0, 0.1),
    FIRE_DAMAGE_REDUCTION(IblisAttributes.FIRE_DAMAGE_REDUCTION, 0.0, 0.1),
    EXPLOSION_DAMAGE_REDUCTION(IblisAttributes.EXPLOSION_DAMAGE_REDUCTION, 0.0, 0.1),
    PROJECTILE_DAMAGE_REDUCTION(IblisAttributes.PROJECTILE_DAMAGE_REDUCTION, 0.0, 0.1),
    MELEE_DAMAGE_BONUS(IblisAttributes.MELEE_DAMAGE_BONUS, 0.0, 0.1),
    ATTACK_SPEED(() -> Attributes.ATTACK_SPEED, 4.0, 0.1),
    LUCK(() -> Attributes.LUCK, 0.0, 0.1),
    INTELLIGENCE(IblisAttributes.INTELLIGENCE, 0.0, 0.1),
    GLUTTONY(IblisAttributes.GLUTTONY, 10.0, 1.0);

    private final Supplier<Attribute> attribute;
    public final double defaultStartLevel;
    public final double defaultPointsPerLevel;
    public volatile double startLevel;
    public volatile double pointsPerLevel;
    public volatile double cap = 1000.0;
    public volatile boolean enabled = true;

    PlayerCharacteristic(Supplier<Attribute> attribute, double startLevel, double pointsPerLevel) {
        this.attribute = attribute;
        this.defaultStartLevel = startLevel;
        this.defaultPointsPerLevel = pointsPerLevel;
        this.startLevel = startLevel;
        this.pointsPerLevel = pointsPerLevel;
    }

    public int getCurrentLevel(Player player) {
        double levels = getInvestedLevels(player);
        return levels >= Integer.MAX_VALUE - 1.0
                ? Integer.MAX_VALUE : (int) Math.round(levels) + 1;
    }

    /** Exact number of purchased upgrades, safe when a pack disables point gains. */
    public double getInvestedLevels(Player player) {
        double increment = pointsPerLevel;
        if (!Double.isFinite(increment) || increment <= 0.0) {
            return 0.0;
        }
        return Math.max(0.0, (getCurrentValue(player) - startLevel) / increment);
    }

    public boolean canRaise(Player player) {
        return enabled
                && getRequiredExperienceLevels(player) <= player.experienceLevel
                && getCurrentValue(player) < cap;
    }

    public boolean raise(Player player) {
        if (player.level().isClientSide) {
            return false;
        }
        if (!canRaise(player)) {
            return false;
        }

        player.giveExperienceLevels(-getRequiredExperienceLevels(player));
        AttributeInstance instance = getAttributeInstance(player);
        instance.setBaseValue(instance.getBaseValue() + pointsPerLevel);
        if (this == MELEE_DAMAGE_BONUS) {
            PlayerAttributeEffects.refreshMeleeDamageBonus(player);
        }
        return true;
    }

    public double getCurrentValue(Player player) {
        return getAttributeInstance(player).getBaseValue();
    }

    public int getRequiredExperienceLevels(Player player) {
        return ProgressionCurves.characteristicExperienceCost(getCurrentLevel(player));
    }

    public double getEffectiveBonus(Player player) {
        int investedLevels = Math.max(0, getCurrentLevel(player) - 1);
        return ProgressionCurves.effectiveCharacteristicBonus(
                getCurrentValue(player), investedLevels);
    }

    public void resetToDefault(Player player) {
        getAttributeInstance(player).setBaseValue(startLevel);
    }

    public Attribute getAttribute() {
        return attribute.get();
    }

    public AttributeInstance getAttributeInstance(Player player) {
        Attribute value = getAttribute();
        AttributeInstance instance = player.getAttribute(value);
        if (instance == null) {
            throw new IllegalStateException(
                    "Missing Iblis player characteristic " + value.getDescriptionId());
        }
        return instance;
    }
}
