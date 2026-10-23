package com.syaru.meomnibus.bus;
import appeng.api.config.*;
import appeng.api.stacks.AEKey;
import appeng.util.prioritylist.IPartitionList;
import com.syaru.meomnibus.platform.Platform;
import net.minecraft.nbt.*;
import net.minecraft.world.level.Level;

public final class BusSearch {
    public static String match(CompoundTag row,AEKey key,Level level) {
        var builder=IPartitionList.builder();
        var keys=row.getList("filters",Tag.TAG_COMPOUND);
        for(int i=0;i<Math.min(keys.size(),row.getInt("enabled"));i++)builder.add(Platform.key(keys.getCompound(i),level));
        if(row.getBoolean("fuzzy")) {
            for(var raw:row.getList("settings",Tag.TAG_COMPOUND)) {
                var s=(CompoundTag)raw;
                if(s.getString("name").equals("fuzzy_mode"))builder.fuzzyMode(FuzzyMode.valueOf(s.getString("value")));
            }
        }
        var filter=builder.build();
        boolean inverted=row.getBoolean("inverted");
        boolean allowed=filter.matchesFilter(key,inverted?IncludeExclude.BLACKLIST:IncludeExclude.WHITELIST);
        String kind=row.getString("kind");
        if(kind.equals("subnet")) {
            for(var raw:row.getList("settings",Tag.TAG_COMPOUND)) {
                var setting=(CompoundTag)raw;
                if(setting.getString("name").equals("subnet_mode")&&setting.getString("value").equals("POWER"))return "none";
            }
            return allowed?"candidate":"none";
        }
        if(kind.equals("storage")) {
            // With extraction filtering disabled, a nonmatching item can still be exposed.
            boolean extractUnfiltered=false;
            for(var raw:row.getList("settings",Tag.TAG_COMPOUND)) {
                var s=(CompoundTag)raw;
                if(s.getString("name").equals("filter_on_extract")&&s.getString("value").equals("NO"))extractUnfiltered=true;
            }
            return allowed || extractUnfiltered ? "candidate":"none";
        }
        if(!allowed)return "none";
        if(kind.equals("export")&&filter.isEmpty())return "none";
        return !inverted&&!filter.isEmpty()?"match":"possible";
    }
}
