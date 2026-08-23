package iblis.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import iblis.entity.BoulderEntity;
import iblis.entity.ThrowingKnifeEntity;
import iblis.player.PlayerSkill;
import java.util.List;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class ThrowingWeaponItem extends AmmoItem {
    private final Kind kind;

    public ThrowingWeaponItem(Properties properties, Kind kind) {
        super(properties, kind.damage, kind.ordinal());
        this.kind = kind;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<net.minecraft.network.chat.Component> tooltip, TooltipFlag flag) {
        if (kind == Kind.IRON_KNIFE) {
            TooltipComponents.addQuality(tooltip, getQuality(stack));
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL, 0.5F,
                0.4F / (player.getRandom().nextFloat() * 0.4F + 0.8F));
        if (!level.isClientSide) {
            Vec3 handOffset = vectorForRotation(player.getXRot() - 15.0F, player.getYRot() + 9.0F);
            Projectile projectile = kind == Kind.BOULDER
                    ? new BoulderEntity(level, player, player.getX() + handOffset.x,
                    player.getY() + player.getEyeHeight() + handOffset.y, player.getZ() + handOffset.z)
                    : new ThrowingKnifeEntity(level, player, player.getX() + handOffset.x,
                    player.getY() + player.getEyeHeight() + handOffset.y, player.getZ() + handOffset.z);
            double skill = PlayerSkill.THROWING.getFullValue(player) + 1.0;
            float attack = player.getAttackStrengthScale(0.0F);
            float speed = (0.4F * attack + (float) skill * attack / 10.0F) / kind.weight;
            Vec3 movement = player.getDeltaMovement();
            speed += (float) movement.length();
            float pitch = player.getXRot();
            float yaw = player.getYRot() - 1.0F;
            float x = -Mth.sin(yaw * Mth.DEG_TO_RAD) * Mth.cos(pitch * Mth.DEG_TO_RAD);
            float y = -Mth.sin(pitch * Mth.DEG_TO_RAD);
            float z = Mth.cos(yaw * Mth.DEG_TO_RAD) * Mth.cos(pitch * Mth.DEG_TO_RAD);
            projectile.shoot(x + movement.x, y + movement.y, z + movement.z,
                    speed, (float) (10.0 / skill));
            level.addFreshEntity(projectile);
            player.resetAttackStrengthTicker();
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(
            EquipmentSlot slot, ItemStack stack) {
        if (slot != EquipmentSlot.MAINHAND) {
            return super.getAttributeModifiers(slot, stack);
        }
        return ImmutableMultimap.of(
                iblis.registry.IblisAttributes.PROJECTILE_DAMAGE.get(),
                new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", getAmmoDamage(stack),
                        AttributeModifier.Operation.ADDITION),
                Attributes.ATTACK_SPEED,
                new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", -1.4000000953674316,
                        AttributeModifier.Operation.ADDITION));
    }

    private static Vec3 vectorForRotation(float pitch, float yaw) {
        float cosYaw = Mth.cos(-yaw * Mth.DEG_TO_RAD - Mth.PI);
        float sinYaw = Mth.sin(-yaw * Mth.DEG_TO_RAD - Mth.PI);
        float horizontal = -Mth.cos(-pitch * Mth.DEG_TO_RAD);
        float vertical = Mth.sin(-pitch * Mth.DEG_TO_RAD);
        return new Vec3(sinYaw * horizontal, vertical, cosYaw * horizontal);
    }

    public enum Kind {
        BOULDER(2.0F, 1.0F),
        IRON_KNIFE(1.0F, 2.0F);

        private final float weight;
        private final float damage;

        Kind(float weight, float damage) {
            this.weight = weight;
            this.damage = damage;
        }
    }
}
