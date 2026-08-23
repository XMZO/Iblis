package iblis.player;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;
import net.minecraft.world.phys.Vec3;

@AutoRegisterCapability
public final class IblisPlayerData {
    private static final String EXPLORED_BOOKS = "exploredBooks";

    private ListTag exploredBooks = new ListTag();
    private int knockState;
    private int sprintCounter;
    private int sprintButtonCounter;
    private int reloadTick;
    private float lastPitch;
    private float lastYaw;
    private boolean hasAimSample;
    private int awarenessTicks;
    private Vec3 sprintStart;

    public ListTag exploredBooks() {
        return exploredBooks;
    }

    public void setExploredBooks(ListTag books) {
        exploredBooks = books.copy();
    }

    public int knockState() {
        return knockState;
    }

    public void setKnockState(int value) {
        knockState = value;
    }

    public int sprintCounter() {
        return sprintCounter;
    }

    public void setSprintCounter(int value) {
        sprintCounter = value;
    }

    public int sprintButtonCounter() {
        return sprintButtonCounter;
    }

    public void setSprintButtonCounter(int value) {
        sprintButtonCounter = value;
    }

    public int reloadTick() {
        return reloadTick;
    }

    public void setReloadTick(int value) {
        reloadTick = value;
    }

    public int awarenessTicks() {
        return awarenessTicks;
    }

    public void setAwarenessTicks(int value) {
        awarenessTicks = value;
    }

    public Vec3 sprintStart() {
        return sprintStart;
    }

    public void setSprintStart(Vec3 value) {
        sprintStart = value;
    }

    public boolean updateAimAndMovedFar(float pitch, float yaw) {
        if (!hasAimSample) {
            lastPitch = pitch;
            lastYaw = yaw;
            hasAimSample = true;
            return false;
        }

        float pitchDelta = lastPitch - pitch;
        float yawDelta = lastYaw - yaw;
        lastPitch = pitch;
        lastYaw = yaw;
        return pitchDelta * pitchDelta + yawDelta * yawDelta > 144.0F;
    }

    public void copyPersistentFrom(IblisPlayerData source) {
        exploredBooks = source.exploredBooks.copy();
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.put(EXPLORED_BOOKS, exploredBooks.copy());
        return tag;
    }

    public void load(CompoundTag tag) {
        exploredBooks = tag.contains(EXPLORED_BOOKS, Tag.TAG_LIST)
                ? tag.getList(EXPLORED_BOOKS, Tag.TAG_COMPOUND).copy()
                : new ListTag();
    }
}
