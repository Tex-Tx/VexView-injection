package cn.pig.compat.fml.common.network;

import cn.pig.compat.fml.common.eventhandler.Event;
import cn.pig.compat.fml.common.network.internal.FMLProxyPacket;

   
                                             
   
public class FMLNetworkEvent<T> extends Event {

    public final T handler;
    public final Object manager;

    FMLNetworkEvent(T handler, Object manager) {
        this.handler = handler;
        this.manager = manager;
    }

    public static class ClientConnectedToServerEvent extends FMLNetworkEvent<Object> {
        public final boolean isLocal;
        public final String connectionType;

        public ClientConnectedToServerEvent(Object manager, String connectionType) {
            super(null, manager);
            this.isLocal = false;
            this.connectionType = connectionType;
        }
    }

    public abstract static class CustomPacketEvent<S> extends FMLNetworkEvent<S> {
        public final FMLProxyPacket packet;
        public FMLProxyPacket reply;

        CustomPacketEvent(S handler, Object manager, FMLProxyPacket packet) {
            super(handler, manager);
            this.packet = packet;
        }
    }

    public static class ClientCustomPacketEvent extends CustomPacketEvent<Object> {
        public ClientCustomPacketEvent(FMLProxyPacket packet) {
            super(null, null, packet);
        }

        public ClientCustomPacketEvent(Object manager, FMLProxyPacket packet) {
            super(null, manager, packet);
        }
    }
}
