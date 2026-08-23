package iblis.effect;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

public final class MedkitEffectInstance extends MobEffectInstance {
    private final int applicationFrequency;

    public MedkitEffectInstance(double medicalAidSkill) {
        super(MobEffects.REGENERATION, 600, 5);
        applicationFrequency = 1 + (int) (512.0 / (medicalAidSkill + 1.0));
    }

    @Override
    public boolean tick(LivingEntity entity, Runnable onExpiration) {
        return entity.getHealth() < entity.getMaxHealth() && super.tick(entity, onExpiration);
    }

    @Override
    public void applyEffect(LivingEntity entity) {
        if (getDuration() % applicationFrequency == 0) {
            super.applyEffect(entity);
        }
    }
}
