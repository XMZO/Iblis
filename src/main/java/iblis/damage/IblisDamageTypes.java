package iblis.damage;

import iblis.IblisMod;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class IblisDamageTypes {
    public static final ResourceKey<DamageType> SHOTGUN = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            new ResourceLocation(IblisMod.MOD_ID, "shotgun"));
    public static final ResourceKey<DamageType> SHOTGUN_ENDERMAN = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            new ResourceLocation(IblisMod.MOD_ID, "shotgun_enderman"));
    public static final ResourceKey<DamageType> CROSSBOW = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            new ResourceLocation(IblisMod.MOD_ID, "crossbow"));

    private IblisDamageTypes() {
    }

    public static DamageSource shotgun(ServerLevel level, Player player) {
        return playerDamage(level, SHOTGUN, player);
    }

    public static DamageSource shotgunAgainstEnderman(ServerLevel level, Player player) {
        return playerDamage(level, SHOTGUN_ENDERMAN, player);
    }

    private static DamageSource playerDamage(
            ServerLevel level, ResourceKey<DamageType> key, Player player) {
        Holder<DamageType> type = level.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(key);
        return new DamageSource(type, player, player);
    }

    public static DamageSource crossbow(ServerLevel level, Entity projectile, Entity owner) {
        Holder<DamageType> type = level.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(CROSSBOW);
        return new DamageSource(type, projectile, owner);
    }

    public static boolean isShotgun(DamageSource source) {
        return source.is(SHOTGUN) || source.is(SHOTGUN_ENDERMAN);
    }

    public static boolean isCrossbow(DamageSource source) {
        return source.is(CROSSBOW);
    }
}
