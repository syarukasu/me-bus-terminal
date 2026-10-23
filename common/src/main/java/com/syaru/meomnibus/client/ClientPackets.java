package com.syaru.meomnibus.client;

import com.syaru.meomnibus.menu.OmnibusTerminalMenu;
import com.syaru.meomnibus.network.OmnibusPackets;
import net.minecraft.client.Minecraft;

public final class ClientPackets {
    public static void handle(OmnibusPackets packet) {
        var player=Minecraft.getInstance().player;
        if(player!=null && player.containerMenu instanceof OmnibusTerminalMenu menu
                && menu.containerId==packet.container() && menu.session.equals(packet.session())) menu.receive(packet.data());
    }
}
