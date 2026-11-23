package cn.pig.agent;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public final class EntryLocator {

    private static final String MANIFEST_KEY = "Pig-Entry";
    private static final String ANNOTATION_TOKEN = "cn/pig/agent/PigEntry";
    private static final String[] SCAN_PREFIXES = {
            "cn/pig/", "kqa/", "zmv/", "xtr/", "qjf/", "wbn/", "vhx/", "lzp/", "rkd/", "cmp/",
    };
    private static final String AGENT_PREFIX = "cn/pig/agent/";
    private static final String CLASS_SUFFIX = ".class";

    private EntryLocator() {
    }

    public static String locate() {
        File self = selfJar();
        if (self != null) {
            String found = locateIn(self);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    public static String locateIn(File jarFile) {
        if (jarFile == null || !jarFile.isFile()) {
            return null;
        }
        try (JarFile jar = new JarFile(jarFile)) {
            String declared = manifestEntry(jar);
            if (declared != null && !declared.isEmpty()) {
                return declared;
            }
            return scanAnnotated(jar);
        } catch (Throwable t) {
            return null;
        }
    }

    private static String manifestEntry(JarFile jar) throws Exception {
        Manifest mf = jar.getManifest();
        if (mf == null) {
            return null;
        }
        return mf.getMainAttributes().getValue(MANIFEST_KEY);
    }

    private static String scanAnnotated(JarFile jar) throws Exception {
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();
            if (!name.endsWith(CLASS_SUFFIX) || !hasScanPrefix(name)
                    || name.startsWith(AGENT_PREFIX)) {
                continue;
            }
            byte[] bytes = readAll(jar.getInputStream(entry));
            if (indexOf(bytes, ANNOTATION_TOKEN) >= 0) {
                return name.substring(0, name.length() - CLASS_SUFFIX.length()).replace('/', '.');
            }
        }
        return null;
    }

    private static boolean hasScanPrefix(String name) {
        for (String prefix : SCAN_PREFIXES) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static byte[] readAll(InputStream in) throws Exception {
        try (InputStream i = in) {
            ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
            byte[] buf = new byte[8192];
            int n;
            while ((n = i.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        }
    }

    private static int indexOf(byte[] data, String token) {
        byte[] t = token.getBytes(StandardCharsets.UTF_8);
        outer:
        for (int i = 0; i + t.length <= data.length; i++) {
            for (int j = 0; j < t.length; j++) {
                if (data[i + j] != t[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    private static File selfJar() {
        File f = codeSourceJar();
        return f != null ? f : resourceJar();
    }

    private static File codeSourceJar() {
        try {
            java.security.CodeSource cs = EntryLocator.class.getProtectionDomain().getCodeSource();
            if (cs == null || cs.getLocation() == null) {
                return null;
            }
            File f = new File(cs.getLocation().toURI());
            return f.isFile() && f.getName().endsWith(".jar") ? f : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static File resourceJar() {
        try {
            java.net.URL url = EntryLocator.class.getResource("EntryLocator.class");
            if (url == null) {
                return null;
            }
            java.net.URLConnection c = url.openConnection();
            if (c instanceof java.net.JarURLConnection) {
                File f = new File(((java.net.JarURLConnection) c).getJarFileURL().toURI());
                return f.isFile() && f.getName().endsWith(".jar") ? f : null;
            }
        } catch (Throwable t) {
        }
        return null;
    }
}
