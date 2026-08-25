package iblis.client;

import com.mojang.blaze3d.platform.InputConstants;
import iblis.IblisMod;
import iblis.compat.CompatHooks;
import iblis.item.CustomLeftClickItem;
import iblis.item.FirearmItem;
import iblis.item.GuideBookItem;
import iblis.crafting.CraftingQuality;
import iblis.client.gui.ModConfigScreens;
import iblis.client.gui.FirearmCooldownDecorator;
import iblis.client.gui.IblisScreenEvents;
import iblis.client.particle.BoulderShardParticle;
import iblis.client.particle.SliverParticle;
import iblis.client.particle.SparkParticle;
import iblis.item.TooltipComponents;
import iblis.client.renderer.BoulderRenderer;
import iblis.client.renderer.CrossbowBoltRenderer;
import iblis.client.renderer.ThrowingKnifeRenderer;
import iblis.network.IblisNetwork;
import iblis.network.packet.PlayerActionPacket;
import iblis.player.PlayerDataAccess;
import iblis.registry.IblisItems;
import iblis.config.IblisConfig;
import iblis.item.CrossbowReloadingItem;
import iblis.player.PlayerSkill;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.Tag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterItemDecorationsEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.ModList;
import org.lwjgl.glfw.GLFW;

public final class IblisClientEvents {
    private static final KeyMapping RELOAD_KEY = new KeyMapping(
            "key.iblis.reload", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R,
            "key.categories.gameplay");
    private static final KeyMapping OPEN_CHARACTERISTICS_KEY = new KeyMapping(
            "key.iblis.open_characteristics", InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(), "key.categories.gameplay");
    private static final KeyMapping OPEN_SKILLS_KEY = new KeyMapping(
            "key.iblis.open_skills", InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(), "key.categories.gameplay");
    private static boolean sprintKeyWasDown;
    private static boolean sprintToggled;
    private static int sprintCounter;
    private static int sprintButtonCounter;
    private static int lastSentSprintCounter;
    private static int lastSentSprintButtonCounter;
    private static ItemStack throttledFirearm = ItemStack.EMPTY;
    private static long nextFireRequestTick;

    private IblisClientEvents() {
    }

    @Mod.EventBusSubscriber(modid = IblisMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD,
            value = Dist.CLIENT)
    public static final class ModBus {
        private ModBus() {
        }

        @SubscribeEvent
        public static void clientSetup(FMLClientSetupEvent event) {
            ModList.get().getModContainerById(IblisMod.MOD_ID).ifPresent(container ->
                    container.registerExtensionPoint(
                            ConfigScreenHandler.ConfigScreenFactory.class,
                            () -> new ConfigScreenHandler.ConfigScreenFactory(
                                    ModConfigScreens::iblis)));
            event.enqueueWork(() -> {
                ItemProperties.register(IblisItems.MEDKIT.get(), id("animation_frame"),
                        (stack, level, entity, seed) -> entity != null
                                && entity.getUseItem() == stack
                                ? stack.getUseDuration() - entity.getUseItemRemainingTicks()
                                : 0.0F);
                ItemProperties.register(IblisItems.HEAVY_SHIELD.get(), id("blocking"),
                        (stack, level, entity, seed) -> entity != null
                                && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
                ItemProperties.register(IblisItems.SHOTGUN.get(), id("aiming"),
                        (stack, level, entity, seed) -> isUsing(entity, stack) ? 1.0F : 0.0F);
                ItemProperties.register(IblisItems.SHOTGUN_RELOADING.get(), id("ammo"),
                        (stack, level, entity, seed) -> ammunition(stack));
                ItemProperties.register(IblisItems.CROSSBOW.get(), id("state"),
                        (stack, level, entity, seed) -> crossbowState(stack, entity));
                ItemProperties.register(IblisItems.CROSSBOW_RELOADING.get(), id("state"),
                        (stack, level, entity, seed) -> crossbowReloadingState(stack, entity));
            });
        }

        @SubscribeEvent
        public static void registerItemDecorations(RegisterItemDecorationsEvent event) {
            var decorator = FirearmCooldownDecorator.INSTANCE;
            event.register(IblisItems.SHOTGUN.get(), decorator);
            event.register(IblisItems.SHOTGUN_RELOADING.get(), decorator);
            event.register(IblisItems.CROSSBOW.get(), decorator);
            event.register(IblisItems.CROSSBOW_RELOADING.get(), decorator);
        }

        @SubscribeEvent
        public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(RELOAD_KEY);
            event.register(OPEN_CHARACTERISTICS_KEY);
            event.register(OPEN_SKILLS_KEY);
        }

