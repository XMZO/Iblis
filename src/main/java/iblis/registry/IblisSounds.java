package iblis.registry;

import iblis.IblisMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class IblisSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, IblisMod.MOD_ID);

    public static final RegistryObject<SoundEvent> BOOK_READING = register("book_reading");
    public static final RegistryObject<SoundEvent> BOOK_CLOSING = register("book_closing");
    public static final RegistryObject<SoundEvent> SHOTGUN_AMMO_LOADING = register("shotgun_ammo_loading");
    public static final RegistryObject<SoundEvent> SHOOT = register("shoot");
    public static final RegistryObject<SoundEvent> SHOTGUN_HAMMER_CLICK = register("shotgun_hammer_click");
    public static final RegistryObject<SoundEvent> SHOTGUN_HAMMER_COCK = register("shotgun_hammer_cock");
    public static final RegistryObject<SoundEvent> SHOTGUN_CHARGING = register("shotgun_charging");
    public static final RegistryObject<SoundEvent> OPENING_MEDKIT = register("opening_medkit");
    public static final RegistryObject<SoundEvent> CLOSING_MEDKIT = register("closing_medkit");
    public static final RegistryObject<SoundEvent> FULL_BOTTLE_SHAKING = register("full_bottle_shaking");
    public static final RegistryObject<SoundEvent> SCISSORS_CLICKING = register("scissors_clicking");
    public static final RegistryObject<SoundEvent> TEARING_BANDAGE = register("tearing_bandage");
    public static final RegistryObject<SoundEvent> BOULDER_IMPACT = register("boulder_impact");
    public static final RegistryObject<SoundEvent> KNIFE_IMPACT = register("knife_impact");
    public static final RegistryObject<SoundEvent> KNIFE_IMPACT_STONE = register("knife_impact_stone");
    public static final RegistryObject<SoundEvent> KNIFE_FALL = register("knife_fall");
    public static final RegistryObject<SoundEvent> CROSSBOW_COCK = register("crossbow_cock");
    public static final RegistryObject<SoundEvent> CROSSBOW_PUTTING_BOLT = register("crossbow_putting_bolt");
    public static final RegistryObject<SoundEvent> CROSSBOW_SHOT = register("crossbow_shot");

    private IblisSounds() {
    }

    private static RegistryObject<SoundEvent> register(String name) {
        ResourceLocation id = new ResourceLocation(IblisMod.MOD_ID, name);
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }

    public static void register(IEventBus modBus) {
        SOUNDS.register(modBus);
    }
}
