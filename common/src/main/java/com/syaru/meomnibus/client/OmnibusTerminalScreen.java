package com.syaru.meomnibus.client;

import appeng.api.stacks.AEKey;
import appeng.core.definitions.AEParts;
import com.syaru.meomnibus.menu.OmnibusTerminalMenu;
import com.syaru.meomnibus.menu.BusCoordinates;
import com.syaru.meomnibus.network.OmnibusPackets;
import com.syaru.meomnibus.platform.Platform;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.*;
import java.util.*;

public final class OmnibusTerminalScreen extends AbstractContainerScreen<OmnibusTerminalMenu> {
    public static final Comparator<CompoundTag> ROW_ORDER=Comparator
            .comparingInt((CompoundTag r)->switch(r.getString("kind")){case "storage"->0;case "import"->1;default->2;})
            .thenComparing(r->r.getString("dimension"))
            .thenComparingLong(r->r.getLong("pos"))
            .thenComparingInt(r->r.getInt("side"))
            .thenComparingLong(r->r.getLong("id"));
    private EditBox search,priority;
    private final List<Button> detailButtons=new ArrayList<>();
    private final List<CompoundTag> visible=new ArrayList<>();
    private final LinkedHashSet<Long> selected=new LinkedHashSet<>();
    private final Map<Long,CompoundTag> cachedRows=new HashMap<>();
    private final Map<Long,String> searchTerms=new HashMap<>();
    private final Map<Long,Long> shownRevisions=new HashMap<>();
    private AEKey activeQuery;
    private long focus=-1,generation=-1;
    private int page,filterPage;
    private String kind="all";
    private AEKey queryKey;
    private String queryText="";
    private boolean dirty=true;
    private static final int INK=0x30383f;
    public OmnibusTerminalScreen(OmnibusTerminalMenu menu,Inventory inv,Component title) {
        super(menu,inv,title);imageWidth=398;imageHeight=232;
    }
    private net.minecraft.network.chat.MutableComponent tr(String s) { return Component.translatable("omnibus."+s); }
    private Button button(int x,int y,int w,int h,Component text,Button.OnPress action) {
        var b=Button.builder(text,action).bounds(leftPos+x,topPos+y,w,h).build();
        b.setTooltip(Tooltip.create(text));return addRenderableWidget(b);
    }
    private Button iconButton(int x,int y,OmnibusAppearance.ActionIcon icon,Component tooltip,Button.OnPress action) {
        var b=new Button(leftPos+x,topPos+y,18,18,Component.empty(),action,supplier->supplier.get()) {
            @Override protected void renderWidget(GuiGraphics g,int mx,int my,float partial) {
                super.renderWidget(g,mx,my,partial);
                icon.render(g,getX()+1,getY()+1);
            }
        };
        b.setTooltip(Tooltip.create(tooltip));return addRenderableWidget(b);
    }
    @Override protected void init() {
        super.init();
        cachedRows.clear();searchTerms.clear();
        search=new EditBox(font,leftPos+8,topPos+24,160,16,tr("search"));search.setMaxLength(160);
        search.setHint(tr("search"));search.setValue(queryText);
        search.setResponder(s->{queryText=s;page=0;dirty=true;});addRenderableWidget(search);
        var subnetIcon=net.minecraft.core.registries.BuiltInRegistries.ITEM.stream()
                .filter(item->net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString().equals("me_subnet_bus:subnet_bus"))
                .findFirst().map(ItemStack::new).orElse(ItemStack.EMPTY);
        String[] kinds=subnetIcon.isEmpty()?new String[]{"all","storage","import","export"}:new String[]{"all","storage","import","export","subnet"};
        int tabWidth=160/kinds.length;
        for(int i=0;i<kinds.length;i++) {
            String k=kinds[i];
            var tab=new Button(leftPos+8+i*tabWidth,topPos+43,tabWidth-1,18,Component.empty(),b->{kind=k;page=0;dirty=true;},supplier->supplier.get()) {
                @Override protected void renderWidget(GuiGraphics g,int mx,int my,float partial) {
                    super.renderWidget(g,mx,my,partial);
                    if(k.equals("all"))g.drawCenteredString(font,tr(k),getX()+tabWidth/2,getY()+5,0xffffff);
                    else if(k.equals("subnet"))OmnibusAppearance.renderItem(g,subnetIcon,getX()+(tabWidth-16)/2,getY()+1);
                    else {
                        var item=switch(k){case "storage"->AEParts.STORAGE_BUS;case "import"->AEParts.IMPORT_BUS;default->AEParts.EXPORT_BUS;};
                        OmnibusAppearance.renderItem(g,new ItemStack(item.asItem()),getX()+(tabWidth-16)/2,getY()+1);
                    }
                    if(kind.equals(k))g.fill(getX()+2,getY()+16,getX()+tabWidth-3,getY()+18,0xff50b29a);
                }
            };
            tab.setTooltip(Tooltip.create(tr(k)));addRenderableWidget(tab);
        }
        button(170,24,14,16,Component.literal("?"),b->{
            var stack=appeng.api.stacks.GenericStack.fromItemStack(menu.getCarried());
            queryKey=stack==null?null:stack.what();page=0;dirty=true;
        }).setTooltip(Tooltip.create(tr("item_search")));
        button(8,128,18,18,Component.literal("<"),b->{page=Math.max(0,page-1);});
        button(150,128,18,18,Component.literal(">"),b->{page=Math.min(Math.max(0,(visible.size()-1)/3),page+1);});
        priority=new EditBox(font,leftPos+231,topPos+205,75,16,tr("priority"));
        priority.setMaxLength(11);priority.setFilter(s->s.matches("-?[0-9]*"));addRenderableWidget(priority);
        dirty=true;generation=-1;rebuildDetails();
    }
    private CompoundTag row() { return menu.rows.get(focus); }
    private ItemStack busIcon(CompoundTag row) { return Platform.stack(row.getCompound("icon"),minecraft.level); }
    private List<CompoundTag> selectedRows() {
        return selected.stream().map(menu.rows::get).filter(Objects::nonNull).toList();
    }
    private void send(String op,java.util.function.Consumer<CompoundTag> extra,boolean onlyFocus) {
        var list=onlyFocus?(row()==null?List.<CompoundTag>of():List.of(row())):selectedRows();
        if(list.isEmpty())return;
        var tag=new CompoundTag();tag.putString("op",op);
        tag.putLongArray("ids",list.stream().mapToLong(t->t.getLong("id")).toArray());
        tag.putLongArray("revisions",list.stream().mapToLong(t->shownRevisions.getOrDefault(t.getLong("id"),-1L)).toArray());
        extra.accept(tag);
        Platform.control(new OmnibusPackets(menu.containerId,menu.session,tag));
    }
    private void detail(int x,int y,int w,int h,Component label,Button.OnPress action) { detailButtons.add(button(x,y,w,h,label,action)); }
    private void rebuildDetails() {
        for(var b:detailButtons)removeWidget(b);detailButtons.clear();
        shownRevisions.clear();
        for(var shown:selectedRows())shownRevisions.put(shown.getLong("id"),shown.getLong("revision"));
        var r=row();priority.visible=r!=null && r.getBoolean("supportsPriority");
        if(r==null)return;
        shownRevisions.put(r.getLong("id"),r.getLong("revision"));
        detailButtons.add(iconButton(332,7,OmnibusAppearance.ActionIcon.COPY,tr("copy"),b->send("copy",t->{},true)));
        detailButtons.add(iconButton(352,7,OmnibusAppearance.ActionIcon.PASTE,tr("paste"),b->send("paste",t->{},false)));
        detailButtons.add(iconButton(372,7,OmnibusAppearance.ActionIcon.CLEAR,tr("clear"),b->send("clear",t->{},false)));
        var pos=net.minecraft.core.BlockPos.of(r.getLong("pos"));
        detail(190,26,200,16,Component.literal(BusCoordinates.format(pos)),b->send("locate",t->{},true));
        detailButtons.get(detailButtons.size()-1).setTooltip(Tooltip.create(Component.literal(BusCoordinates.format(pos)+"\n"+r.getString("dimension")+" / "+net.minecraft.core.Direction.values()[r.getInt("side")])));
        detail(190,99,18,16,Component.literal("<"),b->{filterPage=Math.max(0,filterPage-1);});
        detail(372,99,18,16,Component.literal(">"),b->{filterPage=Math.min((r.getInt("enabled")-1)/18,filterPage+1);});
        int y=119;
        for(var raw:r.getList("settings",Tag.TAG_COMPOUND)) {
            var s=(CompoundTag)raw;
            if(y>186)break;
            var choices=s.getList("choices",Tag.TAG_STRING);
            String current=s.getString("value"),name=s.getString("name");
            int index=0;for(int i=0;i<choices.size();i++)if(choices.getString(i).equals(current))index=i;
            String next=choices.getString((index+1)%choices.size());
            boolean mixed=selectedRows().stream().anyMatch(other->{
                for(var value:other.getList("settings",Tag.TAG_COMPOUND)) {
                    var os=(CompoundTag)value;
                    if(os.getString("name").equals(name))return !os.getString("value").equals(current);
                }
                return true;
            });
            detail(190,y,200,16,Component.translatable("omnibus.setting."+name).append(": ").append(mixed?tr("mixed"):Component.translatable("omnibus.value."+current)),
                    b->send("setting",t->{t.putString("name",name);t.putString("value",next);},false));
            var b=detailButtons.get(detailButtons.size()-1);
            b.active=selectedRows().stream().allMatch(other->{
                for(var value:other.getList("settings",Tag.TAG_COMPOUND)) {
                    var os=(CompoundTag)value;
                    if(os.getString("name").equals(name))return os.getBoolean("enabled")&&os.getList("choices",Tag.TAG_STRING).contains(StringTag.valueOf(next));
                }
                return false;
            });
            y+=17;
        }
        if(priority.visible) {
            if(!priority.isFocused())priority.setValue(Integer.toString(r.getInt("priority")));
            detail(310,204,18,18,Component.literal("\u2713"),b->{
                try {int value=Integer.parseInt(priority.getValue());send("priority",t->t.putInt("priority",value),false);}
                catch(NumberFormatException ignored){menu.message="invalid_priority";}
            });
        }
    }
    private void update() {
        if(generation!=menu.clientGeneration) {
            generation=menu.clientGeneration;dirty=true;
            selected.retainAll(menu.rows.keySet());
            if(!menu.rows.containsKey(focus)){focus=-1;filterPage=0;}
            rebuildDetails();
        }
        if(!dirty)return;dirty=false;visible.clear();
        cachedRows.keySet().retainAll(menu.rows.keySet());searchTerms.keySet().retainAll(menu.rows.keySet());
        String q=search.getValue().toLowerCase(Locale.ROOT);
        AEKey namedItem=null;
        if(queryKey==null&&!q.isEmpty())for(var item:net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            if(item instanceof appeng.api.parts.IPartItem<?>)continue;
            if(item.getDescription().getString().toLowerCase(Locale.ROOT).equals(q)
                    || net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString().equals(q)) {
                namedItem=appeng.api.stacks.AEItemKey.of(item);break;
            }
        }
        activeQuery=queryKey!=null?queryKey:namedItem;
        for(var r:menu.rows.values()) {
            if(!kind.equals("all")&&!kind.equals(r.getString("kind")))continue;
            long id=r.getLong("id");
            if(cachedRows.get(id)!=r) {
                var pos=net.minecraft.core.BlockPos.of(r.getLong("pos"));
                StringBuilder text=new StringBuilder(Component.translatable(r.getString("target")).getString())
                        .append(' ').append(busIcon(r).getHoverName().getString())
                        .append(' ').append(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(busIcon(r).getItem()))
                        .append(' ').append(tr(r.getString("kind")).getString()).append(' ').append(r.getString("kind"))
                        .append(' ').append(r.getString("dimension")).append(' ').append(pos.toShortString())
                        .append(' ').append(BusCoordinates.format(pos));
                for(var raw:r.getList("filters",Tag.TAG_COMPOUND)) {
                    var key=Platform.key((CompoundTag)raw,minecraft.level);
                    if(key!=null)text.append(' ').append(key.getDisplayName().getString()).append(' ').append(key.getId());
                }
                searchTerms.put(id,text.toString().toLowerCase(Locale.ROOT));cachedRows.put(id,r);
            }
            if(namedItem==null&&!searchTerms.get(id).contains(q))continue;
            if(activeQuery!=null && match(r,activeQuery).equals("none"))continue;
            visible.add(r);
        }
        visible.sort(ROW_ORDER);
        page=Math.min(page,Math.max(0,(visible.size()-1)/3));
    }
    private String match(CompoundTag r,AEKey key) {
        return com.syaru.meomnibus.bus.BusSearch.match(r,key,minecraft.level);
    }
    @Override public boolean mouseClicked(double mx,double my,int button) {
        int x=(int)mx-leftPos,y=(int)my-topPos;
        if(x>=8&&x<168&&y>=65&&y<125) {
            int index=page*3+(y-65)/20;
            if(index<visible.size()) {
                long id=visible.get(index).getLong("id");
                if(hasControlDown()) {if(!selected.remove(id)&&selected.size()<128)selected.add(id);}
                else {selected.clear();selected.add(id);}
                focus=id;filterPage=0;rebuildDetails();
            }
            return true;
        }
        if(row()!=null&&x>=198&&x<360&&y>=58&&y<94) {
            int slot=filterPage*18+(y-58)/18*9+(x-198)/18;
            send("filter",t->{t.putInt("slot",slot);t.putBoolean("clear",button==1||menu.getCarried().isEmpty());},false);
            return true;
        }
        return super.mouseClicked(mx,my,button);
    }
    @Override public boolean keyPressed(int key,int scan,int modifiers) {
        if(search.isFocused()||priority.isFocused()) {
            if(key==256)return super.keyPressed(key,scan,modifiers);
            return getFocused()!=null&&getFocused().keyPressed(key,scan,modifiers);
        }
        return super.keyPressed(key,scan,modifiers);
    }
    private void text(GuiGraphics g,Component text,int x,int y,int width,int color) {
        String label=text.getString();
        if(font.width(label)>width)label=font.plainSubstrByWidth(label,Math.max(0,width-font.width("...")))+"...";
        g.drawString(font,label,x,y,color,false);
    }
    private void box(GuiGraphics g,int x,int y,int w,int h,int color) {
        g.fill(leftPos+x,topPos+y,leftPos+x+w,topPos+y+h,0xff7e878b);
        g.fill(leftPos+x+1,topPos+y+1,leftPos+x+w-1,topPos+y+h-1,color);
    }
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my) {
        g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xff42494e);
        g.fill(leftPos+1,topPos+1,leftPos+imageWidth-1,topPos+imageHeight-1,0xffd4d7d9);
        g.fill(leftPos+184,topPos+4,leftPos+185,topPos+228,0xff9da6ab);
        for(var slot:menu.slots)box(g,slot.x-1,slot.y-1,18,18,0xff949da1);
        for(int i=0;i<3;i++) {
            int index=page*3+i,y=65+i*20;box(g,8,y,160,19,0xffbdc5c8);
            if(index<visible.size()) {
                var r=visible.get(index);
                if(selected.contains(r.getLong("id")))g.fill(leftPos+9,topPos+y+1,leftPos+167,topPos+y+18,0xffb8d9cf);
                g.fill(leftPos+11,topPos+y+7,leftPos+14,topPos+y+11,r.getBoolean("online")?0xff368553:0xffa34e3f);
                OmnibusAppearance.renderItem(g,busIcon(r),leftPos+17,topPos+y+1);
                text(g,busIcon(r).getHoverName(),leftPos+36,topPos+y+1,129,INK);
                var pos=net.minecraft.core.BlockPos.of(r.getLong("pos"));
                text(g,activeQuery==null?Component.literal(BusCoordinates.format(pos)):tr(match(r,activeQuery)),leftPos+36,topPos+y+10,129,0x4e626a);
            }
        }
        var r=row();
        if(r!=null) {
            var filters=r.getList("filters",Tag.TAG_COMPOUND);
            for(int i=0;i<18;i++) {
                int slot=filterPage*18+i,x=198+(i%9)*18,y=58+(i/9)*18;
                box(g,x-1,y-1,18,18,slot<r.getInt("enabled")?0xff98b6b7:0xff666d70);
                if(slot<filters.size()) {
                    var key=Platform.key(filters.getCompound(slot),minecraft.level);
                    if(key!=null)g.renderItem(key.wrapForDisplayOrFilter(),leftPos+x,topPos+y);
                }
            }
            int x=288;
            for(var tag:r.getList("upgrades",Tag.TAG_COMPOUND)) {
                var stack=Platform.stack((CompoundTag)tag,minecraft.level);
                box(g,x-1,187,18,18,0xff949da1);
                if(!stack.isEmpty()) {
                    OmnibusAppearance.renderItem(g,stack,leftPos+x,topPos+188);
                    g.renderItemDecorations(font,stack,leftPos+x,topPos+188);
                }
                x+=18;
            }
        }
    }
    @Override protected void renderLabels(GuiGraphics g,int x,int y) {
        text(g,Component.translatable("block.me_omnibus.terminal"),8,9,160,INK);
        text(g,Component.literal((page+1)+"/"+Math.max(1,(visible.size()+2)/3)+"  "+visible.size()+"/"+menu.total),30,133,117,INK);
        var r=row();
        if(r==null){text(g,tr("select"),190,30,195,INK);return;}
        text(g,tr(r.getString("kind")).append(" / ").append(tr(r.getBoolean("online")?"online":"offline")),190,11,139,INK);
        text(g,Component.translatable(r.getString("target")),190,46,200,INK);
        text(g,tr("filters").append(" "+(filterPage+1)+"/"+((r.getInt("enabled")+17)/18)),214,103,154,INK);
        if(priority.visible)text(g,tr("priority"),190,209,39,INK);
        text(g,tr("upgrades"),190,192,94,INK);
        text(g,tr(menu.message.isEmpty()?"selected":menu.message).append(menu.message.isEmpty()?" "+selected.size():""),190,223,200,menu.message.equals("conflict")?0xb43b30:0x3d646b);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial) {
        update();super.render(g,mx,my,partial);renderTooltip(g,mx,my);
        var r=row();int x=mx-leftPos,y=my-topPos;
        if(x>=8&&x<168&&y>=65&&y<125) {
            int index=page*3+(y-65)/20;
            if(index<visible.size()) {
                var hovered=visible.get(index);
                g.renderTooltip(font,List.of(busIcon(hovered).getHoverName(),Component.translatable(hovered.getString("target")),
                    Component.literal(BusCoordinates.format(net.minecraft.core.BlockPos.of(hovered.getLong("pos")))),
                    Component.literal(hovered.getString("dimension"))),Optional.empty(),mx,my);
            }
        }
        if(r!=null&&x>=288&&x<378&&y>=188&&y<204) {
            int slot=(x-288)/18;var list=r.getList("upgrades",Tag.TAG_COMPOUND);
            if(slot<list.size()) {
                var stack=Platform.stack(list.getCompound(slot),minecraft.level);
                if(!stack.isEmpty())g.renderTooltip(font,stack,mx,my);
            }
        }
        if(r!=null&&x>=198&&x<360&&y>=58&&y<94) {
            int slot=filterPage*18+(y-58)/18*9+(x-198)/18;var list=r.getList("filters",Tag.TAG_COMPOUND);
            if(slot<list.size()) {
                var key=Platform.key(list.getCompound(slot),minecraft.level);
                if(key!=null)g.renderTooltip(font,key.getDisplayName(),mx,my);
            }
        }
    }
}
