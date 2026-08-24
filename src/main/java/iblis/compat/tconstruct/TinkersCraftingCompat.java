package iblis.compat.tconstruct;

import iblis.compat.CompatHooks;
import iblis.crafting.CraftingQuality;
import iblis.player.PlayerSkill;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.TierSortingRegistry;
import net.minecraftforge.event.entity.player.PlayerEvent;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.tools.nbt.StatsNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

/** Applies Iblis skill requirements and quality to newly assembled Tinkers' tools. */
public final class TinkersCraftingCompat {
    private static final List<CraftingRule> RULES = List.of(
            new CraftingRule(stack -> stack.is(TinkerTags.Items.ARMOR)
                    || stack.is(TinkerTags.Items.SHIELDS), PlayerSkill.ARMORSMITH,
                    TinkersCraftingCompat::armorRequirement),
            new CraftingRule(stack -> stack.is(TinkerTags.Items.LAUNCHERS),
                    PlayerSkill.MECHANICS, TinkersCraftingCompat::launcherRequirement),
            new CraftingRule(stack -> stack.is(TinkerTags.Items.MODIFIABLE),
                    PlayerSkill.WEAPONSMITH, TinkersCraftingCompat::toolRequirement)
    );

    private TinkersCraftingCompat() {
    }

    static void register() {
        CompatHooks.registerCrafting("tconstruct:craftsmanship",
                stack -> stack.is(TinkerTags.Items.MODIFIABLE),
                TinkersCraftingCompat::onCrafted);
    }

    private static void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack output = event.getCrafting();
        ToolStack.ensureInitialized(output);
        if (!ToolStack.isInitialized(output)) {
            return;
        }

        ToolStack tool = ToolStack.from(output);
        boolean hasQuality = tool.getPersistentData()
                .contains(TinkersCraftsmanshipModifier.QUALITY);
        boolean hasModifier = tool.getModifierLevel(
                TinkersCraftsmanshipModifier.CRAFTSMANSHIP.getId()) > 0;
        if (hasQuality || hasModifier) {
            int quality = tool.getPersistentData()
                    .getInt(TinkersCraftsmanshipModifier.QUALITY);
            tool.getPersistentData().putInt(TinkersCraftsmanshipModifier.QUALITY, quality);
            output.getOrCreateTag().putInt(CraftingQuality.QUALITY, quality);
            if (!hasModifier && TinkersCraftsmanshipModifier.CRAFTSMANSHIP.isBound()) {
                tool.addModifier(TinkersCraftsmanshipModifier.CRAFTSMANSHIP.getId(), 1);
            }
            return;
        }

        CraftingRule rule = RULES.stream()
                .filter(candidate -> candidate.matches().test(output))
                .findFirst()
                .orElse(null);
        if (rule == null) {
            return;
        }

        double required = finiteRequirement(rule.requirement().applyAsDouble(tool));
        if (rule.skill().enabled
                && TinkersCraftsmanshipModifier.CRAFTSMANSHIP.isBound()) {
            int quality = (int) (rule.skill().getRawFullValue(player) - required);
            tool.getPersistentData().putInt(TinkersCraftsmanshipModifier.QUALITY, quality);
            output.getOrCreateTag().putInt(CraftingQuality.QUALITY, quality);
            tool.addModifier(TinkersCraftsmanshipModifier.CRAFTSMANSHIP.getId(), 1);
        }
        rule.skill().raise(player, Math.max(required, 0.0) + 1.0);
    }

    private static double armorRequirement(ToolStack tool) {
        StatsNBT stats = tool.getStats();
        double required = durabilityRequirement(tool, stats);
        if (tool.hasTag(TinkerTags.Items.ARMOR)) {
            required += stats.get(ToolStats.ARMOR);
            required += stats.get(ToolStats.ARMOR_TOUGHNESS);
        }
        if (tool.hasTag(TinkerTags.Items.SHIELDS)) {
            required += stats.get(ToolStats.BLOCK_AMOUNT);
        }
        return required;
    }

    private static double launcherRequirement(ToolStack tool) {
        StatsNBT stats = tool.getStats();
        return durabilityRequirement(tool, stats)
                + stats.get(ToolStats.DRAW_SPEED)
                + stats.get(ToolStats.VELOCITY)
                + stats.get(ToolStats.PROJECTILE_DAMAGE);
    }

    private static double toolRequirement(ToolStack tool) {
        StatsNBT stats = tool.getStats();
        double required = durabilityRequirement(tool, stats);
        if (tool.hasTag(TinkerTags.Items.MELEE)) {
            required += stats.get(ToolStats.ATTACK_DAMAGE);
        }
        if (tool.hasTag(TinkerTags.Items.MELEE_WEAPON)) {
            required += stats.get(ToolStats.ATTACK_SPEED);
        }
        if (tool.hasTag(TinkerTags.Items.HARVEST)) {
            int tierLevel = Math.max(0, TierSortingRegistry.getSortedTiers()
                    .indexOf(stats.get(ToolStats.HARVEST_TIER)));
            required += stats.get(ToolStats.MINING_SPEED) + tierLevel;
        }
        if (tool.hasTag(TinkerTags.Items.RANGED)) {
            required += stats.get(ToolStats.DRAW_SPEED) + stats.get(ToolStats.VELOCITY);
        }
        return required;
    }

    private static double durabilityRequirement(ToolStack tool, StatsNBT stats) {
        return tool.hasTag(TinkerTags.Items.DURABILITY)
                ? stats.get(ToolStats.DURABILITY) / 500.0 - 5.0 : 0.0;
    }

    private static double finiteRequirement(double value) {
        return Double.isFinite(value) ? value : 0.0;
    }

    private record CraftingRule(Predicate<ItemStack> matches, PlayerSkill skill,
                                ToDoubleFunction<ToolStack> requirement) {
    }
}
