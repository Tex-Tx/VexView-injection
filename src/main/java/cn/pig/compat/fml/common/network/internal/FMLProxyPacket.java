package cn.pig.compat.fml.common.network.internal;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.C17PacketCustomPayload;

   
                                         
                                                                              
   
public class FMLProxyPacket {

    private final String channel;
    private final PacketBuffer payload;

    public FMLProxyPacket(PacketBuffer payload, String channel) {
        this.channel = channel == null ? "" : channel;
        this.payload = payload;
    }

    public FMLProxyPacket(ByteBuf payload, String channel) {
        this(new PacketBuffer(payload), channel);
    }

    public String channel() {
        return this.channel;
    }

    public ByteBuf payload() {
        return this.payload;
    }

                                                         
    public C17PacketCustomPayload toC17Packet() {
        return new C17PacketCustomPayload(this.channel, this.payload);
    }

    public FMLProxyPacket copy() {
        byte[] data = new byte[this.payload.readableBytes()];
        this.payload.getBytes(this.payload.readerIndex(), data);
        return new FMLProxyPacket(io.netty.buffer.Unpooled.wrappedBuffer(data), this.channel);
    }
}
