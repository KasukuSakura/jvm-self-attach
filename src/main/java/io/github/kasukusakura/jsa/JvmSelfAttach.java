/*
 * Copyright 2021 KasukuSakura
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.github.kasukusakura.jsa;

import com.sun.tools.attach.VirtualMachine;
import io.github.karlatemp.unsafeaccessor.Unsafe;
import io.github.karlatemp.unsafeaccessor.UnsafeAccess;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.instrument.Instrumentation;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@SuppressWarnings({"unchecked", "rawtypes"})
public class JvmSelfAttach {
    static Instrumentation instrumentation;
    static final byte[] EXTERNAL_AGENT_BYTECODE = Base64.getDecoder().decode(
            "yv66vgAAADQAGgEAKGlvL2dpdGh1Yi9rYXN1a3VzYWt1cmEvanNhL0V4dGVybmFsQWdlbnQHAAEBABBqYXZhL2xhbmcvT2JqZWN0BwADAQASRXh0ZXJuYWxBZ2VudC5qYXZhAQAPaW5zdHJ1bWVudGF0aW9uAQAmTGphdmEvbGFuZy9pbnN0cnVtZW50L0luc3RydW1lbnRhdGlvbjsBAAY8aW5pdD4BAAMoKVYMAAgACQoABAAKAQAEdGhpcwEAKkxpby9naXRodWIva2FzdWt1c2FrdXJhL2pzYS9FeHRlcm5hbEFnZW50OwEAB3ByZW1haW4BADsoTGphdmEvbGFuZy9TdHJpbmc7TGphdmEvbGFuZy9pbnN0cnVtZW50L0luc3RydW1lbnRhdGlvbjspVgwABgAHCQACABABAANvcHQBABJMamF2YS9sYW5nL1N0cmluZzsBAANpbnMBAAlhZ2VudG1haW4BAARDb2RlAQAPTGluZU51bWJlclRhYmxlAQASTG9jYWxWYXJpYWJsZVRhYmxlAQAKU291cmNlRmlsZQAhAAIABAAAAAEACQAGAAcAAAADAAEACAAJAAEAFgAAAC8AAQABAAAABSq3AAuxAAAAAgAXAAAABgABAAAABQAYAAAADAABAAAABQAMAA0AAAAJAA4ADwABABYAAAA9AAEAAgAAAAUrswARsQAAAAIAFwAAAAoAAgAAAAkABAAKABgAAAAWAAIAAAAFABIAEwAAAAAABQAUAAcAAQAJABUADwABABYAAAA9AAEAAgAAAAUrswARsQAAAAIAFwAAAAoAAgAAAA0ABAAOABgAAAAWAAIAAAAFABIAEwAAAAAABQAUAAcAAQABABkAAAACAAU="
    );
    static final UnsafeAccess UA = UnsafeAccess.getInstance();

    public static synchronized void init(File tmp) {
        if (instrumentation != null) {
            return;
        }
        try {
            init0(tmp);
        } catch (Throwable throwable) {
            throw new ExceptionInInitializerError(throwable);
        }
    }

    public static Instrumentation getInstrumentation() {
        Instrumentation i = instrumentation;
        if (i == null) throw new NullPointerException(
                "Instrumentation not initialized, call `JvmSelfAttach.init` first"
        );
        return i;
    }

    private static File genEAFile(File dir, File rdfile, String cnname, String EAN, boolean boot) throws Throwable {
        if (rdfile == null) {
            do {
                rdfile = new File(dir, "external-agent-" + UUID.randomUUID() + ".jar");
            } while (rdfile.exists());
        }

        try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(
                new FileOutputStream(rdfile)
        ))) {
            zos.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
            Manifest mf = new Manifest();
            mf.getMainAttributes().putValue("Manifest-Version", "1");
            mf.getMainAttributes().putValue("Premain-Class", cnname);
            mf.getMainAttributes().putValue("Agent-Class", cnname);
            mf.getMainAttributes().putValue("Launcher-Agent-Class", cnname);
            if (boot) {
                mf.getMainAttributes().putValue("Boot-Class-Path", rdfile.getAbsolutePath().replace('\\', '/'));
            }
            mf.getMainAttributes().putValue("Can-Redefine-Classes", "true");
            mf.getMainAttributes().putValue("Can-Retransform-Classes", "true");
            mf.getMainAttributes().putValue("Can-Set-Native-Method-Prefix", "true");
            mf.write(zos);

            zos.putNextEntry(new ZipEntry(cnname.replace('.', '/') + ".class"));
            byte[] code = EXTERNAL_AGENT_BYTECODE;
            code = BytecodeUtil.replace(
                    code,
                    EAN,
                    cnname
            );
            code = BytecodeUtil.replace(
                    code,
                    EAN.replace('.', '/'),
                    cnname.replace('.', '/')
            );
            code = BytecodeUtil.replace(
                    code,
                    "L" + EAN.replace('.', '/') + ";",
                    "L" + cnname.replace('.', '/') + ";"
            );
            zos.write(code);
        }
        return rdfile;
    }

    private static void init0(File tmp) throws Throwable {
        String cnname = "io.github.kasukusakura.jsa.p" + UUID.randomUUID() + ".EA";
        cnname = cnname.replace('-', '_');

        // Anti relocate
        //noinspection StringBufferReplaceableByString
        String EAN = new StringBuilder()
                .append("io").append(".git").append("hub.kasuku").append("sakura.jsa.")
                .append("ExternalAgent")
                .toString();
        tmp.mkdirs();
        {
            File[] listFiles = tmp.listFiles();
            long sysNow = System.currentTimeMillis();
            if (listFiles != null) for (File subFile : listFiles) {
                if (subFile.isFile() && subFile.getName().startsWith("external-agent-")) {
                    try {
                        if (sysNow - subFile.lastModified() > 1000L * 60) {
                            subFile.delete();
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }
        }
        File rdfile = genEAFile(tmp, null, cnname, EAN, false);

        loadAgent(cnname, EAN, rdfile);
        fetchInst(cnname);
    }

    private static void loadAgent(String cnname, String EAN, File rdfile) throws Throwable {
        UA.getUnsafe();
        String absp = rdfile.getAbsolutePath();
        List<Throwable> allFails = new ArrayList<>();

        long pid;
        String pid_str;
        try {
            Class<?> PH = Class.forName("java.lang.ProcessHandle");
            MethodHandles.Lookup lk = MethodHandles.lookup();
            Object currentProcess = lk.findStatic(PH, "current", MethodType.methodType(PH)).invoke();
            pid = (long) lk.findVirtual(PH, "pid", MethodType.methodType(long.class)).invoke(currentProcess);
            pid_str = Long.toString(pid);
        } catch (Throwable ignored) {
            RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
            String name = runtimeMXBean.getName();
            pid_str = name.substring(0, name.indexOf('@'));
            pid = Long.parseLong(pid_str);
        }
        try { // jdk.attach
            System.setProperty("jdk.attach.allowAttachSelf", "true");
            try {
                Class<?> VM = Class.forName("jdk.internal.misc.VM");
                Field f = VM.getDeclaredField("savedProps");
                UA.setAccessible(f, true);
                ((Map) f.get(null)).put("jdk.attach.allowAttachSelf", "true");
            } catch (Throwable a) {
                allFails.add(a);
            }
            try {
                Class<?> HotSpotVirtualMachine = Class.forName("sun.tools.attach.HotSpotVirtualMachine");
                UA.getUnsafe().ensureClassInitialized(HotSpotVirtualMachine);
                Field attach_self = HotSpotVirtualMachine.getDeclaredField("ALLOW_ATTACH_SELF");
                UA.setAccessible(attach_self, true);
                try {
                    attach_self.setBoolean(null, true);
                } catch (Throwable w) {
                    allFails.add(w);
                    Unsafe unsafe = UA.getUnsafe();
                    unsafe.putBoolean(
                            unsafe.staticFieldBase(attach_self),
                            unsafe.staticFieldOffset(attach_self),
                            true
                    );
                }
            } catch (Throwable a) {
                allFails.add(a);
            }
            VirtualMachine attach = VirtualMachine.attach(pid_str);
            attach.loadAgent(absp);
            attach.detach();
            return;
        } catch (Throwable e) {
            allFails.add(e);
        }

        try {
            File javaHome = new File(System.getProperty("java.home"));
            if (javaHome.getName().equals("jre")) {
                javaHome = javaHome.getParentFile();
            }
            File tools = new File(javaHome, "lib/tools.jar");
            if (tools.exists()) {
                URLClassLoader urlClassLoader = new URLClassLoader(new URL[]{
                        tools.toURI().toURL()
                });
                Class<?> vm = urlClassLoader.loadClass("com.sun.tools.attach.VirtualMachine");
                Object attach = vm.getMethod("attach", String.class).invoke(null, pid_str);
                vm.getMethod("loadAgent", String.class).invoke(attach, absp);
                vm.getMethod("detach").invoke(attach);
                return;
            }
        } catch (Throwable e) {
            allFails.add(e);
        }
        try {
            genEAFile(null, rdfile, cnname, EAN, true);
        } catch (Throwable ignored) {
        }
        try { //java.instrument
            Method met = Class.forName("sun.instrument.InstrumentationImpl")
                    .getDeclaredMethod("loadAgent", String.class);
            UA.setAccessible(met, true);
            met.invoke(null, absp);
            return;
        } catch (Throwable e) {
            allFails.add(e);
        }

        LinkageError error = new LinkageError("Failed to attach self, try upgrade your java version or use JDK");
        for (Throwable t : allFails) {
            error.addSuppressed(t);
        }
        throw error;
    }

    private static void fetchInst(String cname) throws Throwable {
        Class<?> c = Class.forName(cname, false, ClassLoader.getSystemClassLoader());
        Field f = c.getField("instrumentation");
        UA.setAccessible(f, true);
        Instrumentation instrumentation = (Instrumentation) f.get(null);
        f.set(null, null);
        JvmSelfAttach.instrumentation = instrumentation;
        // System.out.println(instrumentation);
    }

    public static void main(String[] args) {
        JvmSelfAttach.init(new File("build/jsa"));
        System.out.println(instrumentation);
    }
}
