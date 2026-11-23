package cn.pig.compat.fml.common.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import cn.pig.compat.fml.common.eventhandler.EventBus;
import cn.pig.compat.fml.common.network.internal.FMLProxyPacket;

   
                                                         
   
public class FMLEventChannel {

    private final String name;
    private final EventBus eventBus = new EventBus();

    FMLEventChannel(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    public void register(Object object) {
        this.eventBus.register(object);
    }

    public void unregister(Object object) {
        this.eventBus.unregister(object);
    }

                         
    public void sendToServer(FMLProxyPacket pkt) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            return;
        }
        NetHandlerPlayClient handler = mc.getNetHandler();
        if (handler == null) {
            return;
        }
        handler.addToSendQueue(pkt.toC17Packet());
    }

                                                                          
    public void fireRead(FMLProxyPacket pkt) {
        this.eventBus.post(new FMLNetworkEvent.ClientCustomPacketEvent(pkt));
    }
}
