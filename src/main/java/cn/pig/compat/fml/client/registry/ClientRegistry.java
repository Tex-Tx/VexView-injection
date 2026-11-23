package cn.pig.compat.fml.client.registry;

import java.util.Arrays;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;

   
                                                              
   
public class ClientRegistry {

    public static void registerKeyBinding(KeyBinding keyBinding) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.gameSettings == null || keyBinding == null) {
            return;
        }
        KeyBinding[] old = mc.gameSettings.keyBindings;
        if (old == null) {
            mc.gameSettings.keyBindings = new KeyBinding[]{keyBinding};
            return;
        }
        for (KeyBinding k : old) {
            if (k == keyBinding) {
                return;
            }
        }
        KeyBinding[] added = Arrays.copyOf(old, old.length + 1);
        added[old.length] = keyBinding;
        mc.gameSettings.keyBindings = added;
    }
}
