package iblis.player;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.entity.player.Player;

public final class ExtendedFoodData extends FoodData {
    public static final int DEFAULT_MAX_FOOD_LEVEL = 20;

    private int maxFood = DEFAULT_MAX_FOOD_LEVEL;

    public ExtendedFoodData(FoodData original) {
        CompoundTag data = new CompoundTag();
        original.addAdditionalSaveData(data);
        readAdditionalSaveData(data);
    }

    @Override
    public void eat(int nutrition, float saturationModifier) {
        setFoodLevel(Math.min(nutrition + getFoodLevel(), maxFood));
        setSaturation(Math.min(
                getSaturationLevel() + nutrition * saturationModifier,
                getFoodLevel()));
    }

    @Override
    public void tick(Player player) {
        maxFood = Math.max(0, Mth.floor(PlayerCharacteristic.GLUTTONY.getCurrentValue(player)));
        super.tick(player);
    }

    @Override
    public boolean needsFood() {
        return getFoodLevel() < maxFood;
    }

    public int getMaxFoodLevel() {
        return maxFood;
    }
}
