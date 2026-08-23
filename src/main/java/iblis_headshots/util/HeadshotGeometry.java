package iblis_headshots.util;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Donkey;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.animal.horse.Mule;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class HeadshotGeometry {
    private static final AABB ZERO = new AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
    private static final AABB SLIME_CORE = box(0.4, 0.4, 0.4, 0.6, 0.6, 0.6);
    private static final AABB SHULKER_CORE = box(0.4, 0.1, 0.4, 0.6, 0.4, 0.6);
    private static final AABB HUMANOID_HEAD = box(0.1, 0.8, 0.1, 0.9, 1.0, 0.9);
    private static final AABB HUSK_HEAD = box(0.1, 0.8, 0.1, 0.9, 1.1, 0.9);
    private static final AABB SPIDER_HEAD = box(0.6, 0.5, 0.3, 1.2, 1.2, 0.7);
    private static final AABB CHICKEN_HEAD = box(0.9, 0.8, 0.3, 1.4, 1.4, 0.7);
    private static final AABB COW_HEAD = box(0.9, 0.7, 0.2, 1.4, 1.2, 0.8);
    private static final AABB HORSE_HEAD = box(0.7, 0.7, 0.2, 1.0, 1.0, 0.8);
    private static final AABB GUARDIAN_EYE = box(0.8, 0.4, 0.4, 1.0, 0.6, 0.6);
    private static final AABB GHAST_EYES = box(0.8, 0.6, 0.2, 1.0, 0.7, 0.8);
    private static final AABB BEAR_HEAD = box(1.0, 0.6, 0.2, 1.5, 1.0, 0.8);

    private HeadshotGeometry() {
    }

    public static boolean intersectsHead(LivingEntity entity, Vec3 start, Vec3 end) {
        return getHeadBox(entity).clip(start, end).isPresent();
    }

    public static AABB getHeadBox(LivingEntity entity) {
        AABB collisionBox = entity.getBoundingBox();
        AABB relative;

        if (entity instanceof Slime) {
            relative = SLIME_CORE;
        } else if (entity instanceof Shulker) {
            relative = SHULKER_CORE;
        } else if (entity instanceof Bat || entity instanceof Endermite || entity instanceof Ocelot
                || entity instanceof Cat
                || entity instanceof Parrot || entity instanceof Silverfish || entity instanceof Squid) {
            return ZERO;
        } else if (entity instanceof Spider) {
            relative = rotateAroundY(SPIDER_HEAD, entity.yBodyRot);
        } else if (entity instanceof Chicken || entity instanceof Rabbit) {
            relative = rotateAroundY(CHICKEN_HEAD, entity.yBodyRot);
        } else if (entity instanceof Cow || entity instanceof Pig || entity instanceof Sheep) {
            relative = rotateAroundY(COW_HEAD, entity.yBodyRot);
        } else if (entity instanceof Donkey || entity instanceof Mule || entity instanceof Llama
                || entity instanceof AbstractHorse) {
            relative = rotateAroundY(HORSE_HEAD, entity.yBodyRot);
        } else if (entity instanceof Guardian) {
            relative = rotateAroundY(GUARDIAN_EYE, entity.yBodyRot);
        } else if (entity instanceof Ghast) {
            relative = rotateAroundY(GHAST_EYES, entity.yBodyRot);
        } else if (entity instanceof PolarBear bear) {
            AABB box = shrinkTo(collisionBox, rotateAroundY(BEAR_HEAD, entity.yBodyRot));
            float attackAnimation = bear.getAttackAnim(0.0F);
            return attackAnimation > 0.2F ? box.move(0.0, attackAnimation, 0.0) : box;
        } else if (entity instanceof Wolf) {
            relative = rotateAroundY(BEAR_HEAD, entity.yBodyRot);
        } else if (entity instanceof Husk) {
            relative = HUSK_HEAD;
        } else {
            relative = HUMANOID_HEAD;
        }

        return shrinkTo(collisionBox, relative);
    }

    private static AABB box(double minX, double minY, double minZ,
                            double maxX, double maxY, double maxZ) {
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static AABB rotateAroundY(AABB box, float yaw) {
        float cos = Mth.cos(-yaw * Mth.DEG_TO_RAD);
        float sin = Mth.sin(-yaw * Mth.DEG_TO_RAD);
        float minX = (float) (box.minX - 0.5);
        float maxX = (float) (box.maxX - 0.5);
        float minZ = (float) (box.minZ - 0.5);
        float maxZ = (float) (box.maxZ - 0.5);
        float x00 = 0.5F + sin * minX + cos * minZ;
        float z00 = 0.5F + cos * minX + sin * minZ;
        float x11 = 0.5F + sin * maxX + cos * maxZ;
        float z11 = 0.5F + cos * maxX + sin * maxZ;
        float x10 = 0.5F + sin * maxX + cos * minZ;
        float z10 = 0.5F + cos * maxX + sin * minZ;
        float x01 = 0.5F + sin * minX + cos * maxZ;
        float z01 = 0.5F + cos * minX + sin * maxZ;
        return new AABB(
                min(x00, x10, x01, x11), box.minY, min(z00, z10, z01, z11),
                max(x00, x10, x01, x11), box.maxY, max(z00, z10, z01, z11));
    }

    private static float min(float first, float second, float third, float fourth) {
        return Math.min(Math.min(first, second), Math.min(third, fourth));
    }

    private static float max(float first, float second, float third, float fourth) {
        return Math.max(Math.max(first, second), Math.max(third, fourth));
    }

    private static AABB shrinkTo(AABB original, AABB relative) {
        double sizeX = original.getXsize();
        double sizeY = original.getYsize();
        double sizeZ = original.getZsize();
        return new AABB(
                original.minX + relative.minX * sizeX,
                original.minY + relative.minY * sizeY,
                original.minZ + relative.minZ * sizeZ,
                original.minX + relative.maxX * sizeX,
                original.minY + relative.maxY * sizeY,
                original.minZ + relative.maxZ * sizeZ);
    }
}
