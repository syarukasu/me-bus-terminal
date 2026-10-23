package com.syaru.meomnibus.platform;
import com.syaru.meomnibus.network.OmnibusPackets;
import com.syaru.meomnibus.client.ClientPackets;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
public final class Network {
    public record Request(OmnibusPackets packet) implements CustomPacketPayload {
        static final Type<Request> TYPE=new Type<>(Platform.id("request"));
        static final StreamCodec<RegistryFriendlyByteBuf,Request> CODEC=StreamCodec.of((b,p)->p.packet.write(b),b->new Request(OmnibusPackets.read(b)));
        @Override public Type<Request> type() { return TYPE; }
    }
    public record Response(OmnibusPackets packet) implements CustomPacketPayload {
        static final Type<Response> TYPE=new Type<>(Platform.id("response"));
        static final StreamCodec<RegistryFriendlyByteBuf,Response> CODEC=StreamCodec.of((b,p)->p.packet.write(b),b->new Response(OmnibusPackets.read(b)));
        @Override public Type<Response> type() { return TYPE; }
    }
    public static void register(RegisterPayloadHandlersEvent event) {
        var r=event.registrar("1");
        r.playToServer(Request.TYPE,Request.CODEC,(p,c)->c.enqueueWork(()->p.packet.handle((ServerPlayer)c.player())));
        r.playToClient(Response.TYPE,Response.CODEC,(p,c)->c.enqueueWork(()->ClientPackets.handle(p.packet)));
    }
    public static void send(ServerPlayer player,OmnibusPackets packet) { PacketDistributor.sendToPlayer(player,new Response(packet)); }
    public static void control(OmnibusPackets packet) { PacketDistributor.sendToServer(new Request(packet)); }
}
