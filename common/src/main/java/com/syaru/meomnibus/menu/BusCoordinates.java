package com.syaru.meomnibus.menu;

import net.minecraft.core.BlockPos;

public final class BusCoordinates {
    private BusCoordinates() { }

    public static String format(BlockPos pos) {
        return "X: " + pos.getX() + " / Y: " + pos.getY() + " / Z: " + pos.getZ();
    }
}
