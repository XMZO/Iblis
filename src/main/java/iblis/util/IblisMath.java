package iblis.util;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class IblisMath {
    private IblisMath() {
    }

    public static float calculateOverlapMultiplier(
            AABB box, Vec3 start, Vec3 end, float splashCone) {
        float startX = (float) start.x;
        float startY = (float) start.y;
        float startZ = (float) start.z;
        float minX = (float) box.minX - startX;
        float maxX = (float) box.maxX - startX;
        float minY = (float) box.minY - startY;
        float maxY = (float) box.maxY - startY;
        float minZ = (float) box.minZ - startZ;
        float maxZ = (float) box.maxZ - startZ;
        float centerX = (minX + maxX) * 0.5F;
        float centerY = (minY + maxY) * 0.5F;
        float centerZ = (minZ + maxZ) * 0.5F;
        float rayX = (float) end.x - startX;
        float rayY = (float) end.y - startY;
        float rayZ = (float) end.z - startZ;
        float centerDistance = (float) Math.sqrt(
                centerX * centerX + centerY * centerY + centerZ * centerZ);
        float rayDistance = (float) Math.sqrt(rayX * rayX + rayY * rayY + rayZ * rayZ);
        if (centerDistance <= 1.0E-6F) {
            return box.contains(start) ? 1.0F : 0.0F;
        }
        if (rayDistance <= 1.0E-6F || splashCone <= 0.0F) {
            return 0.0F;
        }
        minX /= centerDistance;
        minY /= centerDistance;
        minZ /= centerDistance;
        maxX /= centerDistance;
        maxY /= centerDistance;
        maxZ /= centerDistance;
        rayX /= rayDistance;
        rayY /= rayDistance;
        rayZ /= rayDistance;
        float dx = distanceToInterval(rayX, minX, maxX);
        float dy = distanceToInterval(rayY, minY, maxY);
        float dz = distanceToInterval(rayZ, minZ, maxZ);
        float angularDistance = Math.max(dx, Math.max(dy, dz));
        float threshold = centerDistance * splashCone;
        float overlap = 1.0F - (angularDistance * 2.0F + 1.0F) + threshold;
        if (overlap <= 0.0F || threshold <= 0.0F) {
            return 0.0F;
        }
        return Math.min(Math.min(overlap, 1.0F) / threshold, 1.0F);
    }

    private static float distanceToInterval(float value, float min, float max) {
        if (value >= max) {
            return value - max;
        }
        if (value <= min) {
            return min - value;
        }
        return -Math.min(value - min, max - value);
    }
}
