package com.syaru.meomnibus.platform;

import appeng.api.stacks.AEKey;
import com.syaru.meomnibus.menu.OmnibusTerminalMenu;
import com.syaru.meomnibus.block.BaseTerminal;
import com.syaru.meomnibus.network.OmnibusPackets;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.network.chat.Component;

public final class Platform {
    public static final String ID="me_omnibus";
    public static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(ID,path); }
    public static net.minecraft.world.inventory.MenuType<OmnibusTerminalMenu> menu() { return OmnibusMod.MENU.get(); }
    public static CompoundTag keyTag(AEKey key,Level level) { return key.toTagGeneric(level.registryAccess()); }
    public static AEKey key(CompoundTag tag,Level level) { return tag.isEmpty()?null:AEKey.fromTagGeneric(level.registryAccess(),tag); }
    public static CompoundTag stackTag(ItemStack stack,Level level) {
        return stack.isEmpty()?new CompoundTag():(CompoundTag)stack.save(level.registryAccess());
    }
    public static ItemStack stack(CompoundTag tag,Level level) { return tag.isEmpty()?ItemStack.EMPTY:ItemStack.parseOptional(level.registryAccess(),tag); }
    public static void open(ServerPlayer player,com.syaru.meomnibus.menu.TerminalAccess terminal) {
        if(!terminal.canAccess(player)) { player.displayClientMessage(Component.translatable("omnibus.no_access"),true);return; }
        String session=java.util.UUID.randomUUID().toString();
        var provider=new SimpleMenuProvider((id,inv,p)->new OmnibusTerminalMenu(id,inv,terminal,session),Component.translatable("block.me_omnibus.terminal"));
        player.openMenu(provider,buf->buf.writeUtf(session,64));
    }
    public static void send(ServerPlayer player,int id,String session,CompoundTag data) { Network.send(player,new OmnibusPackets(id,session,data)); }
    public static void control(OmnibusPackets packet) { Network.control(packet); }
}
