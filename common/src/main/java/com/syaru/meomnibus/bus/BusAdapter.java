package com.syaru.meomnibus.bus;

import appeng.api.config.Setting;
import appeng.api.config.Settings;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.core.definitions.AEItems;
import appeng.parts.automation.*;
import appeng.parts.storagebus.StorageBusPart;
import appeng.util.ConfigInventory;
import com.syaru.meomnibus.platform.Platform;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.player.Player;
import java.util.*;

public class BusAdapter {
    public final UpgradeablePart part;
    public final String kind;
    protected BusAdapter(UpgradeablePart part, String kind) { this.part = part; this.kind = kind; }
    public static BusAdapter of(Object owner) {
        if(owner instanceof StorageBusPart p) return new BusAdapter(p, "storage");
        if(owner instanceof ImportBusPart p) return new BusAdapter(p, "import");
        if(owner instanceof ExportBusPart p) return new BusAdapter(p, "export");
        return SubnetBusAdapter.tryCreate(owner);
    }
    public net.minecraft.world.level.block.entity.BlockEntity blockEntity() { return part.getBlockEntity(); }
    public net.minecraft.core.Direction side() { return part.getSide(); }
    public boolean acceptsNode(appeng.api.networking.IGridNode node) { return true; }
    public boolean supportsPriority() { return part instanceof StorageBusPart; }
    public boolean validSetting(String name,String value) {
        var s=setting(name);
        return s!=null&&enabled(s)&&s.getValues().stream().anyMatch(v->v.name().equals(value));
    }
    public ConfigInventory config() {
        return part instanceof StorageBusPart p ? p.getConfig() : ((IOBusPart)part).getConfig();
    }
    public int enabledSlots() { return Math.min(config().size(),18+9*part.getInstalledUpgrades(AEItems.CAPACITY_CARD)); }
    public boolean canEdit(Player player) {
        var be = part.getBlockEntity();
        return be != null && !be.isRemoved() && be.getLevel()!=null
                && be.getLevel().hasChunkAt(be.getBlockPos()) && part.getHost().getPart(part.getSide()) == part
                && appeng.util.Platform.hasPermissions(new appeng.api.util.DimensionalBlockPos(be), player);
    }
    public boolean enabled(Setting<?> s) {
        if(s==Settings.FUZZY_MODE) return part.isUpgradedWith(AEItems.FUZZY_CARD);
        if(s==Settings.REDSTONE_CONTROLLED) return part.isUpgradedWith(AEItems.REDSTONE_CARD);
        if(s==Settings.CRAFT_ONLY) return part.isUpgradedWith(AEItems.CRAFTING_CARD);
        return true;
    }
    public Setting<?> setting(String name) {
        return part.getConfigManager().getSettings().stream().filter(s->s.getName().equals(name)).findFirst().orElse(null);
    }
    public CompoundTag snapshot() {
        var tag = new CompoundTag();
        var be = part.getBlockEntity(); var level = be.getLevel();
        tag.putString("kind",kind);
        tag.put("icon",Platform.stackTag(new net.minecraft.world.item.ItemStack(part.getPartItem()),level));
        tag.putBoolean("supportsPriority",supportsPriority());
        tag.putString("dimension",level.dimension().location().toString());
        tag.putLong("pos",be.getBlockPos().asLong());
        tag.putInt("side",part.getSide().ordinal());
        tag.putBoolean("online",part.getMainNode().isActive());
        var target=be.getBlockPos().relative(part.getSide());
        tag.putString("target",level.hasChunkAt(target)?level.getBlockState(target).getBlock().getDescriptionId():"omnibus.unloaded");
        tag.putInt("enabled",enabledSlots());
        tag.putBoolean("inverted",part.isUpgradedWith(AEItems.INVERTER_CARD));
        tag.putBoolean("fuzzy",part.isUpgradedWith(AEItems.FUZZY_CARD));
        tag.putInt("priority",part instanceof StorageBusPart p ? p.getPriority():0);
        var keys = new ListTag();
        for(int i=0;i<config().size();i++) {
            var key=config().getKey(i);
            keys.add(key==null?new CompoundTag():Platform.keyTag(key,level));
        }
        tag.put("filters",keys);
        var settings = new ListTag();
        for(var s:part.getConfigManager().getSettings().stream().sorted(Comparator.comparing(Setting::getName)).toList()) {
            var t = new CompoundTag(); t.putString("name",s.getName()); t.putString("value",s.getValue(part.getConfigManager()).name());
            t.putBoolean("enabled",enabled(s)); var choices=new ListTag();
            for(var v:s.getValues()) choices.add(StringTag.valueOf(v.name()));
            t.put("choices",choices); settings.add(t);
        }
        tag.put("settings",settings);
        var upgrades=new ListTag();
        for(int i=0;i<part.getUpgrades().size();i++)upgrades.add(Platform.stackTag(part.getUpgrades().getStackInSlot(i),level));
        tag.put("upgrades",upgrades);
        return tag;
    }
    public void filter(int slot, AEKey key) {
        if(slot<0 || slot>=enabledSlots()) throw new IllegalArgumentException("slot");
        config().setStack(slot,key==null?null:new GenericStack(key,1));
        part.getHost().markForSave();
    }
    public void paste(List<AEKey> keys) {
        if(keys.size()>enabledSlots()) throw new IllegalArgumentException("capacity");
        config().beginBatch();
        try {
            config().clear();
            for(int i=0;i<keys.size();i++) if(keys.get(i)!=null) config().setStack(i,new GenericStack(keys.get(i),1));
        } finally { config().endBatch(); }
        part.getHost().markForSave();
    }
    public List<AEKey> copyFilters() {
        var keys=new ArrayList<AEKey>();
        for(int i=0;i<enabledSlots();i++) keys.add(config().getKey(i));
        while(!keys.isEmpty() && keys.get(keys.size()-1)==null) keys.remove(keys.size()-1);
        return keys;
    }
    public void priority(int value) {
        if(!(part instanceof StorageBusPart p)) throw new IllegalArgumentException("priority");
        p.setPriority(value);
    }
    public void set(String name,String value) {
        var s=setting(name);
        if(s==null || !enabled(s)) throw new IllegalArgumentException("setting");
        s.setFromString(part.getConfigManager(),value);
    }
}
