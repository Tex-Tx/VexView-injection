package cn.pig.asm;

import cn.pig.asm.transform.TransformEntityRenderer;
import cn.pig.asm.transform.TransformGuiContainer;
import cn.pig.asm.transform.TransformGuiIngame;
import cn.pig.asm.transform.TransformGuiNewChat;
import cn.pig.asm.transform.TransformGuiScreen;
import cn.pig.asm.transform.TransformMinecraft;
import cn.pig.asm.transform.TransformNetHandlerPlayClient;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

   
                                 
   
public final class TransformDispatcher {

    public interface ClassTransform {
        byte[] apply(byte[] classBytes);
    }

    private static final Map<String, ClassTransform> TARGETS;

    static {
        Map<String, ClassTransform> m = new LinkedHashMap<String, ClassTransform>();
        m.put("net.minecraft.client.gui.GuiIngame", new ClassTransform() {
            @Override public byte[] apply(byte[] b) { return TransformGuiIngame.transform(b); }
        });
        m.put("net.minecraft.client.gui.GuiNewChat", new ClassTransform() {
            @Override public byte[] apply(byte[] b) { return TransformGuiNewChat.transform(b); }
        });
        m.put("net.minecraft.client.gui.GuiScreen", new ClassTransform() {
            @Override public byte[] apply(byte[] b) { return TransformGuiScreen.transform(b); }
        });
        m.put("net.minecraft.client.gui.inventory.GuiContainer", new ClassTransform() {
            @Override public byte[] apply(byte[] b) { return TransformGuiContainer.transform(b); }
        });
        m.put("net.minecraft.client.Minecraft", new ClassTransform() {
            @Override public byte[] apply(byte[] b) { return TransformMinecraft.transform(b); }
        });
        m.put("net.minecraft.client.network.NetHandlerPlayClient", new ClassTransform() {
            @Override public byte[] apply(byte[] b) { return TransformNetHandlerPlayClient.transform(b); }
        });
        m.put("net.minecraft.client.renderer.EntityRenderer", new ClassTransform() {
            @Override public byte[] apply(byte[] b) { return TransformEntityRenderer.transform(b); }
        });
        TARGETS = Collections.unmodifiableMap(m);
    }

    private TransformDispatcher() {
    }

    public static boolean isTarget(String mcpName) {
        return mcpName != null && TARGETS.containsKey(mcpName);
    }

    public static byte[] dispatch(String mcpName, byte[] classBytes) {
        ClassTransform t = TARGETS.get(mcpName);
        return t == null ? null : t.apply(classBytes);
    }
}
