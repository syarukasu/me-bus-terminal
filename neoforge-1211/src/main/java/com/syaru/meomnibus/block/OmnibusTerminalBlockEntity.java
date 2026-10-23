package com.syaru.meomnibus.block;
import com.syaru.meomnibus.platform.OmnibusMod;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.HolderLookup;
public final class OmnibusTerminalBlockEntity extends BaseTerminal {
    public OmnibusTerminalBlockEntity(BlockPos pos,BlockState state) { super(OmnibusMod.ENTITY.get(),pos,state); }
    @Override public void loadTag(CompoundTag tag,HolderLookup.Provider registries) { super.loadTag(tag,registries);getMainNode().loadFromNBT(tag); }
    @Override public void saveAdditional(CompoundTag tag,HolderLookup.Provider registries) { super.saveAdditional(tag,registries);getMainNode().saveToNBT(tag); }
    @Override public void onChunkUnloaded() { getMainNode().destroy();super.onChunkUnloaded(); }
}
