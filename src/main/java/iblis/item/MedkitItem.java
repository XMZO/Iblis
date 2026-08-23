package iblis.item;

import iblis.config.IblisConfig;
import iblis.effect.MedkitEffectInstance;
import iblis.player.PlayerSkill;
import iblis.registry.IblisSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public final class MedkitItem extends Item {
    private static final int USE_DURATION = 128;

    public MedkitItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.hasEffect(MobEffects.REGENERATION) || player.getHealth() >= player.getMaxHealth()) {
            return InteractionResultHolder.fail(stack);
        }
        if (IblisConfig.medkitInstantHealing) {
            if (!level.isClientSide) {
                damageAndTrain(stack, player, hand);
                player.heal((float) PlayerSkill.MEDICAL_AID.getFullValue(player) + 1.0F);
                play(level, player, IblisSounds.TEARING_BANDAGE.get());
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return IblisConfig.medkitInstantHealing ? 0 : USE_DURATION;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remainingTicks) {
        if (remainingTicks == USE_DURATION - 2) {
            play(level, user, IblisSounds.OPENING_MEDKIT.get());
        } else if (remainingTicks == USE_DURATION * 3 / 4) {
            play(level, user, IblisSounds.FULL_BOTTLE_SHAKING.get());
        } else if (remainingTicks == USE_DURATION / 2) {
            play(level, user, IblisSounds.SCISSORS_CLICKING.get());
        } else if (remainingTicks == USE_DURATION / 4) {
            play(level, user, IblisSounds.TEARING_BANDAGE.get());
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        play(level, user, IblisSounds.CLOSING_MEDKIT.get());
        if (!level.isClientSide && user instanceof Player player) {
            applyTreatment(stack, player, player, player.getUsedItemHand());
        }
        return stack;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target,
                                                   InteractionHand hand) {
        if (target instanceof Enemy) {
            return InteractionResult.PASS;
        }
        if (!player.level().isClientSide) {
            applyTreatment(stack, player, target, hand);
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    private static void applyTreatment(ItemStack stack, Player healer, LivingEntity target,
                                       InteractionHand hand) {
        double skill = PlayerSkill.MEDICAL_AID.getFullValue(healer);
        damageAndTrain(stack, healer, hand);
        target.addEffect(new MedkitEffectInstance(skill));
    }

    private static void damageAndTrain(ItemStack stack, Player player, InteractionHand hand) {
        stack.hurtAndBreak(1, player, broken -> broken.broadcastBreakEvent(hand));
        player.causeFoodExhaustion(1.0F);
        PlayerSkill.MEDICAL_AID.raise(player, 1.0);
    }

    private static void play(Level level, LivingEntity entity, SoundEvent sound) {
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), sound,
                SoundSource.PLAYERS, 1.0F, level.random.nextFloat() * 0.2F + 0.8F);
    }
}
