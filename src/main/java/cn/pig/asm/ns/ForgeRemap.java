package cn.pig.asm.ns;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.RemappingClassAdapter;

   
                                    
      
                                                             
                                      
      
                                                                  
                                
       
                                                                         
                                                                  
        
                        
   
public final class ForgeRemap {

    private static final String FROM = "net/minecraftforge/";
    private static final String LEGACY_TO = "cn/pig/compat/";
    private static final String FOLDED_TO = "cmp/";
                                                   
    private static final String MARKER = "/cmp/fml/common/event/FMLInitializationEvent.class";

    private static volatile String toPrefix;

    private static String toPrefix() {
        String p = toPrefix;
        if (p == null) {
            p = ForgeRemap.class.getResource(MARKER) != null ? FOLDED_TO : LEGACY_TO;
            toPrefix = p;
        }
        return p;
    }

                                                                      
    public static String compatInternal(String rel) {
        return toPrefix() + rel;
    }

                                 
    public static boolean isCompat(String internalName) {
        return internalName != null && internalName.startsWith(toPrefix());
    }

    private static final Remapper REMAPPER = new Remapper() {
        @Override
        public String map(String internalName) {
            if (internalName != null && internalName.startsWith(FROM)) {
                return toPrefix() + internalName.substring(FROM.length());
            }
            return internalName;
        }
    };

    private ForgeRemap() {
    }

    public static byte[] apply(byte[] classBytes) {
        if (classBytes == null) {
            return null;
        }
        ClassReader reader = new ClassReader(classBytes);
        ClassWriter writer = new ClassWriter(0);
        reader.accept(new Adapter(writer, REMAPPER), ClassReader.EXPAND_FRAMES);
        return writer.toByteArray();
    }

                                                         
    private static final class Adapter extends RemappingClassAdapter {
        Adapter(ClassVisitor cv, Remapper remapper) {
            super(cv, remapper);
        }
    }
}
