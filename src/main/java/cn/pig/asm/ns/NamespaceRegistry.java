package cn.pig.asm.ns;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.RemappingClassAdapter;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Collections;

   
                
  
                                                                           
                                                                               
  
                                                                          
        
                                          
                                                                          
                                                                                                          
         
   
public final class NamespaceRegistry {

    private static final Map<String, NamespaceRegistry> CACHE = new HashMap<String, NamespaceRegistry>();

    private final Map<String, String> cl = new HashMap<String, String>();
    private final Map<String, String> clInv = new HashMap<String, String>();
    private final Map<String, String> fd = new HashMap<String, String>();
    private final Map<String, String> md = new HashMap<String, String>();
    private final Set<String> ambig = new HashSet<String>();
    private final Map<String, String> hierarchyCache = new HashMap<String, String>();
    private final Map<String, java.util.List<String>> classParents =
            new HashMap<String, java.util.List<String>>();
       
                                                  
                                                                       
                          
       
    private static final Map<String, java.util.List<String>> STATIC_HIERARCHY =
            new HashMap<String, java.util.List<String>>();
                                                         
    private static final String[] FOLD_ROOTS = {
            "kqa/", "zmv/", "xtr/", "qjf/", "wbn/", "vhx/", "lzp/", "rkd/", "cmp/",
    };
    private static boolean staticHierarchyLoaded;
    private final boolean forward;
    private final String label;

    private NamespaceRegistry(MappingKind kind, boolean forward) {
        this.forward = forward;
        this.label = kind.name() + (forward ? ":F" : ":R");
        load(kind.resource(), forward);
                                                  
        for (Map.Entry<String, String> e : cl.entrySet()) {
            clInv.put(e.getValue(), e.getKey());
        }
    }

    public static synchronized NamespaceRegistry get(MappingKind kind, boolean forward) {
        String key = kind.name() + (forward ? ":F" : ":R");
        NamespaceRegistry r = CACHE.get(key);
        if (r == null) {
            r = new NamespaceRegistry(kind, forward);
            CACHE.put(key, r);
        }
        return r;
    }

                              
    public static NamespaceRegistry toMcp(MappingKind kind) {
        return get(kind, false);
    }

                               
    public static NamespaceRegistry fromMcp(MappingKind kind) {
        return get(kind, true);
    }

    public String label() {
        return label;
    }

       
                                                  
                                                         
                                     
       
    public static boolean isOwnClass(String internalName) {
        if (internalName == null) {
            return false;
        }
        if (internalName.startsWith("cn/pig/")) {
            return true;
        }
                                                         
        if (internalName.startsWith("lk/vexview/")) {
            return true;
        }
        for (String root : FOLD_ROOTS) {
            if (internalName.startsWith(root)) {
                return true;
            }
        }
        return false;
    }

    public String mapClassName(String internalName) {
        if (internalName == null) {
            return null;
        }
        String r = cl.get(internalName);
        return r != null ? r : internalName;
    }

