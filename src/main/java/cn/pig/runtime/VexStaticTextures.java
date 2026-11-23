package cn.pig.runtime;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import cn.pig.agent.NativeBridge;
import cn.pig.agent.PigLog;

   
                                              
  
                                   
                                                                                   
                                                                        
                                               
   
public final class VexStaticTextures {

    private static final String PREFIX = "assets/vexview/";

                                                               
    private static final String[] FALLBACK = {
            "textures/backpack_button.png",
            "textures/bar.png",
            "textures/bq_button.png",
            "textures/bq_button_.png",
            "textures/broad.png",
            "textures/connect.png",
            "textures/download.png",
            "textures/error.png",
            "textures/feiled.png",
            "textures/guide.png",
            "textures/guide_.png",
            "textures/itemshow.png",
            "textures/loading.png",
            "textures/login_button.png",
            "textures/normal.png",
            "textures/sb_load.png",
            "textures/sidebar.png",
            "textures/sidebar_.png",
            "textures/store.png",
            "textures/store_.png",
            "textures/update.png"
    };

    private VexStaticTextures() {
    }

    public static void install() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            return;
        }
        TextureManager manager = mc.getTextureManager();
        if (manager == null) {
            return;
        }
        List<String> paths = listTexturePaths();
        int ok = 0;
        for (String path : paths) {
            try {
                ResourceLocation location = new ResourceLocation("vexview", path);
                if (manager.loadTexture(location, new ClasspathTexture(location))) {
                    ok++;
                }
            } catch (Throwable ignored) {
            }
        }
        PigLog.log("[VexView] 静态贴图注册完成: " + ok + "/" + paths.size());
    }

                                                                   
    private static List<String> listTexturePaths() {
        List<String> out = new ArrayList<String>();
        String jar = NativeBridge.selfJarPath();
        if (jar != null) {
            try {
                JarFile file = new JarFile(jar);
                try {
                    Enumeration<JarEntry> entries = file.entries();
                    while (entries.hasMoreElements()) {
                        String name = entries.nextElement().getName();
                        if (name.startsWith(PREFIX) && name.endsWith(".png")) {
                            out.add(name.substring(PREFIX.length()));
                        }
                    }
                } finally {
                    file.close();
                }
            } catch (Throwable ignored) {
            }
        }
        if (out.isEmpty()) {
            for (String path : FALLBACK) {
                out.add(path);
            }
        }
        return out;
    }

                                                          
    private static InputStream open(String relative) {
        String resource = "/" + PREFIX + relative;
        InputStream in = VexStaticTextures.class.getResourceAsStream(resource);
        if (in != null) {
            return in;
        }
        String jar = NativeBridge.selfJarPath();
        if (jar == null) {
            return null;
        }
        try {
            JarFile file = new JarFile(jar);
            JarEntry entry = file.getJarEntry(PREFIX + relative);
            if (entry == null) {
                file.close();
                return null;
            }
                                       
            final InputStream delegate = file.getInputStream(entry);
            final JarFile owner = file;
            return new java.io.FilterInputStream(delegate) {
                @Override
                public void close() throws java.io.IOException {
                    try {
                        super.close();
                    } finally {
                        owner.close();
                    }
                }
            };
        } catch (Throwable t) {
            return null;
        }
    }

                                         
    private static final class ClasspathTexture extends AbstractTexture {
        private final ResourceLocation location;

        ClasspathTexture(ResourceLocation location) {
            this.location = location;
        }

        @Override
        public void loadTexture(IResourceManager resourceManager) {
            BufferedImage image = null;
            InputStream in = open(this.location.getResourcePath());
            if (in != null) {
                try {
                    image = ImageIO.read(in);
                } catch (Throwable ignored) {
                } finally {
                    try {
                        in.close();
                    } catch (Throwable ignored) {
                    }
                }
            }
            if (image == null) {
                                               
                image = missing();
            }
            TextureUtil.uploadTextureImage(this.getGlTextureId(), image);
        }

        private static BufferedImage missing() {
            BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    boolean odd = ((x >> 3) + (y >> 3)) % 2 == 0;
                    image.setRGB(x, y, odd ? 0xFFFF00FF : 0xFF000000);
                }
            }
            return image;
        }
    }
}
