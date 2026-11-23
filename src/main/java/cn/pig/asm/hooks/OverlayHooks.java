package cn.pig.asm.hooks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.ScaledResolution;
import cn.pig.asm.CallbackInfo;
import cn.pig.compat.client.event.RenderGameOverlayEvent;
import cn.pig.compat.common.MinecraftForge;

   
                                                  
   
public final class OverlayHooks {

    private OverlayHooks() {
    }

    private static boolean fire(RenderGameOverlayEvent.ElementType type, ScaledResolution res) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            return true;
        }
        try {
            if (res == null) {
                res = new ScaledResolution(mc);
            }
            RenderGameOverlayEvent.Pre event = new RenderGameOverlayEvent.Pre(0.0F, res, type);
            return !MinecraftForge.EVENT_BUS.post(event);
        } catch (Throwable t) {
                                 
            return true;
        }
    }

                                                                     
    public static void onCrosshair() {
        fire(RenderGameOverlayEvent.ElementType.CROSSHAIRS, null);
    }

                                    
    public static void onChat(GuiNewChat gui, CallbackInfo ci) {
        if (!fire(RenderGameOverlayEvent.ElementType.CHAT, null)) {
            ci.cancel();
        }
    }

                                       
    public static void onBossHealth(GuiIngame gui, CallbackInfo ci) {
        if (!fire(RenderGameOverlayEvent.ElementType.BOSSHEALTH, null)) {
            ci.cancel();
        }
    }

                                                                     
    public static void onPlayerStats(GuiIngame gui, ScaledResolution res, CallbackInfo ci) {
        boolean health = fire(RenderGameOverlayEvent.ElementType.HEALTH, res);
        boolean armor = fire(RenderGameOverlayEvent.ElementType.ARMOR, res);
        boolean food = fire(RenderGameOverlayEvent.ElementType.FOOD, res);
        boolean air = fire(RenderGameOverlayEvent.ElementType.AIR, res);
        if (!health || !armor || !food || !air) {
            ci.cancel();
        }
    }

                                   
    public static void onExpBar(GuiIngame gui, ScaledResolution res, int i, CallbackInfo ci) {
        if (!fire(RenderGameOverlayEvent.ElementType.EXPERIENCE, res)) {
            ci.cancel();
        }
    }

                                            
    public static void onHotbar(GuiIngame gui, ScaledResolution res, float partialTicks, CallbackInfo ci) {
        if (!fire(RenderGameOverlayEvent.ElementType.HOTBAR, res)) {
            ci.cancel();
        }
    }
}