    public String mapDesc(String desc) {
        if (desc == null || desc.indexOf('L') < 0) {
            return desc;
        }
        StringBuilder sb = new StringBuilder(desc.length() + 16);
        int i = 0;
        int n = desc.length();
        while (i < n) {
            char c = desc.charAt(i);
            if (c == 'L') {
                int j = desc.indexOf(';', i);
                if (j < 0) {
                    sb.append(desc, i, n);
                    break;
                }
                sb.append('L').append(mapClassName(desc.substring(i + 1, j))).append(';');
                i = j + 1;
            } else if (c == '[') {
                int k = i;
                while (k < n && desc.charAt(k) == '[') {
                    k++;
                }
                sb.append(desc, i, k);
                i = k;
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    public Remapper remapper() {
        return remapper(false, Collections.<String>emptyList(), null, false, false, null, null);
    }

       
                                          
                                                
       
    public Remapper ownRemapper() {
        return remapper(true, Collections.<String>emptyList(), null, false, false, null, null);
    }

    private Remapper ownRemapper(String self, java.util.List<String> parents, boolean mcParent,
                                 boolean srgCompiled, Set<String> selfMethods,
                                 Set<String> selfFields) {
        return remapper(true, parents, self, mcParent, srgCompiled, selfMethods, selfFields);
    }

       
                                                                       
                                                                        
                                                                                   
                                                                   
                                                                
                                                                          
                                                                 
                                                                 
       
    private Remapper remapper(final boolean allowSimpleForAnyOwner, final java.util.List<String> parents,
                              final String self, final boolean mcParent, final boolean srgCompiled,
                              final Set<String> selfMethods, final Set<String> selfFields) {
        return new Remapper() {
            @Override
            public String map(String type) {
                return mapClassName(type);
            }

            @Override
            public String mapFieldName(String owner, String name, String desc) {
                String r = fd.get(owner + "/" + name);
                if (r == null && owner != null && owner.equals(self)) {
                    for (String parent : parents) {
                        r = fd.get(parent + "/" + name);
                        if (r != null) {
                            break;
                        }
                    }
                }
                if (r == null && mapped(owner) && !ambig.contains("F:" + name)) {
                    r = fd.get(name);
                }
                                                                         
                if (r == null && allowSimpleForAnyOwner && owner != null
                        && !ForgeRemap.isCompat(owner) && isSrgName(name)) {
                    r = fd.get(name);
                }
                if (r == null) {
                    r = mapByHierarchy(owner, name, null, false, self, parents);
                }
                if (r == null) {
                    r = staticHierarchyLookup(owner, name, null, false);
                }
                if (r == null) {
                    r = mapByOwnHierarchy(owner, name, null, false);
                }
                                                     
                if (r != null && selfFields != null && owner != null && owner.equals(self)
                        && !r.equals(name) && selfFields.contains(r + ":" + desc)) {
                    r = null;
                }
                return r != null ? r : name;
            }

            @Override
            public String mapMethodName(String owner, String name, String desc) {
                                                                      
                                                                    
                                                                
                                                               
                boolean simple = mapped(owner)
                        || (allowSimpleForAnyOwner && owner != null
                        && !ForgeRemap.isCompat(owner)
                        && (isSrgName(name) || (mcParent && !srgCompiled && owner.equals(self))));
                String r = md.get(owner + "/" + name + " " + desc);
                if (r == null && owner != null && owner.equals(self)) {
                                                     
                    for (String parent : parents) {
                        r = md.get(parent + "/" + name + " " + desc);
                        if (r != null) {
                            break;
                        }
                    }
                }
                if (r == null && simple && !ambig.contains("M:" + name + " " + desc)) {
                    r = md.get(name + " " + desc);
                }
                if (r == null && mapped(owner) && !ambig.contains("M:" + name)) {
                    r = md.get(name);
                }
                if (r == null) {
                    r = mapByHierarchy(owner, name, desc, true, self, parents);
                }
                if (r == null) {
                    r = staticHierarchyLookup(owner, name, desc, true);
                }
                if (r == null) {
                    r = mapByOwnHierarchy(owner, name, desc, true);
                }
                                                     
                if (r != null && selfMethods != null && owner != null && owner.equals(self)
                        && !r.equals(name) && selfMethods.contains(r + desc)) {
                    r = null;
                }
                return r != null ? r : name;
            }
        };
    }

       
                                                                   
                                                           
          
                                             
      
                                                  
       
    private String mapByHierarchy(String owner, String name, String desc, boolean method,
                                  String self, java.util.List<String> parents) {
        if (owner == null) {
            return null;
        }
        String cacheKey = (method ? "M:" : "F:") + owner + "/" + name + " " + desc;
        if (hierarchyCache.containsKey(cacheKey)) {
            return hierarchyCache.get(cacheKey);
        }
        String result = walkHierarchy(owner, name, desc, method);
        if (result == null && self != null && self.equals(owner)) {
            for (String parent : parents) {
                result = walkHierarchy(parent, name, desc, method);
                if (result != null) {
                    break;
                }
            }
        }
        hierarchyCache.put(cacheKey, result);
        return result;
    }

       
                                                          
                                                               
       
    private String staticHierarchyLookup(String owner, String name, String desc, boolean method) {
        if (owner == null || isOwnClass(owner) || owner.startsWith("java/")) {
            return null;
        }
        ensureStaticHierarchy();
        if (STATIC_HIERARCHY.isEmpty()) {
            return null;
        }
        String cacheKey = (method ? "H:M:" : "H:F:") + owner + "/" + name + " " + desc;
        if (hierarchyCache.containsKey(cacheKey)) {
            return hierarchyCache.get(cacheKey);
        }
                                             
        String start = forward ? owner : mapClassName(owner);
        String result = null;
        if (start != null) {
            java.util.ArrayDeque<String> queue = new java.util.ArrayDeque<String>();
            java.util.HashSet<String> seen = new java.util.HashSet<String>();
            queue.add(start);
            while (!queue.isEmpty() && result == null) {
                String cur = queue.poll();
                if (!seen.add(cur)) {
                    continue;
                }
                String keyOwner = forward ? cur : clInv.get(cur);
                if (keyOwner != null) {
                    String hit = method
                            ? md.get(keyOwner + "/" + name + " " + desc)
                            : fd.get(keyOwner + "/" + name);
                    if (hit != null) {
                        result = hit;
                        break;
                    }
                }
                java.util.List<String> ps = STATIC_HIERARCHY.get(cur);
                if (ps != null) {
                    queue.addAll(ps);
                }
            }
        }
        hierarchyCache.put(cacheKey, result);
        return result;
    }

    private static synchronized void ensureStaticHierarchy() {
        if (staticHierarchyLoaded) {
            return;
        }
        staticHierarchyLoaded = true;
        InputStream in = NamespaceRegistry.class.getResourceAsStream("/mappings/mcp-hierarchy.txt");
        if (in == null) {
            return;
        }
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                String[] p = line.split("\\s+");
                if (p.length < 2) {
                    continue;
                }
                java.util.List<String> parents = new java.util.ArrayList<String>(p.length - 1);
                for (int i = 1; i < p.length; i++) {
                    parents.add(p[i]);
                }
                STATIC_HIERARCHY.put(p[0], parents);
            }
            r.close();
        } catch (Exception e) {
        }
    }

       
                                                           
                                                                     
                                                   
                              
       
    private String mapByOwnHierarchy(String owner, String name, String desc, boolean method) {
        if (owner == null || !isOwnClass(owner) || ForgeRemap.isCompat(owner)) {
            return null;
        }
        String cacheKey = (method ? "S:M:" : "S:F:") + owner + "/" + name + " " + desc;
        if (hierarchyCache.containsKey(cacheKey)) {
            return hierarchyCache.get(cacheKey);
        }
        String result = null;
        java.util.ArrayDeque<String> queue = new java.util.ArrayDeque<String>();
        java.util.HashSet<String> seen = new java.util.HashSet<String>();
        queue.add(owner);
        while (!queue.isEmpty() && result == null) {
            String cur = queue.poll();
            if (!seen.add(cur)) {
                continue;
            }
            for (String parent : parentsOf(cur)) {
                if (parent == null) {
                    continue;
                }
                String hit = method
                        ? md.get(parent + "/" + name + " " + desc)
                        : fd.get(parent + "/" + name);
                if (hit != null) {
                    result = hit;
                    break;
                }
                queue.add(parent);
            }
        }
        hierarchyCache.put(cacheKey, result);
        return result;
    }

                                                              
    private java.util.List<String> parentsOf(String internal) {
        if (isOwnClass(internal)) {
            return parentsOfClass(internal);
        }
        ensureStaticHierarchy();
        java.util.List<String> parents = STATIC_HIERARCHY.get(internal);
        return parents != null ? parents : parentsOfClass(internal);
    }

                                                
    private java.util.List<String> parentsOfClass(String internal) {
        java.util.List<String> cached = classParents.get(internal);
        if (cached != null) {
            return cached;
        }
        java.util.List<String> list = new java.util.ArrayList<String>();
        try {
            java.io.InputStream in = openResource(internal + ".class");
            if (in != null) {
                ClassReader cr = new ClassReader(in);
                if (cr.getSuperName() != null) {
                    list.add(cr.getSuperName());
                }
                for (String itf : cr.getInterfaces()) {
                    list.add(itf);
                }
                in.close();
            }
        } catch (Throwable ignored) {
        }
        classParents.put(internal, list);
        return list;
    }

    private static java.io.InputStream openResource(String resource) {
        ClassLoader[] loaders = new ClassLoader[] {
                Thread.currentThread().getContextClassLoader(),
                NamespaceRegistry.class.getClassLoader(),
                ClassLoader.getSystemClassLoader(),
        };
        for (ClassLoader loader : loaders) {
            if (loader == null) {
                continue;
            }
            java.io.InputStream in = loader.getResourceAsStream(resource);
            if (in != null) {
                return in;
            }
        }
        return null;
    }

    private String walkHierarchy(String start, String name, String desc, boolean method) {
        try {
                                                        
            String runtime = forward && cl.containsKey(start) ? cl.get(start) : start;
            Class<?> c = loadClass(runtime);
            if (c == null) {
                return null;
            }
            java.util.ArrayDeque<Class<?>> queue = new java.util.ArrayDeque<Class<?>>();
            java.util.HashSet<Class<?>> seen = new java.util.HashSet<Class<?>>();
            queue.add(c);
            while (!queue.isEmpty()) {
                Class<?> k = queue.poll();
                if (!seen.add(k)) {
                    continue;
                }
                String rt = k.getName().replace('.', '/');
                String keyOwner = forward ? clInv.get(rt) : rt;
                if (keyOwner != null) {
                    String hit = method
                            ? md.get(keyOwner + "/" + name + " " + desc)
                            : fd.get(keyOwner + "/" + name);
                    if (hit != null) {
                        return hit;
                    }
                }
                Class<?> sup = k.getSuperclass();
                if (sup != null) {
                    queue.add(sup);
                }
                Class<?>[] itfs = k.getInterfaces();
                if (itfs != null) {
                    for (Class<?> itf : itfs) {
                        queue.add(itf);
                    }
                }
            }
        } catch (Throwable ignore) {
                                                 
        }
        return null;
    }


    private static Class<?> loadClass(String internalName) {
        if (internalName == null) {
            return null;
        }
        String dotted = internalName.replace('/', '.');
        ClassLoader[] loaders = new ClassLoader[] {
                Thread.currentThread().getContextClassLoader(),
                NamespaceRegistry.class.getClassLoader(),
                ClassLoader.getSystemClassLoader(),
        };
        for (ClassLoader loader : loaders) {
                                                            
                                                                            
            Class<?> c = findLoaded(loader, dotted);
            if (c != null) {
                return c;
            }
        }
        return null;
    }

    private static Class<?> findLoaded(ClassLoader loader, String dottedName) {
        if (loader == null) {
            return null;
        }
        try {
            java.lang.reflect.Method m =
                    ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);
            m.setAccessible(true);
            return (Class<?>) m.invoke(loader, dottedName);
        } catch (Throwable t) {
            return null;
        }
    }

       
                                                             
      
                                             
                                                                    
                                                                          
                                  
       
