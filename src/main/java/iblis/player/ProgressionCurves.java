package iblis.player;

import iblis.config.IblisConfig;
import iblis.config.Legacy112Feature;

/** Shared, monotonic curves for progression cost and gameplay bonuses. */
final class ProgressionCurves {
    private ProgressionCurves() {
    }

    static double skillTrainingResistance(double currentValue) {
        double value = Math.max(currentValue, 0.0);
        if (IblisConfig.useLegacy112(Legacy112Feature.SKILL_TRAINING_CURVE)) {
            return value * 0.5 + 1.0;
        }
        return Math.max(0.000001,
                IblisConfig.skillTrainingBaseResistance
                        + value * IblisConfig.skillTrainingLinearResistance
                        + value * value * IblisConfig.skillTrainingQuadraticResistance);
    }

    static double effectiveSkillBonus(double rawValue) {
        double value = Math.max(rawValue, 0.0);
        if (IblisConfig.useLegacy112(Legacy112Feature.SKILL_BONUS_CURVE)) {
            return value;
        }
        return value * bonusMultiplier(value);
    }

    static double effectiveCharacteristicBonus(double rawValue, int investedLevels) {
        if (IblisConfig.useLegacy112(Legacy112Feature.MELEE_CHARACTERISTIC_BONUS)) {
            return Math.max(rawValue, 0.0);
        }
        return Math.max(rawValue, 0.0) * bonusMultiplier(Math.max(investedLevels, 0));
    }

    static int characteristicExperienceCost(int level) {
        int safeLevel = Math.max(level, 1);
        if (IblisConfig.useLegacy112(Legacy112Feature.CHARACTERISTIC_XP_COST)) {
            return safeLevel;
        }
        double cost = safeLevel * IblisConfig.characteristicCostBaseMultiplier;
        cost += Math.max(0, safeLevel - IblisConfig.characteristicCostMidStartLevel)
                * IblisConfig.characteristicCostMidMultiplier;
        cost += Math.max(0, safeLevel - IblisConfig.characteristicCostLateStartLevel)
                * IblisConfig.characteristicCostLateMultiplier;
        return cost >= Integer.MAX_VALUE ? Integer.MAX_VALUE
                : (int) Math.ceil(Math.max(cost, 0.0));
    }

    private static double bonusMultiplier(double progressionValue) {
        return IblisConfig.bonusBaseMultiplier
                / (1.0 + progressionValue / IblisConfig.bonusSoftCapScale);
    }
}
