package cn.pig.compat.fml.common.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;

   
                                  
   
public class ByteBufUtils {

    public static ItemStack readItemStack(ByteBuf buf) {
        if (buf == null) {
            return null;
        }
        try {
            return new PacketBuffer(buf).readItemStackFromBuffer();
        } catch (Throwable t) {
            return null;
        }
    }

    public static void writeItemStack(ByteBuf buf, ItemStack stack) {
        if (buf == null) {
            return;
        }
        try {
            new PacketBuffer(buf).writeItemStackToBuffer(stack);
        } catch (Throwable ignored) {
        }
    }

    public static String readUTF8String(ByteBuf buf) {
        if (buf == null) {
            return "";
        }
        try {
            return new PacketBuffer(buf).readStringFromBuffer(32767);
        } catch (Throwable t) {
            return "";
        }
    }

    public static void writeUTF8String(ByteBuf buf, String value) {
        if (buf == null) {
            return;
        }
        try {
            new PacketBuffer(buf).writeString(value == null ? "" : value);
        } catch (Throwable ignored) {
        }
    }
}
