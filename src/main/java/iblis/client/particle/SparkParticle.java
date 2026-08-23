package iblis.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import iblis.IblisMod;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;

public final class SparkParticle extends SimpleAnimatedParticle {
    private static final AtomicBoolean FAILURE_REPORTED = new AtomicBoolean();

    private SparkParticle(ClientLevel level, double x, double y, double z,
                          double xSpeed, double ySpeed, double zSpeed,
                          SpriteSet sprites) {
        super(level, x, y, z, sprites, 0.01F);
        setAlpha(0.9F);
        setFadeColor(0xAC0C00);
        setColor(0xFFF2B8);
        scale(0.4F);
        setLifetime(Math.max(1, getLifetime() / 2));
        // SimpleAnimatedParticle only selects a sprite on its first tick, but a
        // newly added particle may be rendered before that tick happens.
        setSpriteFromAge(sprites);
        xd = xSpeed;
        yd = ySpeed;
        zd = zSpeed;
    }

    @Override
    public void tick() {
        try {
            super.tick();
        } catch (RuntimeException exception) {
            remove();
            reportFailure(exception);
        }
    }

    @Override
    public void render(VertexConsumer consumer, Camera camera, float partialTick) {
        if (sprite == null) {
            remove();
            reportFailure(null);
            return;
        }
        super.render(consumer, camera, partialTick);
    }

    private boolean hasSprite() {
        return sprite != null;
    }

    private static void reportFailure(@Nullable RuntimeException exception) {
        if (!FAILURE_REPORTED.compareAndSet(false, true)) {
            return;
        }
        if (exception == null) {
            IblisMod.LOGGER.warn("Skipped an Iblis spark particle because its sprite was unavailable");
        } else {
            IblisMod.LOGGER.warn("Skipped an Iblis spark particle after a client particle failure", exception);
        }
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        @Nullable
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            if (sprites == null || level == null) {
                reportFailure(null);
                return null;
            }
            try {
                SparkParticle particle = new SparkParticle(
                        level, x, y, z, xSpeed, ySpeed, zSpeed, sprites);
                if (!particle.hasSprite()) {
                    particle.remove();
                    reportFailure(null);
                    return null;
                }
                return particle;
            } catch (RuntimeException exception) {
                reportFailure(exception);
                return null;
            }
        }
    }
}