    public Remapper exactRemapper() {
        return new Remapper() {
            @Override
            public String map(String type) {
                return mapClassName(type);
            }

            @Override
            public String mapFieldName(String owner, String name, String desc) {
                String r = fd.get(owner + "/" + name);
                return r != null ? r : name;
            }

            @Override
            public String mapMethodName(String owner, String name, String desc) {
                String r = md.get(owner + "/" + name + " " + desc);
                return r != null ? r : name;
            }
        };
    }

                             
    public byte[] apply(byte[] classBytes) {
        return apply(classBytes, false);
    }

                                             
    public byte[] applyOwn(byte[] classBytes) {
        ClassReader cr = new ClassReader(classBytes);
        java.util.List<String> parents = new java.util.ArrayList<String>();
        boolean mcParent = isMinecraftType(cr.getSuperName());
        if (cr.getSuperName() != null) {
            parents.add(cr.getSuperName());
        }
        for (String itf : cr.getInterfaces()) {
            parents.add(itf);
            if (isMinecraftType(itf)) {
                mcParent = true;
            }
        }
        ClassWriter cw = new ClassWriter(0);
                                                             
                                                                
                                                      
        final Set<String> methods = new HashSet<String>();
        final Set<String> fields = new HashSet<String>();
        final boolean[] srgCompiled = {false};
        cr.accept(new ClassVisitor(org.objectweb.asm.Opcodes.ASM5) {
            @Override
            public org.objectweb.asm.FieldVisitor visitField(int access, String name, String desc,
                                                             String signature, Object value) {
                fields.add(name + ":" + desc);
                if (name.startsWith("field_")) {
                    srgCompiled[0] = true;
                }
                return null;
            }

            @Override
            public org.objectweb.asm.MethodVisitor visitMethod(int access, String name, String desc,
                                                               String signature, String[] exceptions) {
                methods.add(name + desc);
                if (name.startsWith("func_")) {
                    srgCompiled[0] = true;
                }
                return null;
            }
        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                                                  
                                                                             
        cr.accept(new RemapAdapter(cw, ownRemapper(cr.getClassName(), parents, mcParent,
                srgCompiled[0], methods, fields)), ClassReader.EXPAND_FRAMES);
        return cw.toByteArray();
    }

    private static boolean isMinecraftType(String internal) {
        return internal != null && internal.startsWith("net/minecraft/");
    }

                           
    public byte[] applyExact(byte[] classBytes) {
        return apply(classBytes, true);
    }

    private byte[] apply(byte[] classBytes, boolean exact) {
        ClassReader cr = new ClassReader(classBytes);
        ClassWriter cw = new ClassWriter(0);
                                                                 
                                                                                                 
        cr.accept(new RemapAdapter(cw, exact ? exactRemapper() : remapper()), ClassReader.EXPAND_FRAMES);
        return cw.toByteArray();
    }

                                           
                                                                      
    private boolean mapped(String owner) {
        return owner != null && cl.containsKey(owner);
    }

    private void load(String resource, boolean forward) {
        if (resource == null) {
            return;
        }
        InputStream in = NamespaceRegistry.class.getResourceAsStream(resource);
        if (in == null) {
            return;
        }
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                int c = line.indexOf(':');
                if (c < 0) {
                    continue;
                }
                String kind = line.substring(0, c);
                String[] p = line.substring(c + 1).trim().split("\\s+");
                if ("CL".equals(kind) && p.length == 2) {
                    put(cl, p[0], p[1], forward);
                } else if ("FD".equals(kind) && p.length == 2) {
                    String a = p[0];
                    String b = p[1];
                    String from = forward ? a : b;
                    String to = forward ? b : a;
                    fd.put(from, simple(to));
                    String fk = simple(from);
                    String prev = fd.get(fk);
                    if (prev != null && !prev.equals(simple(to))) {
                        ambig.add("F:" + fk);
                    }
                    fd.put(fk, simple(to));
                } else if ("MD".equals(kind) && p.length == 4) {
                    String a = p[0];
                    String ad = p[1];
                    String b = p[2];
                    String bd = p[3];
                    String from = forward ? a : b;
                    String fdesc = forward ? ad : bd;
                    String to = forward ? b : a;
                    md.put(from + " " + fdesc, simple(to));
                    String mk = simple(from) + " " + fdesc;
                    String prev = md.get(mk);
                    if (prev != null && !prev.equals(simple(to))) {
                        ambig.add("M:" + mk);
                    }
                    md.put(mk, simple(to));
                    String nk = simple(from);
                    String prevN = md.get(nk);
                    if (prevN != null && !prevN.equals(simple(to))) {
                        ambig.add("M:" + nk);
                    }
                    md.put(nk, simple(to));
                }
            }
            r.close();
        } catch (Exception e) {
        }
    }

    private static void put(Map<String, String> m, String a, String b, boolean forward) {
        if (forward) {
            m.put(a, b);
        } else {
            m.put(b, a);
        }
    }

    private static boolean isSrgName(String name) {
        return name != null && (name.startsWith("func_") || name.startsWith("field_"));
    }

    private static String simple(String path) {
        int i = path.lastIndexOf('/');
        return i < 0 ? path : path.substring(i + 1);
    }

                                                     
    private static final class RemapAdapter extends RemappingClassAdapter {
        RemapAdapter(ClassVisitor cv, Remapper remapper) {
            super(cv, remapper);
        }
    }
}
