package cn.pig.compat.fml.common;

import cn.pig.compat.common.MinecraftForge;
import cn.pig.compat.fml.common.eventhandler.EventBus;
import cn.pig.compat.fml.common.gameevent.InputEvent;

   
                                                                         
   
public class FMLCommonHandler {

    private static final FMLCommonHandler INSTANCE = new FMLCommonHandler();

    public static FMLCommonHandler instance() {
        return INSTANCE;
    }

    public EventBus bus() {
        return MinecraftForge.EVENT_BUS;
    }

    public void fireKeyInput() {
        MinecraftForge.EVENT_BUS.post(new InputEvent.KeyInputEvent());
    }
}
