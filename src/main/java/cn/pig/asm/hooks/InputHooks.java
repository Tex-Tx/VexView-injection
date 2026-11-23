package cn.pig.asm.hooks;

import cn.pig.compat.common.MinecraftForge;
import cn.pig.compat.fml.common.gameevent.InputEvent;

   
                                              
   
public final class InputHooks {

    private InputHooks() {
    }

    public static void fireKeyInput() {
        try {
            MinecraftForge.EVENT_BUS.post(new InputEvent.KeyInputEvent());
        } catch (Throwable ignored) {
        }
    }
}
