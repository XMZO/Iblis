package iblis.util;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class IblisMath {
    private IblisMath() {
    }

    public static float[] calculateOverlap(AABB box, Vec3 start, Vec3 end) {
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
        return new float[] {Math.max(dx, Math.max(dy, dz)), centerDistance};
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
