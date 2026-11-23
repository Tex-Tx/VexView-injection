package cn.pig.asm.hooks;

import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.S3FPacketCustomPayload;
import cn.pig.compat.common.MinecraftForge;
import cn.pig.compat.fml.common.network.FMLEventChannel;
import cn.pig.compat.fml.common.network.FMLNetworkEvent;
import cn.pig.compat.fml.common.network.NetworkRegistry;
import cn.pig.compat.fml.common.network.internal.FMLProxyPacket;

   
                                                      
   
public final class NetHooks {

    private NetHooks() {
    }

       
                                                       
       
    public static boolean onCustomPayload(Object netHandler, S3FPacketCustomPayload packet) {
        if (packet == null) {
            return false;
        }
        try {
            String channel = packet.getChannelName();
            FMLEventChannel eventChannel = NetworkRegistry.channel(channel);
            if (eventChannel == null) {
                return false;
            }
            PacketBuffer buffer = packet.getBufferData();
            int readable = buffer.readableBytes();
            byte[] data = new byte[readable];
            buffer.getBytes(buffer.readerIndex(), data);
            FMLProxyPacket proxy = new FMLProxyPacket(Unpooled.wrappedBuffer(data), channel);
            eventChannel.fireRead(proxy);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

                                               
    public static void onJoinGame() {
        try {
            MinecraftForge.EVENT_BUS.post(
                    new FMLNetworkEvent.ClientConnectedToServerEvent(null, "MOD"));
        } catch (Throwable ignored) {
        }
    }
}
