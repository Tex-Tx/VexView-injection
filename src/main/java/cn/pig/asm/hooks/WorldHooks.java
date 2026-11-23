package cn.pig.asm.hooks;

import net.minecraft.client.Minecraft;
import cn.pig.compat.client.event.RenderWorldLastEvent;
import cn.pig.compat.common.MinecraftForge;

   
                                         
   
public final class WorldHooks {

    private WorldHooks() {
    }

    public static void onRenderWorldLast() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            return;
        }
        try {
            MinecraftForge.EVENT_BUS.post(new RenderWorldLastEvent(mc.renderGlobal, 0.0F));
        } catch (Throwable ignored) {
        }
    }
}
