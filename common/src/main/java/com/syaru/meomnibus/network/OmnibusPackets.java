package com.syaru.meomnibus.network;

import com.syaru.meomnibus.menu.OmnibusTerminalMenu;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public record OmnibusPackets(int container,String session,CompoundTag data) {
    public void write(FriendlyByteBuf buf) { buf.writeVarInt(container);buf.writeUtf(session,64);buf.writeNbt(data); }
    public static OmnibusPackets read(FriendlyByteBuf buf) {
        int id=buf.readVarInt();String session=buf.readUtf(64);var tag=buf.readNbt();
        return new OmnibusPackets(id,session,tag==null?new CompoundTag():tag);
    }
    public void handle(ServerPlayer player) {
        if(player!=null && player.containerMenu instanceof OmnibusTerminalMenu menu
                && menu.containerId==container && menu.session.equals(session))menu.command(player,data);
    }
}
