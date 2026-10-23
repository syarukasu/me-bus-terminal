package com.syaru.meomnibus.block;

import appeng.api.networking.*;
import appeng.blockentity.AEBaseBlockEntity;
import appeng.me.helpers.IGridConnectedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import java.util.EnumSet;

public abstract class BaseTerminal extends AEBaseBlockEntity implements IGridConnectedBlockEntity, com.syaru.meomnibus.menu.TerminalAccess {
    private final IManagedGridNode node = GridHelper.createManagedNode(this, new IGridNodeListener<BaseTerminal>() {
        @Override public void onSaveChanges(BaseTerminal host, IGridNode node) { host.saveChanges(); }
    }).setInWorldNode(true).setTagName("grid").setIdlePowerUsage(1)
            .setFlags(GridFlags.REQUIRE_CHANNEL).setExposedOnSides(EnumSet.allOf(Direction.class));
    protected BaseTerminal(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }
    @Override public IManagedGridNode getMainNode() { return node; }
    @Override public IGrid grid() { return node.getGrid(); }
    @Override public void onReady() {
        super.onReady();
        node.setVisualRepresentation(com.syaru.meomnibus.platform.OmnibusMod.ITEM.get());
        node.create(level, worldPosition);
    }
    @Override public void setRemoved() { node.destroy(); super.setRemoved(); }
    @Override public void clearRemoved() { super.clearRemoved(); scheduleInit(); }
    public boolean canAccess(Player player) {
        return level != null && !isRemoved() && !player.isSpectator()
                && level == player.level() && player.distanceToSqr(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5) <= 64
                && node.isActive() && appeng.util.Platform.hasPermissions(new appeng.api.util.DimensionalBlockPos(this), player);
    }
}
