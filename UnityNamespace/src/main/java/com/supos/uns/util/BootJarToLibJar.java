package com.supos.uns.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StreamUtils;

import java.io.*;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.jar.*;

/**
 * spring boot jar 包转为 libs 依赖在外面的普通jar
 */
@Slf4j
public class BootJarToLibJar {

    public static void main(String[] args) throws IOException {
        String path = args.length > 0 ? args[0] : ".";
        String libs = args.length > 1 ? args[1] : "libs";
        if (path == null) {
            System.err.println("文件路径不能为空!");
            return;
        }
        File srcJar = new File(path);
        if (!srcJar.exists()) {
            System.err.println("文件不存在: " + srcJar);
            System.exit(1);
        }
        File libDir = new File(libs);
        searchAndExec(srcJar, libDir, null);
    }

    public static void searchAndExec(File target, File libDir, BiFunction<String, InputStream, Boolean> acceptJar) throws IOException {
        String name = target.getName();
        if (target.isDirectory() && !name.equals("src") && !name.equals(libDir.getName())) {
            if (name.equals("target")) {
                for (File f : target.listFiles()) {
                    if (f.isFile() && f.getName().endsWith(".jar")) {
                        searchAndExec(f, libDir, acceptJar);
                    }
                }
            } else {
                for (File f : target.listFiles()) {
                    if (!f.getName().startsWith(".")) {
                        searchAndExec(f, libDir, acceptJar);
                    }
                }
            }
        } else if (target.isFile() && name.endsWith(".jar") && !name.endsWith("-sources.jar")) {
            procJar(target, libDir, acceptJar);
        }
    }

    private static void procJar(File srcJar, File libDir, BiFunction<String, InputStream, Boolean> acceptJar) throws IOException {
        String[] errorHolder = new String[1];
        File newJar = replaceMavenSpringBoot(srcJar, libDir, errorHolder, acceptJar);
        if (newJar != null) {
            log.debug("处理: {}", srcJar);
            mv(newJar, srcJar);
            newJar.delete();
        } else if (errorHolder[0] != null) {
            log.error(errorHolder[0]);
        }
    }

    static File replaceMavenSpringBoot(File file, File libsDir, String[] errorHolder, BiFunction<String, InputStream, Boolean> acceptJar) throws IOException {
        if (file.isDirectory() || !file.getName().endsWith(".jar")) {
            if (errorHolder != null) {
                errorHolder[0] = "不是jar包: " + file;
            }
            return null;
        }
        JarFile jarFile = new JarFile(file);
        Manifest manifest = jarFile.getManifest();
        Attributes mainAttributes = manifest.getMainAttributes();
        String libEntryPath = mainAttributes.getValue("Spring-Boot-Lib");
        String mainClass = mainAttributes.getValue("Start-Class");
        String classDir = mainAttributes.getValue("Spring-Boot-Classes");
        if (libEntryPath == null || mainClass == null || classDir == null) {
            jarFile.close();
            if (errorHolder != null) {
                errorHolder[0] = "不是SpringBoot打出的包: " + file;
            }
            return null;
        }
        if (libsDir == null) {
            libsDir = new File(file.getParentFile().getParentFile(), "libs");
        }
        if (!libsDir.exists()) {
            libsDir.mkdirs();
        }
        final String LIB_NAME = libsDir.getName();
        File targetFile = new File(jarFile.getName() + ".lite.jar");
        try (JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(targetFile))) {
            StringBuilder classPathLibs = new StringBuilder(4096);
            Enumeration<JarEntry> enumeration = jarFile.entries();
            HashSet<String> dirs = new HashSet<>();
            while (enumeration.hasMoreElements()) {
                JarEntry jarEntry = enumeration.nextElement();
                if (jarEntry.isDirectory()) {
                    continue;
                }
                String name = jarEntry.getName();
                log.trace("** jarEntry: {}", name);
                if (name.startsWith(libEntryPath)) {
                    InputStream stream = null;
                    if (acceptJar != null) {
                        byte[] data = StreamUtils.copyToByteArray(jarFile.getInputStream(jarEntry));
                        stream = new ByteArrayInputStream(data);
                        if (!Boolean.TRUE.equals(acceptJar.apply(name, stream))) {
                            continue;
                        }
                        stream = new ByteArrayInputStream(data);
                    }
                    String jar = name.substring(libEntryPath.length());
                    classPathLibs.append(LIB_NAME).append("/").append(jar).append(" ");
                    File libFile = new File(libsDir, jar);
                    copyLib(stream != null ? stream : jarFile.getInputStream(jarEntry), libFile);
                } else if (name.startsWith(classDir)) {
                    String classFile = name.substring(classDir.length());
                    int lastX = classFile.lastIndexOf('/');
                    if (lastX > 0) {
                        final String classFileDir = classFile.substring(0, lastX);
                        int sp = classFileDir.indexOf('/');
                        while (sp > 0) {
                            String pDir = classFileDir.substring(0, sp + 1);
                            if (dirs.add(pDir)) {
                                jarOutputStream.putNextEntry(new JarEntry(pDir));
                            }
                            sp = classFileDir.indexOf('/', sp + 1);
                        }
                        String dir = classFileDir + "/";
                        if (dirs.add(dir)) {
                            jarOutputStream.putNextEntry(new JarEntry(dir));
                        }
                    }
                    JarEntry classPathEntry = new JarEntry(classFile);
                    jarOutputStream.putNextEntry(classPathEntry);
                    jarOutputStream.flush();
                    copy(jarFile.getInputStream(jarEntry), jarOutputStream);
                } else if (name.contains("META-INF/maven")) {
                    jarOutputStream.putNextEntry(new JarEntry(name));
                    copy(jarFile.getInputStream(jarEntry), jarOutputStream);
                }
            }
            mainAttributes.remove(new Attributes.Name("Start-Class"));
            Iterator<Map.Entry<Object, Object>> iterator = mainAttributes.entrySet().iterator();
            while (iterator.hasNext()) {
                String k = iterator.next().getKey().toString();
                if (k.startsWith("Spring-Boot-")) {
                    iterator.remove();
                }
            }
            mainAttributes.put(new Attributes.Name("Main-Class"), mainClass);
            mainAttributes.put(new Attributes.Name("Class-Path"), classPathLibs.toString());
            jarOutputStream.putNextEntry(new JarEntry("META-INF/MANIFEST.MF"));
            manifest.write(jarOutputStream);
        } finally {
            jarFile.close();
        }
        return targetFile;
    }

    static void copyLib(InputStream inputStream, File libFile) throws IOException {
        if (libFile.exists()) {
            return;
        }
        try (FileOutputStream out = new FileOutputStream(libFile)) {
            copy(inputStream, out);
        } finally {
            inputStream.close();
        }
    }

    static void mv(File src, File to) throws IOException {
        if (!src.renameTo(to)) {
            FileInputStream inputStream = new FileInputStream(src);
            try (FileOutputStream out = new FileOutputStream(to)) {
                copy(inputStream, out);
            } finally {
                inputStream.close();
            }
            src.deleteOnExit();
        }
    }

    static int copy(InputStream in, OutputStream out) throws IOException {
        int byteCount = 0;
        byte[] buffer = new byte[4096];
        int bytesRead = -1;
        while ((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
            byteCount += bytesRead;
        }
        out.flush();
        return byteCount;
    }

}
