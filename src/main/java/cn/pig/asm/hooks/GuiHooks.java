package cn.pig.asm.hooks;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import cn.pig.asm.CallbackInfo;
import cn.pig.compat.client.event.GuiOpenEvent;
import cn.pig.compat.client.event.GuiScreenEvent;
import cn.pig.compat.common.MinecraftForge;
import cn.pig.runtime.VexGuiStateGuard;

   
                                                                 
   
public final class GuiHooks {

    private GuiHooks() {
    }

                                                            
    public static void onContainerDraw(GuiContainer gui) {
        try {
            if (gui instanceof lk.vexview.gui.VexCustomGui) {
                VexGuiStateGuard.clearIfInconsistent(gui);
            }
        } catch (Throwable ignored) {
        }
    }

                                                                               
    public static void onContainerDrawThrown(GuiContainer gui, Throwable t) throws Throwable {
        if (!(gui instanceof lk.vexview.gui.VexCustomGui)) {
            throw t;
        }
        VexGuiStateGuard.onRenderFailed(gui, t);
    }

                                                      
    public static GuiScreen onDisplayGuiScreen(GuiScreen gui) {
        try {
            GuiOpenEvent event = new GuiOpenEvent(gui);
            if (MinecraftForge.EVENT_BUS.post(event)) {
                return null;
            }
            return event.gui;
        } catch (Throwable t) {
            return gui;
        }
    }

    public static void onDrawScreenPre(GuiScreen gui, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        try {
            GuiScreenEvent.DrawScreenEvent.Pre event =
                    new GuiScreenEvent.DrawScreenEvent.Pre(gui, mouseX, mouseY, partialTicks);
            if (MinecraftForge.EVENT_BUS.post(event)) {
                ci.cancel();
            }
        } catch (Throwable ignored) {
        }
    }

    public static void onDrawScreenPost(GuiScreen gui, int mouseX, int mouseY, float partialTicks) {
        try {
            MinecraftForge.EVENT_BUS.post(new GuiScreenEvent.DrawScreenEvent.Post(gui, mouseX, mouseY, partialTicks));
        } catch (Throwable ignored) {
        }
    }
}
