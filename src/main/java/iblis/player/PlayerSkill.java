package iblis.player;

import iblis.registry.IblisAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;

public enum PlayerSkill {
    BOXING(IblisAttributes.BOXING, 0.005, IblisAttributes.MARTIAL_ARTS, IblisAttributes.WISDOM),
    SWORDSMANSHIP(IblisAttributes.SWORDSMANSHIP, 0.001, IblisAttributes.MARTIAL_ARTS, IblisAttributes.WISDOM),
    PARRY(IblisAttributes.PARRY, 0.001, IblisAttributes.MARTIAL_ARTS, IblisAttributes.WISDOM),
    ARCHERY(IblisAttributes.ARCHERY, 0.001, IblisAttributes.MARTIAL_ARTS, IblisAttributes.WISDOM),
    THROWING(IblisAttributes.THROWING, 0.0002, IblisAttributes.MARTIAL_ARTS, IblisAttributes.WISDOM),
    SHARPSHOOTING(IblisAttributes.SHARPSHOOTING, 0.001, IblisAttributes.MARTIAL_ARTS, IblisAttributes.WISDOM),
    ARMORSMITH(IblisAttributes.ARMORSMITH, 0.02, IblisAttributes.CRAFTMANSHIP, IblisAttributes.WISDOM),
    WEAPONSMITH(IblisAttributes.WEAPONSMITH, 0.02, IblisAttributes.CRAFTMANSHIP, IblisAttributes.WISDOM),
    MECHANICS(IblisAttributes.MECHANICS, 0.02, IblisAttributes.CRAFTMANSHIP, IblisAttributes.WISDOM),
    MEDICAL_AID(IblisAttributes.MEDICAL_AID, 0.02, IblisAttributes.CRAFTMANSHIP, IblisAttributes.WISDOM),
    DIGGING(IblisAttributes.DIGGING, 0.002, IblisAttributes.CRAFTMANSHIP, IblisAttributes.WISDOM),
    CHEMISTRY(IblisAttributes.CHEMISTRY, 0.02, IblisAttributes.CRAFTMANSHIP, IblisAttributes.WISDOM),
    RUNNING(IblisAttributes.RUNNING, 0.0001, IblisAttributes.ACROBATICS, IblisAttributes.WISDOM),
    JUMPING(IblisAttributes.JUMPING, 0.001, IblisAttributes.ACROBATICS, IblisAttributes.WISDOM),
    FALLING(IblisAttributes.FALLING, 0.02, IblisAttributes.ACROBATICS, IblisAttributes.WISDOM),
    EQUILIBRIUM(IblisAttributes.EQUILIBRIUM, 0.001, IblisAttributes.ACROBATICS, IblisAttributes.WISDOM);

    private final Supplier<Attribute> attribute;
    private final List<Supplier<Attribute>> progressionPath;
    public final double defaultPointsPerAction;
    public volatile double pointsPerAction;
    public volatile double cap = 1000.0;
    public volatile boolean enabled = true;

    @SafeVarargs
    PlayerSkill(Supplier<Attribute> attribute, double pointsPerAction, Supplier<Attribute>... parents) {
        this.attribute = attribute;
        this.defaultPointsPerAction = pointsPerAction;
        this.pointsPerAction = pointsPerAction;

        List<Supplier<Attribute>> path = new ArrayList<>(parents.length + 1);
        path.add(attribute);
        Collections.addAll(path, parents);
        this.progressionPath = List.copyOf(path);
    }

    public void raise(Player player, double actionValue) {
        AttributeInstance skillInstance = getAttributeInstance(player);
        if (skillInstance.getBaseValue() >= cap) {
            return;
        }
        if (player.level().isClientSide) {
            throw new IllegalStateException("Skills must only be raised on the logical server");
        }

        int divider = 1;
        for (int i = 0; i < progressionPath.size(); i++) {
            AttributeInstance instance = i == 0
                    ? skillInstance : instance(player, progressionPath.get(i).get());
            double value = instance.getBaseValue();
            value += pointsPerAction * actionValue / divider
                    / ProgressionCurves.skillTrainingResistance(value);
            instance.setBaseValue(value);
            divider <<= 2;
        }
    }

    public void raiseTo(Player player, double targetValue) {
        if (player.level().isClientSide) {
            throw new IllegalStateException("Skills must only be raised on the logical server");
        }
        AttributeInstance skillInstance = getAttributeInstance(player);
        double difference = targetValue - skillInstance.getBaseValue();
        int divider = 1;
        for (int i = 0; i < progressionPath.size(); i++) {
            AttributeInstance instance = i == 0
                    ? skillInstance : instance(player, progressionPath.get(i).get());
            instance.setBaseValue(instance.getBaseValue() + difference / divider);
            divider <<= 2;
        }
    }

    public double getCurrentValue(Player player) {
        return getAttributeInstance(player).getBaseValue();
    }

    public double getProgressForAction(Player player, double actionValue) {
        return pointsPerAction * actionValue
                / ProgressionCurves.skillTrainingResistance(getCurrentValue(player));
    }

    public double getFullValue(Player player) {
        return ProgressionCurves.effectiveSkillBonus(getRawFullValue(player));
    }

    /** Raw combined proficiency used by crafting requirements and item quality. */
    public double getRawFullValue(Player player) {
        if (!enabled) {
            return 0.0;
        }

        double value = instance(player, IblisAttributes.INTELLIGENCE.get()).getValue();
        for (Supplier<Attribute> pathEntry : progressionPath) {
            value += instance(player, pathEntry.get()).getValue();
        }
        return value;
    }

    public Attribute getAttribute() {
        return attribute.get();
    }

    public AttributeInstance getAttributeInstance(Player player) {
        return instance(player, getAttribute());
    }

    private static AttributeInstance instance(Player player, Attribute attribute) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            throw new IllegalStateException(
                    "Missing Iblis player attribute " + attribute.getDescriptionId());
        }
        return instance;
    }
}
