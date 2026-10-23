package com.syaru.meomnibus.menu;

import appeng.api.stacks.*;
import com.syaru.meomnibus.network.OmnibusGridService;
import com.syaru.meomnibus.platform.Platform;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import java.util.*;
import java.util.function.LongConsumer;

public final class OmnibusTerminalMenu extends AbstractContainerMenu {
    public final TerminalAccess terminal;
    public final String session;
    public final Map<Long,CompoundTag> rows=new LinkedHashMap<>();
    public String message="";
    public int total;
    public long clientGeneration;
    private final ServerPlayer owner;
    private OmnibusGridService service;
    private final LinkedHashSet<Long> pending=new LinkedHashSet<>();
    private final LongConsumer listener=pending::add;
    private List<AEKey> clipboard;
    private long requestTick=-1;
    private int requests;
    public OmnibusTerminalMenu(int id,Inventory inv,TerminalAccess terminal,String session) {
        super(Platform.menu(),id);this.terminal=terminal;this.session=session;
        owner=inv.player instanceof ServerPlayer p?p:null;
        for(int y=0;y<3;y++) for(int x=0;x<9;x++) addSlot(new Slot(inv,9+y*9+x,8+x*18,150+y*18));
        for(int x=0;x<9;x++) addSlot(new Slot(inv,x,8+x*18,208));
    }
    public static OmnibusTerminalMenu fromNetwork(int id,Inventory inv,net.minecraft.network.FriendlyByteBuf buf) {
        return new OmnibusTerminalMenu(id,inv,null,buf.readUtf(64));
    }
    @Override public boolean stillValid(Player player) { return terminal==null ? player.level().isClientSide() : terminal.canAccess(player); }
    private void detach() { if(service!=null)service.unsubscribe(listener);service=null;pending.clear(); }
    @Override public void removed(Player player) { detach();super.removed(player); }
    @Override public void broadcastChanges() {
        super.broadcastChanges();
        if(owner==null) return;
        if(!stillValid(owner) || !terminal.tick(this)) { detach();owner.closeContainer();return; }
        var grid=terminal.grid();
        if(grid==null) { detach();owner.closeContainer();return; }
        var current=grid.getService(OmnibusGridService.class);
        if(service!=current) {
            detach();service=current;pending.addAll(service.subscribe(listener));
            var reset=new CompoundTag();reset.putBoolean("reset",true);send(reset);
        }
        var updates=new ListTag();
        // Bounded per-view synchronization; never copy the entire world or storage inventory.
        for(int n=0;n<16 && !pending.isEmpty();n++) {
            long id=pending.iterator().next();pending.remove(id);
            var e=service.refresh(id);
            pending.remove(id);
            var row=e==null?new CompoundTag():e.state();
            row.putLong("id",id);
            if(e==null) row.putBoolean("removed",true);else row.putLong("revision",e.revision);
            updates.add(row);
        }
        if(!updates.isEmpty()) {
            var tag=new CompoundTag();tag.put("rows",updates);tag.putInt("total",service.size());send(tag);
        }
    }
    private void send(CompoundTag tag) { Platform.send(owner,containerId,session,tag); }
    private void result(String key) { var tag=new CompoundTag();tag.putString("message",key);send(tag); }
    private void immediate(OmnibusGridService.Entry entry) {
        var row=entry.state();row.putLong("id",entry.id);row.putLong("revision",entry.revision);
        var list=new ListTag();list.add(row);var tag=new CompoundTag();tag.put("rows",list);
        send(tag);pending.remove(entry.id);
    }
    public void receive(CompoundTag tag) {
        if(tag.getBoolean("reset"))rows.clear();
        for(var raw:tag.getList("rows",Tag.TAG_COMPOUND)) {
            var row=(CompoundTag)raw;long id=row.getLong("id");
            if(row.getBoolean("removed"))rows.remove(id);else rows.put(id,row);
        }
        if(tag.contains("total"))total=tag.getInt("total");
        if(tag.contains("message"))message=tag.getString("message");
        clientGeneration++;
    }
    public void command(ServerPlayer player,CompoundTag request) {
        if(player!=owner || !stillValid(player) || service==null
                || terminal.grid()!=service.grid) return;
        long tick=player.level().getGameTime();
        if(tick!=requestTick) { requestTick=tick;requests=0; }
        if(++requests>8) return;
        long[] ids=request.getLongArray("ids"), revisions=request.getLongArray("revisions");
        if(ids.length==0 || ids.length>128 || ids.length!=revisions.length) return;
        var selected=new ArrayList<OmnibusGridService.Entry>();
        var unique=new HashSet<Long>();
        for(int i=0;i<ids.length;i++) {
            var e=service.refresh(ids[i]);
            if(e==null || !unique.add(ids[i]) || !e.bus.canEdit(player)) { result("unavailable");return; }
            if(e.revision!=revisions[i]) { immediate(e);result("conflict");return; }
            selected.add(e);
        }
        String op=request.getString("op");
        try {
            if(op.equals("copy")) {
                if(selected.size()!=1) return;
                clipboard=selected.get(0).bus.copyFilters();result("copied");return;
            }
            if(op.equals("locate")) {
                if(selected.size()!=1) return;
                var state=selected.get(0).state();
                var pos=net.minecraft.core.BlockPos.of(state.getLong("pos"));
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(state.getString("dimension")+"  "+BusCoordinates.format(pos)+"  "+selected.get(0).bus.side()));
                result("located");return;
            }
            int slot=request.getInt("slot"), priority=request.getInt("priority");
            String name=request.getString("name"),value=request.getString("value");
            GenericStack carried=GenericStack.fromItemStack(getCarried());
            AEKey key=request.getBoolean("clear") || carried==null ? null:carried.what();
            // Validate the whole batch before modifying any real bus.
            for(var e:selected) {
                var bus=e.bus;
                switch(op) {
                    case "filter" -> { if(slot<0||slot>=bus.enabledSlots())throw new IllegalArgumentException(); }
                    case "paste" -> { if(clipboard==null||clipboard.size()>bus.enabledSlots())throw new IllegalArgumentException(); }
                    case "clear" -> { }
                    case "priority" -> { if(!bus.supportsPriority())throw new IllegalArgumentException(); }
                    case "setting" -> {
                        if(!bus.validSetting(name,value))throw new IllegalArgumentException();
                    }
                    default -> throw new IllegalArgumentException();
                }
            }
            for(var e:selected) {
                switch(op) {
                    case "filter" -> e.bus.filter(slot,key);
                    case "paste" -> e.bus.paste(clipboard);
                    case "clear" -> e.bus.paste(List.of());
                    case "priority" -> e.bus.priority(priority);
                    case "setting" -> e.bus.set(name,value);
                }
                var refreshed=service.refresh(e.id);
                if(refreshed!=null)immediate(refreshed);
            }
            result("updated");
        } catch(IllegalArgumentException ex) { result("unsupported"); }
    }
    @Override public ItemStack quickMoveStack(Player player,int slot) { return ItemStack.EMPTY; }
    @Override public void clicked(int slotId, int button, ClickType type, Player player) {
        int locked=terminal==null ? -1 : terminal.lockedSlot();
        if(locked>=0 && ((slotId>=0 && slotId<slots.size() && slots.get(slotId).getSlotIndex()==locked)
                || (type==ClickType.SWAP && button==locked))) return;
        super.clicked(slotId,button,type,player);
    }
}
