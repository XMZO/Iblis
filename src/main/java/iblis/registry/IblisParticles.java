package iblis.registry;

import iblis.IblisMod;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class IblisParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, IblisMod.MOD_ID);

    public static final RegistryObject<SimpleParticleType> SPARK =
            PARTICLES.register("spark", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> BOULDER_SHARD =
            PARTICLES.register("boulder_shard", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> SLIVER =
            PARTICLES.register("sliver", () -> new SimpleParticleType(false));

    private IblisParticles() {
    }

    public static void register(IEventBus modBus) {
        PARTICLES.register(modBus);
    }
}
