package com.syaru.meomnibus.bus;

import appeng.api.networking.IGridNode;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.stacks.AEKey;
import com.syaru.meomnibus.platform.Platform;
import java.lang.reflect.*;
import java.util.*;
import net.minecraft.core.Direction;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Optional bridge to ME Subnet Bus' public configuration API, never a hard dependency. */
final class SubnetBusAdapter extends BusAdapter {
    private static final String API_CLASS="com.syaru.mesubnetbus.BaseSubnetPart";
    private final IPart subnet;
    private final Access api;
    private record Access(Field mode,Field reversed,Field priority,Field exclude,Field filters,
                          Method configure,Method canConfigure,Method blockEntity,Method side,Method usable) {
        Access(Class<?> type) throws ReflectiveOperationException {
            this(type.getField("mode"),type.getField("reversed"),type.getField("priority"),
                    type.getField("excludeFilter"),type.getField("filters"),
                    type.getMethod("configure",type.getField("mode").getType(),boolean.class,int.class,boolean.class,List.class),
                    type.getMethod("canConfigure",Player.class),type.getMethod("blockEntity"),
                    type.getMethod("side"),type.getMethod("usable"));
        }
    }
    private static final ClassValue<Optional<Access>> APIS=new ClassValue<>() {
        @Override protected Optional<Access> computeValue(Class<?> type) {
            try { return Optional.of(new Access(type)); }
            catch(ReflectiveOperationException ex) {
                org.slf4j.LoggerFactory.getLogger(SubnetBusAdapter.class).warn("Unsupported ME Subnet Bus API; remote editing disabled",ex);
                return Optional.empty();
            }
        }
    };
    static BusAdapter tryCreate(Object owner) {
        if(!(owner instanceof IPart part))return null;
        for(Class<?> type=owner.getClass();type!=null;type=type.getSuperclass()) {
            if(type.getName().equals(API_CLASS))return APIS.get(type).map(api->new SubnetBusAdapter(part,api)).orElse(null);
        }
        return null;
    }
    private SubnetBusAdapter(IPart part,Access api) { super(null,"subnet");this.subnet=part;this.api=api; }
    private Object read(Field field) {
        try { return field.get(subnet); }
        catch(ReflectiveOperationException ex) { throw new IllegalArgumentException("Subnet API",ex); }
    }
    private Object call(Method method,Object... args) {
        try { return method.invoke(subnet,args); }
        catch(ReflectiveOperationException ex) { throw new IllegalArgumentException("Subnet API",ex); }
    }
    @Override public BlockEntity blockEntity() { return (BlockEntity)call(api.blockEntity); }
    @Override public Direction side() { return (Direction)call(api.side); }
    @Override public boolean acceptsNode(IGridNode node) { return node==subnet.getGridNode(); }
    @Override public boolean supportsPriority() { return true; }
    @Override public int enabledSlots() { return 9; }
    @Override public boolean canEdit(Player player) {
        var be=blockEntity();
        return be!=null&&!be.isRemoved()&&be.getLevel()!=null&&be.getLevel().hasChunkAt(be.getBlockPos())
                &&be instanceof IPartHost host&&host.getPart(side())==subnet&&(boolean)call(api.canConfigure,player);
    }
    @SuppressWarnings("unchecked")
    @Override public List<AEKey> copyFilters() { return new ArrayList<>((List<AEKey>)read(api.filters)); }
    private void configure(Object mode,boolean reversed,int priority,boolean exclude,List<AEKey> keys) {
        call(api.configure,mode,reversed,priority,exclude,keys);
    }
    @Override public void paste(List<AEKey> keys) {
        if(keys.size()>enabledSlots())throw new IllegalArgumentException("capacity");
        configure(read(api.mode),(boolean)read(api.reversed),(int)read(api.priority),(boolean)read(api.exclude),
                keys.stream().filter(Objects::nonNull).toList());
    }
    @Override public void filter(int slot,AEKey key) {
        if(slot<0||slot>=enabledSlots())throw new IllegalArgumentException("slot");
        var keys=copyFilters();
        while(keys.size()<=slot)keys.add(null);
        keys.set(slot,key);paste(keys);
    }
    @Override public void priority(int value) {
        configure(read(api.mode),(boolean)read(api.reversed),value,(boolean)read(api.exclude),copyFilters());
    }
    private List<String> modes() {
        return Arrays.stream(api.mode.getType().getEnumConstants()).map(v->((Enum<?>)v).name()).toList();
    }
    @Override public boolean validSetting(String name,String value) {
        return switch(name) {
            case "subnet_mode" -> modes().contains(value);
            case "subnet_direction","subnet_exclude" -> value.equals("YES")||value.equals("NO");
            default -> false;
        };
    }
    @Override public void set(String name,String value) {
        if(!validSetting(name,value))throw new IllegalArgumentException("setting");
        Object mode=read(api.mode);
        boolean reversed=(boolean)read(api.reversed),exclude=(boolean)read(api.exclude);
        if(name.equals("subnet_mode"))mode=Arrays.stream(api.mode.getType().getEnumConstants()).filter(v->((Enum<?>)v).name().equals(value)).findFirst().orElseThrow();
        if(name.equals("subnet_direction"))reversed=value.equals("YES");
        if(name.equals("subnet_exclude"))exclude=value.equals("YES");
        configure(mode,reversed,(int)read(api.priority),exclude,copyFilters());
    }
    private CompoundTag settingTag(String name,String value,List<String> options) {
        var tag=new CompoundTag();tag.putString("name",name);tag.putString("value",value);tag.putBoolean("enabled",true);
        var choices=new ListTag();options.forEach(v->choices.add(StringTag.valueOf(v)));tag.put("choices",choices);
        return tag;
    }
    @Override public CompoundTag snapshot() {
        var tag=new CompoundTag();var be=blockEntity();var level=be.getLevel();var side=side();
        tag.putString("kind",kind);tag.put("icon",Platform.stackTag(new ItemStack(subnet.getPartItem()),level));
        tag.putString("dimension",level.dimension().location().toString());tag.putLong("pos",be.getBlockPos().asLong());tag.putInt("side",side.ordinal());
        tag.putBoolean("online",(boolean)call(api.usable));
        var target=be.getBlockPos().relative(side);
        tag.putString("target",level.hasChunkAt(target)?level.getBlockState(target).getBlock().getDescriptionId():"omnibus.unloaded");
        tag.putInt("enabled",enabledSlots());tag.putBoolean("inverted",(boolean)read(api.exclude));
        tag.putBoolean("fuzzy",false);tag.putBoolean("supportsPriority",true);tag.putInt("priority",(int)read(api.priority));
        var filters=new ListTag();var keys=copyFilters();
        for(int i=0;i<9;i++)filters.add(i<keys.size()&&keys.get(i)!=null?Platform.keyTag(keys.get(i),level):new CompoundTag());
        tag.put("filters",filters);
        var settings=new ListTag();
        settings.add(settingTag("subnet_mode",((Enum<?>)read(api.mode)).name(),modes()));
        settings.add(settingTag("subnet_direction",(boolean)read(api.reversed)?"YES":"NO",List.of("NO","YES")));
        settings.add(settingTag("subnet_exclude",(boolean)read(api.exclude)?"YES":"NO",List.of("NO","YES")));
        tag.put("settings",settings);tag.put("upgrades",new ListTag());return tag;
    }
}
