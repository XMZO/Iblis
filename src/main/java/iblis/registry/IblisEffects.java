package iblis.registry;

import iblis.IblisMod;
import iblis.effect.IblisMobEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class IblisEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, IblisMod.MOD_ID);

    public static final RegistryObject<MobEffect> AWARENESS = EFFECTS.register("awareness", () ->
            new IblisMobEffect(MobEffectCategory.BENEFICIAL, 0)
                    .addAttributeModifier(Attributes.FOLLOW_RANGE,
                            "0a111a5f-fef5-f1e7-0f01-0000001770b5", 1.0,
                            AttributeModifier.Operation.ADDITION));

    public static final RegistryObject<MobEffect> OVERHEATING = EFFECTS.register("overheating", () ->
            new IblisMobEffect(MobEffectCategory.HARMFUL, 0)
                    .addAttributeModifier(IblisAttributes.FIRE_DAMAGE_REDUCTION.get(),
                            "0a111a5f-fef5-f1e7-0f01-0000001770b5", -0.1,
                            AttributeModifier.Operation.ADDITION));

    private IblisEffects() {
    }

    public static void register(IEventBus modBus) {
        EFFECTS.register(modBus);
    }
}
