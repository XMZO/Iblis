package iblis.player;

import iblis.IblisMod;
import iblis.config.IblisConfig;
import iblis.network.IblisNetwork;
import iblis.registry.IblisAttributes;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = IblisMod.MOD_ID)
public final class PlayerDataEvents {
    private static final ResourceLocation PLAYER_DATA_ID =
            new ResourceLocation(IblisMod.MOD_ID, "player_data");

    private PlayerDataEvents() {
    }

    @SubscribeEvent
    public static void attachPlayerData(AttachCapabilitiesEvent<Entity> event) {
        if (!(event.getObject() instanceof Player)) {
            return;
        }

        IblisPlayerDataProvider provider = new IblisPlayerDataProvider();
        event.addCapability(PLAYER_DATA_ID, provider);
        event.addListener(provider::invalidate);
    }

    @SubscribeEvent
    public static void clonePlayer(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        Player clone = event.getEntity();
        boolean preserve = !event.isWasDeath() || IblisConfig.noDeathPenalty;
        if (!preserve) {
            return;
        }

        original.reviveCaps();
        try {
            PlayerDataAccess.get(clone).copyPersistentFrom(PlayerDataAccess.get(original));
            copyAttributes(original, clone);
        } finally {
            original.invalidateCaps();
        }
        PlayerAttributeEffects.refreshMeleeDamageBonus(clone);
    }

    @SubscribeEvent
    public static void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        sync(event.getEntity());
    }

    @SubscribeEvent
    public static void playerRespawned(PlayerEvent.PlayerRespawnEvent event) {
        sync(event.getEntity());
    }

    @SubscribeEvent
    public static void playerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        sync(event.getEntity());
    }

    private static void sync(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            IblisNetwork.sendGameplayConfig(serverPlayer);
            IblisNetwork.sendPlayerData(serverPlayer);
            PlayerAttributeEffects.refreshMeleeDamageBonus(serverPlayer);
            PlayerAttributeEffects.refreshWeaponSkill(serverPlayer);
        }
    }

    private static void copyAttributes(Player source, Player target) {
        Set<Attribute> attributes = new LinkedHashSet<>();
        attributes.add(Attributes.MAX_HEALTH);
        attributes.add(Attributes.ATTACK_SPEED);
        attributes.add(Attributes.LUCK);
        attributes.addAll(IblisAttributes.playerAttributes());

        for (Attribute attribute : attributes) {
            AttributeInstance sourceInstance = source.getAttribute(attribute);
            AttributeInstance targetInstance = target.getAttribute(attribute);
            if (sourceInstance != null && targetInstance != null) {
                targetInstance.setBaseValue(sourceInstance.getBaseValue());
            }
        }
    }
}
