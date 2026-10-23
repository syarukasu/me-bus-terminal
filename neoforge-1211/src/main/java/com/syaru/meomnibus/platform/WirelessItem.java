package com.syaru.meomnibus.platform;

import com.syaru.meomnibus.block.BaseWirelessTerminal;
import com.syaru.meomnibus.menu.TerminalAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class WirelessItem extends BaseWirelessTerminal {
    @Override public boolean openFromInventory(Player player, appeng.menu.locator.ItemMenuHostLocator locator) {
        var slot=locator.getPlayerInventorySlot();
        return slot!=null && openRemote(player,slot);
    }

    @Override protected TerminalAccess access(ServerPlayer player, int slot, ItemStack stack) {
        var grid=getLinkedGrid(stack,player.level(),null);
        var radio=new Radio(this,player,slot);
        return new TerminalAccess() {
            @Override public appeng.api.networking.IGrid grid() { return grid; }
            @Override public int lockedSlot() { return slot; }
            @Override public boolean canAccess(Player user) {
                return user==player && heldBy(player,slot,stack) && grid!=null
                        && getLinkedGrid(stack,player.level(),null)==grid && radio.connected()
                        && permitted(player,radio.getActionableNode());
            }
            @Override public boolean tick(net.minecraft.world.inventory.AbstractContainerMenu menu) {
                radio.tick();
                return radio.isValid() && radio.getLinkStatus().connected();
            }
        };
    }

    private static final class Radio extends appeng.helpers.WirelessTerminalMenuHost<WirelessItem> {
        Radio(WirelessItem item,Player player,int slot) {
            super(item,player,appeng.menu.locator.MenuLocators.forInventorySlot(slot),(p,sub)->item.openRemote(p,slot));
        }
        boolean connected() {
            updateConnectedAccessPoint();
            updateLinkStatus();
            return isValid() && getLinkStatus().connected();
        }
    }
}
