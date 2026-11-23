package cn.pig.asm.ns;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

   
                                    
                                                                             
  
                                                            
                                    
       
                                       
                                                        
                                                             
                                
        
                                
   
public final class SchemaGuard {

    private SchemaGuard() {
    }

    public static byte[] apply(String name, byte[] input, byte[] output) {
        if (input == null || output == null) {
            return output;
        }
        try {
            ClassNode before = read(input);
            ClassNode after = read(output);
                                                                         
                                                
                                                
            trimToInput(name, before, after);
            ClassWriter writer = new ClassWriter(0);
            after.accept(writer);
            return writer.toByteArray();
        } catch (Throwable t) {
            log("schema " + name + " guard failed: " + t);
            return output;
        }
    }

                                                   

    private static void trimToInput(String name, ClassNode before, ClassNode after) {
        Set<String> methods = new HashSet<String>();
        for (Object o : before.methods) {
            MethodNode m = (MethodNode) o;
            methods.add(m.name + m.desc);
        }
        Set<String> fields = new HashSet<String>();
        for (Object o : before.fields) {
            FieldNode f = (FieldNode) o;
            fields.add(f.name + ":" + f.desc);
        }
        TreeSet<String> addedMethods = new TreeSet<String>();
        for (int i = after.methods.size() - 1; i >= 0; i--) {
            MethodNode m = (MethodNode) after.methods.get(i);
            if (!methods.contains(m.name + m.desc)) {
                addedMethods.add(m.name + m.desc);
                after.methods.remove(i);
            }
        }
        TreeSet<String> addedFields = new TreeSet<String>();
        for (int i = after.fields.size() - 1; i >= 0; i--) {
            FieldNode f = (FieldNode) after.fields.get(i);
            if (!fields.contains(f.name + ":" + f.desc)) {
                addedFields.add(f.name + ":" + f.desc);
                after.fields.remove(i);
            }
        }
        boolean ifaceChanged = !before.interfaces.equals(after.interfaces);
        if (!addedMethods.isEmpty() || !addedFields.isEmpty() || ifaceChanged) {
            log("schema " + name + " addedM=" + addedMethods + " addedF=" + addedFields
                    + " iface " + before.interfaces + "->" + after.interfaces);
            after.interfaces.clear();
            after.interfaces.addAll(before.interfaces);
        }
    }

                                                      

