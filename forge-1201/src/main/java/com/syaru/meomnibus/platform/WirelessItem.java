package com.syaru.meomnibus.platform;

import com.syaru.meomnibus.block.BaseWirelessTerminal;
import com.syaru.meomnibus.menu.TerminalAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class WirelessItem extends BaseWirelessTerminal {
    @Override public boolean openFromInventory(Player player, int slot) { return openRemote(player, slot); }

    @Override protected TerminalAccess access(ServerPlayer player, int slot, ItemStack stack) {
        var grid = getLinkedGrid(stack, player.level(), null);
        var radio = new appeng.helpers.WirelessTerminalMenuHost(player, slot, stack,
                (p, sub) -> openRemote(p, slot));
        return new TerminalAccess() {
            private boolean active = true;
            @Override public appeng.api.networking.IGrid grid() { return grid; }
            @Override public int lockedSlot() { return slot; }
            @Override public boolean canAccess(Player user) {
                return active && user==player && heldBy(player,slot,stack) && grid!=null
                        && getLinkedGrid(stack,player.level(),null)==grid && radio.rangeCheck()
                        && hasPower(player,.5,stack) && permitted(player,radio.getActionableNode());
            }
            @Override public boolean tick(net.minecraft.world.inventory.AbstractContainerMenu menu) {
                active=radio.onBroadcastChanges(menu);
                return active;
            }
        };
    }
}
