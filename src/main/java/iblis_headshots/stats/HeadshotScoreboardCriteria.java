package iblis_headshots.stats;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

/** Scoreboard criteria updated after a server-confirmed headshot. */
public final class HeadshotScoreboardCriteria {
    public static final ObjectiveCriteria RECEIVED =
            new HeadshotCriterion("iblis.headshots_received");
    public static final ObjectiveCriteria LIVING =
            new HeadshotCriterion("iblis.headshots_living");
    public static final ObjectiveCriteria PLAYERS =
            new HeadshotCriterion("iblis.headshots_players");

    private HeadshotScoreboardCriteria() {
    }

    /** Forces criterion registration before worlds and commands are loaded. */
    public static void bootstrap() {
    }

    public static void record(LivingEntity victim, Entity attacker) {
        if (victim instanceof ServerPlayer player) {
            increment(player, RECEIVED);
        }
        if (attacker instanceof ServerPlayer player) {
            increment(player, LIVING);
            if (victim instanceof Player) {
                increment(player, PLAYERS);
            }
        }
    }

    private static void increment(ServerPlayer player, ObjectiveCriteria criterion) {
        player.getScoreboard().forAllObjectives(
                criterion, player.getScoreboardName(), Score::increment);
    }

    private static final class HeadshotCriterion extends ObjectiveCriteria {
        private HeadshotCriterion(String name) {
            super(name);
        }
    }
}
