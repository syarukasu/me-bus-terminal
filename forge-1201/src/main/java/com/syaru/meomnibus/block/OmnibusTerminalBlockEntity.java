package com.syaru.meomnibus.block;
import com.syaru.meomnibus.platform.OmnibusMod;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

public final class OmnibusTerminalBlockEntity extends BaseTerminal {
    public OmnibusTerminalBlockEntity(BlockPos pos,BlockState state) { super(OmnibusMod.ENTITY.get(),pos,state); }
    @Override public void loadTag(CompoundTag tag) { super.loadTag(tag);getMainNode().loadFromNBT(tag); }
    @Override public void saveAdditional(CompoundTag tag) { super.saveAdditional(tag);getMainNode().saveToNBT(tag); }
    @Override public void onChunkUnloaded() { getMainNode().destroy();super.onChunkUnloaded(); }
}
