package iblis.registry;

import iblis.IblisMod;
import iblis.entity.BoulderEntity;
import iblis.entity.CrossbowBoltEntity;
import iblis.entity.PlayerZombieEntity;
import iblis.entity.ThrowingKnifeEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class IblisEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, IblisMod.MOD_ID);

    public static final RegistryObject<EntityType<PlayerZombieEntity>> PLAYER_ZOMBIE = ENTITIES.register(
            "player_zombie", () -> EntityType.Builder.of(PlayerZombieEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).clientTrackingRange(80).updateInterval(3)
                    .build(IblisMod.MOD_ID + ":player_zombie"));
    public static final RegistryObject<EntityType<BoulderEntity>> BOULDER = ENTITIES.register(
            "boulder", () -> EntityType.Builder.<BoulderEntity>of(BoulderEntity::new, MobCategory.MISC)
                    .sized(0.4F, 0.4F).clientTrackingRange(64).updateInterval(1)
                    .build(IblisMod.MOD_ID + ":boulder"));
    public static final RegistryObject<EntityType<ThrowingKnifeEntity>> THROWING_KNIFE = ENTITIES.register(
            "throwing_knife", () -> EntityType.Builder.<ThrowingKnifeEntity>of(
                            ThrowingKnifeEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(64).updateInterval(1)
                    .build(IblisMod.MOD_ID + ":throwing_knife"));
    public static final RegistryObject<EntityType<CrossbowBoltEntity>> CROSSBOW_BOLT = ENTITIES.register(
            "crossbow_bolt", () -> EntityType.Builder.<CrossbowBoltEntity>of(
                            CrossbowBoltEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(64).updateInterval(1)
                    .build(IblisMod.MOD_ID + ":crossbow_bolt"));

    private IblisEntities() {
    }

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
    }

    public static void createAttributes(EntityAttributeCreationEvent event) {
        event.put(PLAYER_ZOMBIE.get(), Zombie.createAttributes().build());
    }
}
