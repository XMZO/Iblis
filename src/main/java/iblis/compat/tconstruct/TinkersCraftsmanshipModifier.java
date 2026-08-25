package iblis.compat.tconstruct;

import iblis.IblisMod;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.ToolStatsModifierHook;
import slimeknights.tconstruct.library.modifiers.util.ModifierDeferredRegister;
import slimeknights.tconstruct.library.modifiers.util.StaticModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.stat.FloatToolStat;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

/** Reapplies Iblis crafting quality whenever Tinkers' Construct rebuilds tool stats. */
public final class TinkersCraftsmanshipModifier extends Modifier
        implements ToolStatsModifierHook {
    static final ResourceLocation QUALITY = new ResourceLocation(
            IblisMod.MOD_ID, "tconstruct_crafting_quality");
    private static final ModifierDeferredRegister MODIFIERS =
            ModifierDeferredRegister.create(IblisMod.MOD_ID);
    static final StaticModifier<TinkersCraftsmanshipModifier> CRAFTSMANSHIP =
            MODIFIERS.register("craftsmanship", TinkersCraftsmanshipModifier::new);

    private static final List<StatRule> STAT_RULES = List.of(
            rule(context -> context.hasTag(TinkerTags.Items.DURABILITY), ToolStats.DURABILITY),
            rule(context -> context.hasTag(TinkerTags.Items.MELEE), ToolStats.ATTACK_DAMAGE),
            rule(context -> context.hasTag(TinkerTags.Items.MELEE_WEAPON), ToolStats.ATTACK_SPEED),
            rule(context -> context.hasTag(TinkerTags.Items.HARVEST), ToolStats.MINING_SPEED),
            rule(context -> context.hasTag(TinkerTags.Items.ARMOR), ToolStats.ARMOR),
            rule(context -> context.hasTag(TinkerTags.Items.ARMOR), ToolStats.ARMOR_TOUGHNESS),
            rule(context -> context.hasTag(TinkerTags.Items.SHIELDS), ToolStats.BLOCK_AMOUNT),
            rule(context -> context.hasTag(TinkerTags.Items.RANGED), ToolStats.DRAW_SPEED),
            rule(context -> context.hasTag(TinkerTags.Items.RANGED), ToolStats.VELOCITY),
            rule(context -> context.hasTag(TinkerTags.Items.LAUNCHERS),
                    ToolStats.PROJECTILE_DAMAGE)
    );

    static void register(IEventBus modBus) {
        MODIFIERS.register(modBus);
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.TOOL_STATS);
    }

    @Override
    public void addToolStats(IToolContext context, ModifierEntry modifier,
                             ModifierStatsBuilder builder) {
        double factor = qualityFactor(context.getPersistentData().getInt(QUALITY));
        if (factor == 1.0) {
            return;
        }
        STAT_RULES.stream()
                .filter(rule -> rule.supports().test(context))
                .forEach(rule -> rule.stat().multiply(builder, factor));
    }

    private static double qualityFactor(int quality) {
        if (quality < 0) {
            return 1.0 / (1.0 - quality);
        }
        return 1.0 + quality * 0.1;
    }

    private static StatRule rule(Predicate<IToolContext> supports, FloatToolStat stat) {
        return new StatRule(supports, stat);
    }

    private record StatRule(Predicate<IToolContext> supports, FloatToolStat stat) {
    }
}