    private static byte[] alignToLive(String name, ClassNode after, Class<?> live) {
                           
        Class<?>[] interfaces = live.getInterfaces();
        Set<String> liveIfaces = new HashSet<String>();
        for (Class<?> i : interfaces) {
            liveIfaces.add(i.getName().replace('.', '/'));
        }
        if (!new HashSet<String>(after.interfaces).equals(liveIfaces)) {
            log("live " + name + " iface " + after.interfaces + "->" + liveIfaces);
            after.interfaces.clear();
            after.interfaces.addAll(liveIfaces);
        }
        Class<?> superClass = live.getSuperclass();
        if (superClass != null) {
            String liveSuper = superClass.getName().replace('.', '/');
            if (!liveSuper.equals(after.superName)) {
                log("live " + name + " super " + after.superName + "->" + liveSuper);
                after.superName = liveSuper;
            }
        }

                                                    
        Set<String> liveMethods = new HashSet<String>();
        Map<String, String> liveMethodByDesc = new HashMap<String, String>();
        for (java.lang.reflect.Method m : live.getDeclaredMethods()) {
            String desc = methodDesc(m);
            liveMethods.add(m.getName() + desc);
            liveMethodByDesc.put(desc, m.getName());
        }
        for (Constructor<?> c : live.getDeclaredConstructors()) {
            liveMethods.add("<init>" + ctorDesc(c));
        }
        Set<String> liveFields = new HashSet<String>();
        Map<String, String> liveFieldByDesc = new HashMap<String, String>();
        for (java.lang.reflect.Field f : live.getDeclaredFields()) {
            String desc = descriptorOf(f.getType());
            liveFields.add(f.getName() + ":" + desc);
            liveFieldByDesc.put(desc, f.getName());
        }

        Set<String> outMethods = new HashSet<String>();
        for (Object o : after.methods) {
            MethodNode m = (MethodNode) o;
            outMethods.add(m.name + m.desc);
        }
        Set<String> outFields = new HashSet<String>();
        for (Object o : after.fields) {
            FieldNode f = (FieldNode) o;
            outFields.add(f.name + ":" + f.desc);
        }

        TreeSet<String> liveOnlyM = new TreeSet<String>(liveMethods);
        liveOnlyM.removeAll(outMethods);
        TreeSet<String> outOnlyM = new TreeSet<String>(outMethods);
        outOnlyM.removeAll(liveMethods);
        TreeSet<String> liveOnlyF = new TreeSet<String>(liveFields);
        liveOnlyF.removeAll(outFields);
        TreeSet<String> outOnlyF = new TreeSet<String>(outFields);
        outOnlyF.removeAll(liveFields);

                                
        Map<String, String> fieldRenames = pairByDesc(outOnlyF, liveOnlyF, true);
        for (String key : fieldRenames.keySet()) {
            String oldName = key.substring(0, key.indexOf(':'));
            String desc = key.substring(key.indexOf(':') + 1);
            String newName = fieldRenames.get(key);
            log("live " + name + " rename field " + oldName + " -> " + newName);
            for (Object o : after.fields) {
                FieldNode f = (FieldNode) o;
                if (f.name.equals(oldName) && f.desc.equals(desc)) {
                    f.name = newName;
                }
            }
            for (Object o : after.methods) {
                MethodNode m = (MethodNode) o;
                for (Object insnObj : m.instructions.toArray()) {
                    if (insnObj instanceof FieldInsnNode) {
                        FieldInsnNode fi = (FieldInsnNode) insnObj;
                        if (fi.owner.equals(after.name) && fi.name.equals(oldName) && fi.desc.equals(desc)) {
                            fi.name = newName;
                        }
                    }
                }
            }
        }
        for (String key : fieldRenames.keySet()) {
            outOnlyF.remove(key);
            liveOnlyF.remove(fieldRenames.get(key) + key.substring(key.indexOf(':')));
        }

        Map<String, String> methodRenames = pairByDesc(outOnlyM, liveOnlyM, false);
        for (String key : methodRenames.keySet()) {
            int paren = key.indexOf('(');
            String oldName = key.substring(0, paren);
            String desc = key.substring(paren);
            String newName = methodRenames.get(key);
            log("live " + name + " rename method " + oldName + desc + " -> " + newName);
            for (Object o : after.methods) {
                MethodNode m = (MethodNode) o;
                if (m.name.equals(oldName) && m.desc.equals(desc)) {
                    m.name = newName;
                }
                for (Object insnObj : m.instructions.toArray()) {
                    if (insnObj instanceof MethodInsnNode) {
                        MethodInsnNode mi = (MethodInsnNode) insnObj;
                        if (mi.owner.equals(after.name) && mi.name.equals(oldName) && mi.desc.equals(desc)) {
                            mi.name = newName;
                        }
                    }
                }
            }
        }
        for (String key : methodRenames.keySet()) {
            int paren = key.indexOf('(');
            outOnlyM.remove(key);
            liveOnlyM.remove(methodRenames.get(key) + key.substring(paren));
        }

        if (!liveOnlyF.isEmpty() || !liveOnlyM.isEmpty()) {
            log("live " + name + " liveOnlyM=" + liveOnlyM + " liveOnlyF=" + liveOnlyF);
            return null;
        }

                                                               
        for (int i = after.methods.size() - 1; i >= 0; i--) {
            MethodNode m = (MethodNode) after.methods.get(i);
            String key = m.name + m.desc;
            if (m.name.equals("<clinit>")) {
                continue;
            }
            if (!liveMethods.contains(key)) {
                after.methods.remove(i);
            }
        }
        for (int i = after.fields.size() - 1; i >= 0; i--) {
            FieldNode f = (FieldNode) after.fields.get(i);
            if (!liveFields.contains(f.name + ":" + f.desc)) {
                after.fields.remove(i);
            }
        }

        ClassWriter writer = new ClassWriter(0);
        after.accept(writer);
        return writer.toByteArray();
    }

                                                           
    private static Map<String, String> pairByDesc(Set<String> outOnly, Set<String> liveOnly, boolean field) {
        Map<String, String> renames = new HashMap<String, String>();
        Iterator<String> it = outOnly.iterator();
        while (it.hasNext()) {
            String key = it.next();
            if (!field && key.startsWith("<")) {
                continue;                   
            }
            int split = field ? key.indexOf(':') : key.indexOf('(');
            String desc = key.substring(split);
            Iterator<String> jt = liveOnly.iterator();
            while (jt.hasNext()) {
                String other = jt.next();
                int split2 = field ? other.indexOf(':') : other.indexOf('(');
                if (!other.substring(split2).equals(desc)) {
                    continue;
                }
                renames.put(key, other.substring(0, split2));
                jt.remove();
                break;
            }
        }
        return renames;
    }

    private static Class<?> findLiveClass(byte[] input) {
        try {
            String internal = new ClassReader(input).getClassName();
            java.lang.reflect.Method find = ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);
            find.setAccessible(true);
            Object loaded = find.invoke(SchemaGuard.class.getClassLoader(), internal.replace('/', '.'));
            return loaded == null ? null : (Class<?>) loaded;
        } catch (Throwable t) {
            return null;
        }
    }

    private static String methodDesc(java.lang.reflect.Method m) {
        StringBuilder sb = new StringBuilder("(");
        for (Class<?> p : m.getParameterTypes()) {
            sb.append(descriptorOf(p));
        }
        sb.append(')').append(descriptorOf(m.getReturnType()));
        return sb.toString();
    }

    private static String ctorDesc(Constructor<?> c) {
        StringBuilder sb = new StringBuilder("(");
        for (Class<?> p : c.getParameterTypes()) {
            sb.append(descriptorOf(p));
        }
        return sb.append(")V").toString();
    }

    private static String descriptorOf(Class<?> c) {
        if (c.isArray()) {
            return c.getName().replace('.', '/');
        }
        if (c == void.class) return "V";
        if (c == boolean.class) return "Z";
        if (c == byte.class) return "B";
        if (c == char.class) return "C";
        if (c == short.class) return "S";
        if (c == int.class) return "I";
        if (c == long.class) return "J";
        if (c == float.class) return "F";
        if (c == double.class) return "D";
        return "L" + c.getName().replace('.', '/') + ";";
    }

    private static ClassNode read(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        return node;
    }

    private static synchronized void log(String line) {
                    
    }
}
