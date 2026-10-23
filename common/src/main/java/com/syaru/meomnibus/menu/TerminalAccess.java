package com.syaru.meomnibus.menu;

import appeng.api.networking.IGrid;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

public interface TerminalAccess {
    IGrid grid();
    boolean canAccess(Player player);
    default boolean tick(AbstractContainerMenu menu) { return true; }
    default int lockedSlot() { return -1; }
}
