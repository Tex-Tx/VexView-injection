package cn.pig.runtime;

import java.lang.reflect.Field;

import net.minecraft.client.gui.inventory.GuiContainer;
import org.objectweb.asm.commons.Remapper;
import cn.pig.agent.NativeBridge;
import cn.pig.agent.PigLog;
import cn.pig.asm.ns.MappingKind;
import cn.pig.asm.ns.NamespaceRegistry;

   
                     
  
                                                             
                                                                  
                                                                              
                                                                
               
   
public final class VexGuiStateGuard {

    private static boolean resolved;
    private static Field returningStack;
    private static Field returningStackDestSlot;
    private static Field draggedStack;
    private static Field clickedSlot;
    private static Field theSlot;
    private static boolean logged;

    private VexGuiStateGuard() {
    }

       
                                                                      
                                                    
       
    public static net.minecraft.inventory.Slot slotUnderMouse(Object gui) {
        resolve();
        try {
            return (net.minecraft.inventory.Slot) get(theSlot, gui);
        } catch (Throwable t) {
            return null;
        }
    }

                                        
    public static void clearIfInconsistent(Object gui) {
        if (gui == null) {
            return;
        }
        resolve();
        try {
            Object ret = get(returningStack, gui);
            Object dest = get(returningStackDestSlot, gui);
            if (ret != null && dest == null) {
                set(returningStack, gui, null);
                logOnce("returningStack 缺少 returningStackDestSlot，已清理");
            }
            Object drag = get(draggedStack, gui);
            Object click = get(clickedSlot, gui);
            if (drag != null && click == null) {
                set(draggedStack, gui, null);
                logOnce("draggedStack 缺少 clickedSlot，已清理");
            }
        } catch (Throwable ignored) {
        }
    }

                                                         
    public static void onRenderFailed(Object gui, Throwable t) {
        resolve();
        try {
            set(returningStack, gui, null);
            set(returningStackDestSlot, gui, null);
            set(draggedStack, gui, null);
            set(clickedSlot, gui, null);
        } catch (Throwable ignored) {
        }
        logOnce("GuiContainer.drawScreen 异常已拦截: " + t);
    }

    private static synchronized void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        try {
            MappingKind kind = NativeBridge.bridge() == null ? MappingKind.MCP : NativeBridge.bridge().runtime();
            Remapper remapper = NamespaceRegistry.fromMcp(kind).remapper();
            returningStack = field(remapper, "returningStack", "Lnet/minecraft/item/ItemStack;");
            returningStackDestSlot = field(remapper, "returningStackDestSlot", "Lnet/minecraft/inventory/Slot;");
            draggedStack = field(remapper, "draggedStack", "Lnet/minecraft/item/ItemStack;");
            clickedSlot = field(remapper, "clickedSlot", "Lnet/minecraft/inventory/Slot;");
            theSlot = field(remapper, "theSlot", "Lnet/minecraft/inventory/Slot;");
        } catch (Throwable ignored) {
        }
    }

    private static Field field(Remapper remapper, String name, String desc) {
        try {
            String runtimeName = remapper.mapFieldName(
                    "net/minecraft/client/gui/inventory/GuiContainer", name, desc);
            Field f = GuiContainer.class.getDeclaredField(runtimeName);
            f.setAccessible(true);
            return f;
        } catch (Throwable t) {
            return null;
        }
    }

    private static Object get(Field f, Object target) throws IllegalAccessException {
        return f == null ? null : f.get(target);
    }

    private static void set(Field f, Object target, Object value) throws IllegalAccessException {
        if (f != null) {
            f.set(target, value);
        }
    }

    private static synchronized void logOnce(String msg) {
        if (logged) {
            return;
        }
        logged = true;
        PigLog.log("[VexView] " + msg);
    }
}
