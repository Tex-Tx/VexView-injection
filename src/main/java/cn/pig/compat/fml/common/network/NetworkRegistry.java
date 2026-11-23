package cn.pig.compat.fml.common.network;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

   
                                         
   
public final class NetworkRegistry {

    public static final NetworkRegistry INSTANCE = new NetworkRegistry();

    private final Map<String, FMLEventChannel> channels = new ConcurrentHashMap<String, FMLEventChannel>();

    private NetworkRegistry() {
    }

    public FMLEventChannel newEventDrivenChannel(String name) {
        FMLEventChannel channel = this.channels.get(name);
        if (channel == null) {
            channel = new FMLEventChannel(name);
            this.channels.put(name, channel);
        }
        return channel;
    }

                                                       
    public static FMLEventChannel channel(String name) {
        return name == null ? null : INSTANCE.channels.get(name);
    }
}
