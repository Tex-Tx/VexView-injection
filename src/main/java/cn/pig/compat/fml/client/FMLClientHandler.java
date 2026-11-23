package cn.pig.compat.fml.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.ServerStatusResponse;

   
                                         
                                           
   
public class FMLClientHandler {

    private static final FMLClientHandler INSTANCE = new FMLClientHandler();

    public static FMLClientHandler instance() {
        return INSTANCE;
    }

    public Minecraft getClient() {
        return Minecraft.getMinecraft();
    }

    public void setupServerList() {
    }

    public void connectToServer(GuiScreen parent, ServerData data) {
    }

    public void bindServerListData(ServerData data, ServerStatusResponse response) {
    }
}
