package iblis.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;

public interface CustomLeftClickItem {
    void onLeftClick(ServerLevel level, ServerPlayer player, InteractionHand hand);
}
