package iblis.item;

import iblis.config.IblisConfig;
import iblis.config.Legacy112Feature;
import iblis.entity.CrossbowBoltEntity;
import iblis.registry.IblisAttributes;
import iblis.registry.IblisItems;
import iblis.registry.IblisSounds;
import iblis.util.FirearmDamageRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class CrossbowItem extends FirearmItem {
    public CrossbowItem(Properties properties) {
        super(properties);
    }

    @Override
    protected void fire(ServerLevel level, Vec3 aim, ServerPlayer player, boolean critical,
                        double accuracy, float ammoDamage, int ammoType) {
        float pitch = player.getXRot() - 1.0F;
        float yaw = player.getYRot();
        Vec3 position = player.getEyePosition();
        if (!player.isUsingItem()) {
            Vec3 offset = Vec3.directionFromRotation(pitch + 4.0F, yaw + 4.0F);
            position = position.add(offset);
            pitch--;
            yaw--;
        }
        CrossbowBoltEntity bolt = new CrossbowBoltEntity(
                level, player, position.x, position.y, position.z);
        float speed = 8.0F + (float) player.getDeltaMovement().length();
        bolt.shootFromRotation(player, pitch, yaw, 0.0F, speed, (float) (10.0 / accuracy));
        double damage = player.getAttributeValue(IblisAttributes.PROJECTILE_DAMAGE.get()) * ammoDamage;
        boolean legacyLuckyShot = critical && IblisConfig.useLegacy112(
                Legacy112Feature.FIREARM_LUCKY_SHOT_DAMAGE);
        bolt.setBaseDamage(critical ? damage * FirearmDamageRules.luckyShotMultiplier() : damage);
        bolt.setLegacyLuckyShot(legacyLuckyShot);
        level.addFreshEntity(bolt);
        player.resetAttackStrengthTicker();
        playDropStringSound(player);
    }

    @Override
    protected int baseFireCooldownTicks() {
        return IblisConfig.useLegacy112(Legacy112Feature.CROSSBOW_FIRE_COOLDOWN)
                ? 0 : IblisConfig.crossbowFireCooldownTicks;
    }

    @Override
    public ItemStack toReloading(ItemStack stack) {
        stack.getOrCreateTag();
        return transfer(IblisItems.CROSSBOW_RELOADING.get(), stack);
    }

    @Override
    public void playReloadingSound(Player player) {
    }

    @Override
    protected void playDropStringSound(Player player) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                IblisSounds.CROSSBOW_SHOT.get(), SoundSource.PLAYERS,
                1.0F, player.getRandom().nextFloat() * 0.2F + 0.8F);
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return repairCandidate.is(ItemTags.PLANKS) || super.isValidRepairItem(stack, repairCandidate);
    }
}
