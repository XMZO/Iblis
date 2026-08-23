package iblis.entity;

import iblis.player.PlayerSkill;
import iblis.registry.IblisAttributes;
import iblis.registry.IblisEntities;
import iblis.registry.IblisItems;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

public final class ThrowingKnifeEntity extends AbstractIblisArrow {
    private boolean hasRicocheted;

    public ThrowingKnifeEntity(EntityType<? extends ThrowingKnifeEntity> type, Level level) {
        super(type, level);
    }

    public ThrowingKnifeEntity(Level level, Player owner, double x, double y, double z) {
        super(IblisEntities.THROWING_KNIFE.get(), level, owner, x, y, z);
        double baseDamage = owner.getAttributeValue(IblisAttributes.PROJECTILE_DAMAGE.get());
        setBaseDamage(baseDamage * (PlayerSkill.THROWING.getFullValue(owner) * 0.1 + 0.2));
    }

    @Override
    protected boolean shouldRicochet(BlockHitResult result, boolean hard) {
        boolean ricochet = (!hasRicocheted || result.getDirection() != Direction.UP) && hard;
        if (ricochet) {
            hasRicocheted = true;
        }
        return ricochet;
    }

    @Override
    protected ItemStack getPickupItem() {
        return new ItemStack(IblisItems.IRON_THROWING_KNIFE.get());
    }
}
