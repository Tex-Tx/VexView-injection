package cn.pig.compat.fml.client.config;

import java.util.List;

import net.minecraft.client.gui.FontRenderer;

   
                                                    
   
public class GuiUtils {

    public static void drawHoveringText(List<String> textLines, int x, int y, int width, int height,
                                        int maxTextWidth, FontRenderer font) {
        if (textLines == null || textLines.isEmpty() || font == null) {
            return;
        }
        int line = 0;
        for (String s : textLines) {
            if (s == null) {
                continue;
            }
            font.drawStringWithShadow(s, (float) x, (float) (y + line), 0xFFFFFF);
            line += font.FONT_HEIGHT + 2;
        }
    }
}
