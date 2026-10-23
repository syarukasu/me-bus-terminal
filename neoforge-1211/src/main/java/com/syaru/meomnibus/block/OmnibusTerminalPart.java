package com.syaru.meomnibus.block;

import appeng.api.parts.IPartItem;
import com.syaru.meomnibus.platform.Platform;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class OmnibusTerminalPart extends BaseTerminalPart {
    public OmnibusTerminalPart(IPartItem<?> item) { super(item); }
    @Override public boolean onUseWithoutItem(Player player, Vec3 hit) {
        if(!super.onUseWithoutItem(player,hit) && player instanceof ServerPlayer serverPlayer)
            Platform.open(serverPlayer,this);
        return true;
    }
}
