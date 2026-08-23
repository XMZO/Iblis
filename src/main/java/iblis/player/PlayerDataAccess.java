package iblis.player;

import net.minecraft.world.entity.player.Player;

public final class PlayerDataAccess {
    private PlayerDataAccess() {
    }

    public static IblisPlayerData get(Player player) {
        return player.getCapability(IblisPlayerDataProvider.CAPABILITY)
                .orElseThrow(() -> new IllegalStateException(
                        "Iblis player data was not attached to " + player.getScoreboardName()));
    }
}
