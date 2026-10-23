package com.syaru.meomnibus.network;

import appeng.api.networking.*;
import com.syaru.meomnibus.bus.BusAdapter;
import net.minecraft.nbt.CompoundTag;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongConsumer;

/** A grid-local index, not a world scanner. No viewers means no configuration polling. */
public final class OmnibusGridService implements IGridService, IGridServiceProvider {
    public static final int REFRESH_PER_TICK = 32;
    private static final AtomicLong IDS = new AtomicLong();
    public final IGrid grid;
    private final IdentityHashMap<IGridNode,Long> nodes = new IdentityHashMap<>();
    private final NavigableMap<Long,Entry> entries = new TreeMap<>();
    private final Set<LongConsumer> viewers = new LinkedHashSet<>();
    private long cursor;
    public OmnibusGridService(IGrid grid) { this.grid=grid; }
    public static final class Entry {
        public final long id;
        public final IGridNode node;
        public final BusAdapter bus;
        public long revision;
        private CompoundTag state;
        Entry(long id,IGridNode node,BusAdapter bus) { this.id=id;this.node=node;this.bus=bus; }
        public CompoundTag state() { return state==null?new CompoundTag():state.copy(); }
    }
    @Override public void addNode(IGridNode node,CompoundTag saved) {
        var bus=BusAdapter.of(node.getOwner());
        if(bus==null || !bus.acceptsNode(node) || nodes.containsKey(node)) return;
        long id=IDS.incrementAndGet();
        nodes.put(node,id);entries.put(id,new Entry(id,node,bus));changed(id);
    }
    @Override public void removeNode(IGridNode node) {
        var id=nodes.remove(node);
        if(id!=null) { entries.remove(id);changed(id); }
    }
    public List<Long> subscribe(LongConsumer viewer) { viewers.add(viewer);return new ArrayList<>(entries.keySet()); }
    public void unsubscribe(LongConsumer viewer) { viewers.remove(viewer); }
    private void changed(long id) { for(var v:List.copyOf(viewers)) v.accept(id); }
    public int size() { return entries.size(); }
    public Entry get(long id) { return entries.get(id); }
    public Entry refresh(long id) {
        var e=entries.get(id);
        if(e==null) return null;
        var be=e.bus.blockEntity();
        if(e.node.getGrid()!=grid || be==null || be.isRemoved() || be.getLevel()==null
                || !be.getLevel().hasChunkAt(be.getBlockPos())) return null;
        var state=e.bus.snapshot();
        if(!state.equals(e.state)) { e.state=state;e.revision++;changed(id); }
        return e;
    }
    @Override public void onServerEndTick() {
        if(viewers.isEmpty() || entries.isEmpty()) return;
        int count=Math.min(REFRESH_PER_TICK,entries.size());
        for(int i=0;i<count;i++) {
            var next=entries.higherKey(cursor);
            if(next==null) next=entries.firstKey();
            cursor=next;refresh(next);
        }
    }
}
