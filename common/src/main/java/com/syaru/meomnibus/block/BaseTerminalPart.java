package com.syaru.meomnibus.block;

import appeng.api.networking.IGrid;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.parts.reporting.AbstractDisplayPart;
import appeng.parts.PartModel;
import appeng.items.parts.PartModels;
import com.syaru.meomnibus.platform.Platform;
import com.syaru.meomnibus.menu.TerminalAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Uses AE2's display geometry, face placement and powered/channel models. */
public abstract class BaseTerminalPart extends AbstractDisplayPart implements TerminalAccess {
    @PartModels public static final IPartModel OFF = new PartModel(MODEL_BASE,Platform.id("part/terminal_off"),MODEL_STATUS_OFF);
    @PartModels public static final IPartModel ON = new PartModel(MODEL_BASE,Platform.id("part/terminal_on"),MODEL_STATUS_ON);
    @PartModels public static final IPartModel ACTIVE = new PartModel(MODEL_BASE,Platform.id("part/terminal_on"),MODEL_STATUS_HAS_CHANNEL);
    protected BaseTerminalPart(IPartItem<?> item) { super(item, true); }

    @Override public IPartModel getStaticModels() {
        return selectModel(OFF,ON,ACTIVE);
    }

    @Override public IGrid grid() { return getMainNode().getGrid(); }

    @Override public boolean canAccess(Player player) {
        var be = getBlockEntity();
        return be != null && !be.isRemoved() && be.getLevel() == player.level()
                && player.isAlive() && !player.isSpectator()
                && getHost().getPart(getSide()) == this
                && player.distanceToSqr(Vec3.atCenterOf(be.getBlockPos())) <= 64
                && getMainNode().isActive()
                && appeng.util.Platform.hasPermissions(new appeng.api.util.DimensionalBlockPos(be), player);
    }
}
