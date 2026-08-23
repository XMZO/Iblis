package iblis.player;

import iblis.config.IblisConfig;

/** Shared, monotonic curves for progression cost and gameplay bonuses. */
final class ProgressionCurves {
    private ProgressionCurves() {
    }

    static double skillTrainingResistance(double currentValue) {
        double value = Math.max(currentValue, 0.0);
        return Math.max(0.000001,
                IblisConfig.skillTrainingBaseResistance
                        + value * IblisConfig.skillTrainingLinearResistance
                        + value * value * IblisConfig.skillTrainingQuadraticResistance);
    }

    static double effectiveSkillBonus(double rawValue) {
        double value = Math.max(rawValue, 0.0);
        return value * bonusMultiplier(value);
    }

    static double effectiveCharacteristicBonus(double rawValue, int investedLevels) {
        return Math.max(rawValue, 0.0) * bonusMultiplier(Math.max(investedLevels, 0));
    }

    static int characteristicExperienceCost(int level) {
        int safeLevel = Math.max(level, 1);
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
