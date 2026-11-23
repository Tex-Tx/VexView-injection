package cn.pig.asm.ns;

import cn.pig.asm.SafeClassWriter;
import cn.pig.asm.TransformDispatcher;
import org.objectweb.asm.ClassReader;

   
                                             
  
                                                            
                                                            
                                                    
   
public final class NamespaceBridge {

    private final MappingKind runtime;

    public NamespaceBridge(MappingKind runtime) {
        this.runtime = runtime == null ? MappingKind.MCP : runtime;
    }

    public MappingKind runtime() {
        return runtime;
    }

    private boolean normalizeClassNames() {
        return runtime == MappingKind.NOTCH;
    }

    private String toMcpName(String incomingName) {
        if (incomingName == null) {
            return null;
        }
        if (normalizeClassNames()) {
            return dotted(NamespaceRegistry.toMcp(runtime).mapClassName(incomingName));
        }
        return dotted(incomingName);
    }

                                     
    public boolean isGameTarget(String incomingName) {
        if (incomingName == null) {
            return false;
        }
        return TransformDispatcher.isTarget(toMcpName(incomingName));
    }

    public String displayName(String incomingName) {
        return toMcpName(incomingName);
    }

                                                             
    public byte[] transformGameClass(String incomingName, byte[] classBytes) {
        String mcpName = toMcpName(incomingName);
        if (!TransformDispatcher.isTarget(mcpName)) {
            return null;
        }
        MappingKind prev = SafeClassWriter.RUNTIME_KIND.get();
        SafeClassWriter.RUNTIME_KIND.set(runtime);
        try {
            byte[] mcpBytes = normalizeClassNames()
                    ? NamespaceRegistry.toMcp(runtime).applyExact(classBytes)
                    : classBytes;
            byte[] transformed = TransformDispatcher.dispatch(mcpName, mcpBytes);
            if (transformed == null) {
                return null;
            }
            byte[] fixed = FrameFix.apply(transformed);
            if (fixed == null) {
                                                    
                return null;
            }
            transformed = fixed;
            byte[] result = normalizeClassNames()
                    ? NamespaceRegistry.fromMcp(runtime).applyExact(transformed)
                    : transformed;
            return SchemaGuard.apply(mcpName, classBytes, result);
        } catch (Throwable t) {
            return null;
        } finally {
            if (prev == null) {
                SafeClassWriter.RUNTIME_KIND.remove();
            } else {
                SafeClassWriter.RUNTIME_KIND.set(prev);
            }
        }
    }

       
                                                   
                                             
       
    public byte[] transformOwnClass(byte[] classBytes) {
        try {
            byte[] compat = ForgeRemap.apply(VexViewFix.apply(classBytes));
            if (runtime == MappingKind.MCP) {
                return compat;
            }
            return NamespaceRegistry.fromMcp(runtime).applyOwn(compat);
        } catch (Throwable t) {
            return classBytes;
        }
    }

    private static String dotted(String internal) {
        return internal == null ? null : internal.replace('/', '.');
    }
}
