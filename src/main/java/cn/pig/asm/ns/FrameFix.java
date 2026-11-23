package cn.pig.asm.ns;

import cn.pig.asm.SafeClassWriter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

   
                        
  
                                              
                                                           
                                           
                                                   
  
                                              
                                               
                                       
   
public final class FrameFix {

    private FrameFix() {
    }

                                                           
    public static byte[] apply(byte[] classBytes) {
        try {
            ClassReader reader = new ClassReader(classBytes);
            ClassNode node = new ClassNode();
            reader.accept(node, ClassReader.SKIP_FRAMES);
            ClassWriter writer = new SafeClassWriter(reader, ClassWriter.COMPUTE_FRAMES);
            node.accept(writer);
            return writer.toByteArray();
        } catch (Throwable t) {
            return null;
        }
    }
}