        @SubscribeEvent
        public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
            event.registerSpriteSet(iblis.registry.IblisParticles.SPARK.get(),
                    SparkParticle.Provider::new);
            event.registerSpecial(iblis.registry.IblisParticles.BOULDER_SHARD.get(),
                    new BoulderShardParticle.Provider());
            event.registerSpecial(iblis.registry.IblisParticles.SLIVER.get(),
                    new SliverParticle.Provider());
        }

        @SubscribeEvent
        public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
            for (ResourceLocation model : SliverParticle.MODELS) {
                event.register(model);
            }
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        @SubscribeEvent
        public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(iblis.registry.IblisEntities.BOULDER.get(),
                    BoulderRenderer::new);
            event.registerEntityRenderer(iblis.registry.IblisEntities.THROWING_KNIFE.get(),
                    ThrowingKnifeRenderer::new);
            event.registerEntityRenderer(iblis.registry.IblisEntities.CROSSBOW_BOLT.get(),
                    CrossbowBoltRenderer::new);
            event.registerEntityRenderer(iblis.registry.IblisEntities.PLAYER_ZOMBIE.get(),
                    context -> (net.minecraft.client.renderer.entity.EntityRenderer) new ZombieRenderer(context));
        }

        private static ResourceLocation id(String path) {
            return new ResourceLocation(IblisMod.MOD_ID, path);
        }

        private static boolean isUsing(net.minecraft.world.entity.LivingEntity entity,
                                       ItemStack stack) {
            return entity != null && entity.isUsingItem() && entity.getUseItem() == stack;
        }

        private static int ammunition(ItemStack stack) {
            return stack.hasTag()
                    ? stack.getTag().getList(iblis.item.FirearmItem.AMMO, Tag.TAG_COMPOUND).size()
                    : 0;
        }

        private static float crossbowState(ItemStack stack,
                                           net.minecraft.world.entity.LivingEntity entity) {
            int ammo = ammunition(stack);
            int cocked = stack.hasTag()
                    ? stack.getTag().getInt(iblis.item.FirearmItem.COCKED_STATE) : 0;
            int state;
            if (ammo == 0) {
                state = cocked == 1 ? 1 : 0;
            } else if (ammo == 1) {
                state = cocked == 2 ? 3 : 2;
            } else {
                state = 4;
            }
            return isUsing(entity, stack) ? state + 5 : state;
        }

        private static float crossbowReloadingState(ItemStack stack,
                                                    net.minecraft.world.entity.LivingEntity entity) {
            int ammo = ammunition(stack);
            int cocked = stack.hasTag()
                    ? stack.getTag().getInt(iblis.item.FirearmItem.COCKED_STATE) : 0;
            if (!isUsing(entity, stack)) {
                if (ammo == 0) {
                    return cocked == 1 ? 5 : 0;
                }
                if (ammo == 1) {
                    return cocked == 2 ? 12 : 7;
                }
                return 14;
            }

            int remaining = entity.getUseItemRemainingTicks();
            if (ammo == 0 && cocked == 0) {
                return remaining <= 2 ? 5 : remaining <= 3 ? 4 : remaining <= 4 ? 3
                        : remaining <= 5 ? 2 : remaining <= 6 ? 1 : 0;
            }
            if (ammo == 0 && cocked == 1) {
                return remaining <= 2 ? 7 : remaining <= 4 ? 6 : 5;
            }
            if (ammo == 1 && cocked == 1) {
                return remaining <= 2 ? 12 : remaining <= 3 ? 11 : remaining <= 4 ? 10
                        : remaining <= 5 ? 9 : remaining <= 6 ? 8 : 7;
            }
            if (ammo == 1 && cocked == 2) {
                return remaining <= 2 ? 14 : remaining <= 4 ? 13 : 12;
            }
            return 14;
        }
    }

    @Mod.EventBusSubscriber(modid = IblisMod.MOD_ID, value = Dist.CLIENT)
    public static final class ForgeBus {
        private ForgeBus() {
        }

        @SubscribeEvent
        public static void itemTooltip(ItemTooltipEvent event) {
            ItemStack stack = event.getItemStack();
            Player player = event.getEntity();
            if (!stack.hasTag()) {
                return;
            }
            if (stack.getTag().contains(CraftingQuality.QUALITY)) {
                TooltipComponents.addQuality(event.getToolTip(),
                        stack.getTag().getInt(CraftingQuality.QUALITY));
            }
            if (stack.getItem() instanceof GuideBookItem && player != null) {
                int id = stack.getTag().getInt(GuideBookItem.BOOK_ID);
                if (GuideBookItem.findBook(PlayerDataAccess.get(player).exploredBooks(), id) != null) {
                    event.getToolTip().add(net.minecraft.network.chat.Component.translatable(
                            "iblis.youAlreadyReadThatBook"));
                }
            }
        }

        @SubscribeEvent
        public static void clientTick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.START) {
                handleAimedLeftClick();
                handleHeldFire();
                return;
            }
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null) {
                resetSprintState();
                return;
            }
            updateSprinting(minecraft);
            if (minecraft.screen != null) {
                return;
            }
            boolean openCharacteristics = OPEN_CHARACTERISTICS_KEY.consumeClick();
            boolean openSkills = OPEN_SKILLS_KEY.consumeClick();
            if (openCharacteristics
                    && IblisScreenEvents.openCharacteristicsScreen(null)) {
                return;
            }
            if (openSkills && IblisScreenEvents.openSkillsScreen(null)) {
                return;
            }
            while (RELOAD_KEY.consumeClick()) {
                IblisNetwork.sendPlayerAction(PlayerActionPacket.Action.RELOAD,
                        InteractionHand.MAIN_HAND);
            }
        }

        private static void handleAimedLeftClick() {
            Minecraft minecraft = Minecraft.getInstance();
            Player player = minecraft.player;
            if (minecraft.screen != null || player == null || !player.isUsingItem()
                    || player.getUsedItemHand() != InteractionHand.MAIN_HAND
                    || !(player.getUseItem().getItem() instanceof CustomLeftClickItem)) {
                return;
            }
            // Vanilla consumes attack clicks without firing the Forge interaction event
            // while an item is being used, so forward those clicks before it discards them.
            while (minecraft.options.keyAttack.consumeClick()) {
                requestLeftClick(player, InteractionHand.MAIN_HAND);
            }
        }

        private static void handleHeldFire() {
            Minecraft minecraft = Minecraft.getInstance();
            Player player = minecraft.player;
            if (minecraft.screen != null || player == null || minecraft.level == null
                    || !minecraft.options.keyAttack.isDown()
                    || !(player.getMainHandItem().getItem() instanceof FirearmItem)) {
                throttledFirearm = ItemStack.EMPTY;
                nextFireRequestTick = 0L;
                return;
            }
            requestLeftClick(player, InteractionHand.MAIN_HAND);
        }

        private static boolean requestLeftClick(Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof FirearmItem firearm) {
                long now = player.level().getGameTime();
                if (stack != throttledFirearm) {
                    throttledFirearm = stack;
                    nextFireRequestTick = now;
                }
                if (now < nextFireRequestTick
                        || FirearmItem.fireCooldownPercent(stack, player.level(), 0.0F) > 0.0F) {
                    return false;
                }
                nextFireRequestTick = now + firearm.fireAttemptIntervalTicks(stack);
            }
            IblisNetwork.sendPlayerAction(PlayerActionPacket.Action.LEFT_CLICK, hand);
            return true;
        }

        @SubscribeEvent
        public static void clientLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
            IblisConfig.clearServerSnapshot();
            throttledFirearm = ItemStack.EMPTY;
            nextFireRequestTick = 0L;
        }

        @SubscribeEvent
        public static void interactionInput(InputEvent.InteractionKeyMappingTriggered event) {
            if (!event.isAttack()) {
                return;
            }
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen != null || minecraft.player == null) {
                return;
            }
            Player player = minecraft.player;
            ItemStack held = player.getMainHandItem();
            if (held.getItem() instanceof CustomLeftClickItem) {
                requestLeftClick(player, InteractionHand.MAIN_HAND);
                event.setSwingHand(false);
                event.setCanceled(true);
                return;
            }
            if (player.onGround() && player.isBlocking()) {
                IblisNetwork.sendPlayerAction(PlayerActionPacket.Action.SHIELD_PUNCH,
                        player.getUsedItemHand());
                float yaw = player.getYRot() * net.minecraft.util.Mth.DEG_TO_RAD;
                float strength = player.getAttackStrengthScale(0.0F) * 0.2F;
                player.setDeltaMovement(player.getDeltaMovement().add(
                        -net.minecraft.util.Mth.sin(yaw) * strength, 0.0,
                        net.minecraft.util.Mth.cos(yaw) * strength));
                player.swing(player.getUsedItemHand());
                event.setSwingHand(false);
                event.setCanceled(true);
                return;
            }
            if (player.onGround() && !player.isUsingItem() && minecraft.options.keyUse.isDown()) {
                IblisNetwork.sendPlayerAction(PlayerActionPacket.Action.KICK,
                        InteractionHand.MAIN_HAND);
                event.setSwingHand(false);
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void movementInput(MovementInputUpdateEvent event) {
            Player player = event.getEntity();
            if (player.isPassenger() || !player.isUsingItem()) {
                return;
            }
            ItemStack using = player.getUseItem();
            if (using.getItem() instanceof CrossbowReloadingItem) {
                event.getInput().leftImpulse = 0.0F;
                event.getInput().forwardImpulse = 0.0F;
                return;
            }
            if (using.getItem() instanceof net.minecraft.world.item.BowItem) {
                compensateMovement(event, PlayerSkill.ARCHERY.getFullValue(player));
            } else {
                CompatHooks.UseItemProfile profile = CompatHooks.useItemProfile(using);
                if (profile != null) {
                    compensateMovement(event, profile.skill().getFullValue(player));
                } else if (player.isBlocking()) {
                    compensateMovement(event, PlayerSkill.PARRY.getFullValue(player));
                }
            }
        }

        private static void compensateMovement(MovementInputUpdateEvent event, double skill) {
            if (skill < 5.1) {
                return;
            }
            float multiplier = 5.0F - 20.0F / (float) skill;
            event.getInput().leftImpulse *= multiplier;
            event.getInput().forwardImpulse *= multiplier;
        }

        private static void updateSprinting(Minecraft minecraft) {
            Player player = minecraft.player;
            boolean keyDown = minecraft.options.keySprint.isDown();
            if (IblisConfig.toggleSprint && keyDown && !sprintKeyWasDown
                    && minecraft.screen == null) {
                sprintToggled = !sprintToggled;
            }
            sprintKeyWasDown = keyDown;
            if (!IblisConfig.toggleSprint) {
                sprintToggled = false;
            } else if (minecraft.screen == null) {
                player.setSprinting(sprintToggled);
            }

            boolean pressed = IblisConfig.toggleSprint ? sprintToggled : keyDown;
            if (!player.isSprinting()) {
                sprintCounter = 0;
            } else if (sprintCounter == 0) {
                sprintCounter = 1;
            } else if (pressed && sprintCounter < 32) {
                sprintCounter++;
            }
            if ((sprintCounter >>> 2) != (lastSentSprintCounter >>> 2)) {
                IblisNetwork.sendSprintState(sprintCounter);
                lastSentSprintCounter = sprintCounter;
            }
            if (pressed && player.getFoodData().getFoodLevel() > 6) {
                sprintButtonCounter = Math.min(sprintButtonCounter + 1, 32);
            } else {
                sprintButtonCounter = 0;
            }
            if ((sprintButtonCounter >>> 2) != (lastSentSprintButtonCounter >>> 2)) {
                IblisNetwork.sendSprintButtonState(sprintButtonCounter);
                lastSentSprintButtonCounter = sprintButtonCounter;
            }
        }

        private static void resetSprintState() {
            sprintKeyWasDown = false;
            sprintToggled = false;
            sprintCounter = 0;
            sprintButtonCounter = 0;
            lastSentSprintCounter = 0;
            lastSentSprintButtonCounter = 0;
        }
    }

    public static int sprintCounter() {
        return sprintCounter;
    }

    public static int sprintButtonCounter() {
        return sprintButtonCounter;
    }
}
