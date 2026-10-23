package com.syaru.meomnibus.platform;
import com.syaru.meomnibus.network.OmnibusPackets;
import com.syaru.meomnibus.client.ClientPackets;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.*;
public final class Network {
    private static final net.minecraftforge.network.simple.SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(Platform.id("main"),()->"1","1"::equals,"1"::equals);
    public static void register() {
        CHANNEL.messageBuilder(OmnibusPackets.class,0,NetworkDirection.PLAY_TO_SERVER).encoder(OmnibusPackets::write).decoder(OmnibusPackets::read)
            .consumerMainThread((p,c)->p.handle(c.get().getSender())).add();
        CHANNEL.messageBuilder(Response.class,1,NetworkDirection.PLAY_TO_CLIENT).encoder((p,b)->p.packet.write(b)).decoder(b->new Response(OmnibusPackets.read(b)))
            .consumerMainThread((p,c)->ClientPackets.handle(p.packet)).add();
    }
    public record Response(OmnibusPackets packet) {}
    public static void send(ServerPlayer player,OmnibusPackets packet) { CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new Response(packet)); }
    public static void control(OmnibusPackets packet) { CHANNEL.sendToServer(packet); }
}
