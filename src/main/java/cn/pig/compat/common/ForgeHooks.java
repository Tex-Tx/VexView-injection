package cn.pig.compat.common;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

   
                                 
   
public final class ForgeHooks {
    private ForgeHooks() {
    }

    public static void onLivingJump(EntityLivingBase entity) {
    }

                                                                    
    public static IChatComponent newChatWithLinks(String string, boolean allowRedirect) {
        return new ChatComponentText(string == null ? "" : string);
    }

    public static IChatComponent newChatWithLinks(String string) {
        return newChatWithLinks(string, false);
    }
}
